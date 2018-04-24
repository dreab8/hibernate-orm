/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.HibernateException;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;

/**
 * A registry of {@link BasicType} instances
 *
 * @author Steve Ebersole
 */
public class BasicTypeRegistry implements Serializable {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( BasicTypeRegistry.class );

	// TODO : analyze these sizing params; unfortunately this seems to be the only way to give a "concurrencyLevel"
	private Map<String, BasicType> registry = new ConcurrentHashMap<>( 100, .75f, 1 );
	private boolean locked;
	private TypeConfiguration typeConfiguration;

	public BasicTypeRegistry(TypeConfiguration typeConfiguration){
		this();
		this.typeConfiguration = typeConfiguration;
	}

	public BasicTypeRegistry() {
		register( BooleanTypeImpl.INSTANCE );
		register( NumericBooleanTypeImpl.INSTANCE );
		register( TrueFalseTypeImpl.INSTANCE );
		register( YesNoTypeImpl.INSTANCE );

		register( ByteTypeImpl.INSTANCE );
		register( CharacterTypeImpl.INSTANCE );
		register( ShortTypeImpl.INSTANCE );
		register( IntegerTypeImpl.INSTANCE );
		register( LongTypeImpl.INSTANCE );
		register( FloatTypeImpl.INSTANCE );
		register( DoubleTypeImpl.INSTANCE );
		register( BigDecimalTypeImpl.INSTANCE );
		register( BigIntegerTypeImpl.INSTANCE );

		register( StringTypeImpl.INSTANCE );
		register( StringNVarcharTypeImpl.INSTANCE );
		register( CharacterNCharTypeImpl.INSTANCE );
		register( UrlTypeImpl.INSTANCE );

		register( DurationTypeImpl.INSTANCE );
		register( InstantTypeImpl.INSTANCE );
		register( LocalDateTimeTypeImpl.INSTANCE );
		register( LocalDateTypeImpl.INSTANCE );
		register( LocalTimeTypeImpl.INSTANCE );
		register( OffsetDateTimeTypeImpl.INSTANCE );
		register( OffsetTimeTypeImpl.INSTANCE );
		register( ZonedDateTimeTypeImpl.INSTANCE );

		register( DateTypeImpl.INSTANCE );
		register( TimeTypeImpl.INSTANCE );
		register( TimestampTypeImpl.INSTANCE );
		register( DbTimestampTypeImpl.INSTANCE );
		register( CalendarTypeImpl.INSTANCE );
		register( CalendarDateTypeImpl.INSTANCE );

		register( LocaleTypeImpl.INSTANCE );
		register( CurrencyTypeImpl.INSTANCE );
		register( TimeZoneTypeImpl.INSTANCE );
		register( ClassTypeImpl.INSTANCE );
		register( UUIDBinaryTypeImpl.INSTANCE );
		register( UUIDCharTypeImpl.INSTANCE );

		register( BinaryTypeImpl.INSTANCE );
		register( WrapperBinaryTypeImpl.INSTANCE );
		register( RowVersionTypeImpl.INSTANCE );
		register( ImageTypeImpl.INSTANCE );
		register( CharArrayTypeImpl.INSTANCE );
		register( CharacterArrayTypeImpl.INSTANCE );
		register( TextTypeImpl.INSTANCE );
		register( NTextTypeImpl.INSTANCE );
		register( BlobTypeImpl.INSTANCE );
		register( MaterializedBlobTypeImpl.INSTANCE );
		register( ClobTypeImpl.INSTANCE );
		register( NClobTypeImpl.INSTANCE );
		register( MaterializedClobTypeImpl.INSTANCE );
		register( MaterializedNClobTypeImpl.INSTANCE );
		register( SerializableTypeImpl.INSTANCE );

		register( ObjectType.INSTANCE );

		//noinspection unchecked
		register( new AdaptedImmutableTypeImpl( DateTypeImpl.INSTANCE ) );
		//noinspection unchecked
		register( new AdaptedImmutableTypeImpl( TimeTypeImpl.INSTANCE ) );
		//noinspection unchecked
		register( new AdaptedImmutableTypeImpl( TimestampTypeImpl.INSTANCE ) );
		//noinspection unchecked
		register( new AdaptedImmutableTypeImpl( DbTimestampTypeImpl.INSTANCE ) );
		//noinspection unchecked
		register( new AdaptedImmutableTypeImpl( CalendarTypeImpl.INSTANCE ) );
		//noinspection unchecked
		register( new AdaptedImmutableTypeImpl( CalendarDateTypeImpl.INSTANCE ) );
		//noinspection unchecked
		register( new AdaptedImmutableTypeImpl( BinaryTypeImpl.INSTANCE ) );
		//noinspection unchecked
		register( new AdaptedImmutableTypeImpl( SerializableTypeImpl.INSTANCE ) );
	}

	/**
	 * Constructor version used during shallow copy
	 *
	 * @param registeredTypes The type map to copy over
	 */
	@SuppressWarnings({"UnusedDeclaration"})
	private BasicTypeRegistry(Map<String, BasicType> registeredTypes) {
		registry.putAll( registeredTypes );
		locked = true;
	}

	public void register(BasicType type) {
		register( type, type.getRegistrationKeys() );
	}

	public void register(BasicType type, String[] keys) {
		if ( locked ) {
			throw new HibernateException( "Can not alter TypeRegistry at this time" );
		}

		if ( type == null ) {
			throw new HibernateException( "Type to register cannot be null" );
		}

		if ( keys == null || keys.length == 0 ) {
			LOG.typeDefinedNoRegistrationKeys( type );
			return;
		}

		for ( String key : keys ) {
			// be safe...
			if ( key == null ) {
				continue;
			}
			//Use String#intern here as there's high chances of duplicates combined with long term usage:
			//just running our testsuite would generate 210,000 instances for the String "java.lang.Class" alone.
			//Incidentally this might help with map lookup efficiency too.
			key = key.intern();
			LOG.debugf( "Adding type registration %s -> %s", key, type );
			final Type old = registry.put( key, type );
			if ( old != null && old != type ) {
				LOG.typeRegistrationOverridesPrevious( key, old );
			}
		}
	}

	public void register(UserType type, String[] keys) {
		register( new CustomType( type, keys ) );
	}

	public void register(CompositeUserType type, String[] keys) {
		register( new CompositeCustomType( type, keys ) );
	}

	public void unregister(String... keys) {
		for ( String key : keys ) {
			registry.remove( key );
		}
	}

	public BasicType getRegisteredType(String key) {
		return registry.get( key );
	}

	public BasicTypeRegistry shallowCopy() {
		return new BasicTypeRegistry( this.registry );
	}
}
