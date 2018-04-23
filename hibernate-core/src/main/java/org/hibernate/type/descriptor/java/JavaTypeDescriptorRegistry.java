/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;
import java.sql.Types;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.HibernateException;
import org.hibernate.annotations.Immutable;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.internal.BigDecimalJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.BigIntegerJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.BlobJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.BooleanJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.ByteArrayJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.ByteJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.CalendarJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.CharacterArrayJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.CharacterJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.ClassJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.ClobJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.DoubleJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.FloatJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.IntegerJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.LongJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.ShortJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.RegistryHelper;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Basically a map from {@link Class} -> {@link JavaTypeDescriptor}
 *
 * @author Steve Ebersole
 *
 * @deprecated Use (5.3) Use {@link org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry} instead
 */
@Deprecated
public class JavaTypeDescriptorRegistry implements Serializable {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( JavaTypeDescriptorRegistry.class );

	/**
	 * @deprecated (5.3) Use {@link TypeConfiguration#getJavaTypeDescriptorRegistry()} instead.
	 */
	@Deprecated
	public static final JavaTypeDescriptorRegistry INSTANCE = new JavaTypeDescriptorRegistry();

	private ConcurrentHashMap<Class, JavaTypeDescriptor> descriptorsByClass = new ConcurrentHashMap<>();

	public JavaTypeDescriptorRegistry() {
		addDescriptorInternal( ByteJavaDescriptor.INSTANCE );
		addDescriptorInternal( BooleanJavaDescriptor.INSTANCE );
		addDescriptorInternal( CharacterJavaDescriptor.INSTANCE );
		addDescriptorInternal( ShortJavaDescriptor.INSTANCE );
		addDescriptorInternal( IntegerJavaDescriptor.INSTANCE );
		addDescriptorInternal( LongJavaDescriptor.INSTANCE );
		addDescriptorInternal( FloatJavaDescriptor.INSTANCE );
		addDescriptorInternal( DoubleJavaDescriptor.INSTANCE );
		addDescriptorInternal( BigDecimalJavaDescriptor.INSTANCE );
		addDescriptorInternal( BigIntegerJavaDescriptor.INSTANCE );

		addDescriptorInternal( StringTypeDescriptor.INSTANCE );

		addDescriptorInternal( BlobJavaDescriptor.INSTANCE );
		addDescriptorInternal( ClobJavaDescriptor.INSTANCE );
		addDescriptorInternal( NClobTypeDescriptor.INSTANCE );

		addDescriptorInternal( ByteArrayJavaDescriptor.INSTANCE );
		addDescriptorInternal( CharacterArrayJavaDescriptor.INSTANCE );
		addDescriptorInternal( PrimitiveByteArrayTypeDescriptor.INSTANCE );
		addDescriptorInternal( PrimitiveCharacterArrayTypeDescriptor.INSTANCE );

		addDescriptorInternal( DurationJavaDescriptor.INSTANCE );
		addDescriptorInternal( InstantJavaDescriptor.INSTANCE );
		addDescriptorInternal( LocalDateJavaDescriptor.INSTANCE );
		addDescriptorInternal( LocalDateTimeJavaDescriptor.INSTANCE );
		addDescriptorInternal( OffsetDateTimeJavaDescriptor.INSTANCE );
		addDescriptorInternal( OffsetTimeJavaDescriptor.INSTANCE );
		addDescriptorInternal( ZonedDateTimeJavaDescriptor.INSTANCE );

		addDescriptorInternal( CalendarJavaDescriptor.INSTANCE );
		addDescriptorInternal( DateTypeDescriptor.INSTANCE );
		descriptorsByClass.put( java.sql.Date.class, JdbcDateTypeDescriptor.INSTANCE );
		descriptorsByClass.put( java.sql.Time.class, JdbcTimeTypeDescriptor.INSTANCE );
		descriptorsByClass.put( java.sql.Timestamp.class, JdbcTimestampTypeDescriptor.INSTANCE );
		addDescriptorInternal( TimeZoneTypeDescriptor.INSTANCE );

		addDescriptorInternal( ClassJavaDescriptor.INSTANCE );

		addDescriptorInternal( CurrencyTypeDescriptor.INSTANCE );
		addDescriptorInternal( LocaleTypeDescriptor.INSTANCE );
		addDescriptorInternal( UrlTypeDescriptor.INSTANCE );
		addDescriptorInternal( UUIDTypeDescriptor.INSTANCE );
	}

	private JavaTypeDescriptor addDescriptorInternal(JavaTypeDescriptor descriptor) {
		return descriptorsByClass.put( descriptor.getJavaType(), descriptor );
	}

	/**
	 * Adds the given descriptor to this registry
	 *
	 * @param descriptor The descriptor to add.
	 *
	 * @deprecated (5.3) Use {@link org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry#addDescriptor(JavaTypeDescriptor)} instead.
	 */
	@Deprecated
	public void addDescriptor(JavaTypeDescriptor descriptor) {
		JavaTypeDescriptor old = addDescriptorInternal( descriptor );
		if ( old != null ) {
			log.debugf(
					"JavaTypeDescriptorRegistry entry replaced : %s -> %s (was %s)",
					descriptor.getJavaType(),
					descriptor,
					old
			);
		}
	}

	/**
	 * @deprecated (5.3) Use {@link org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry#getDescriptor(Class)} instead.
	 */
	@Deprecated
	@SuppressWarnings("unchecked")
	public <J> JavaTypeDescriptor<J> getDescriptor(Class<J> cls) {
		return RegistryHelper.INSTANCE.resolveDescriptor(
				descriptorsByClass,
				cls,
				() -> {
					if ( Serializable.class.isAssignableFrom( cls ) ) {
						return new SerializableTypeDescriptor( cls );
					}

					log.debugf(
							"Could not find matching JavaTypeDescriptor for requested Java class [%s]; using fallback.  " +
									"This means Hibernate does not know how to perform certain basic operations in relation to this Java type." +
									"",
							cls.getName()
					);
					checkEqualsAndHashCode( cls );

					return new FallbackJavaTypeDescriptor<>( cls );
				}
		);
	}

	@SuppressWarnings("unchecked")
	private void checkEqualsAndHashCode(Class javaType) {
		if ( !ReflectHelper.overridesEquals( javaType ) || !ReflectHelper.overridesHashCode( javaType ) ) {
			log.unknownJavaTypeNoEqualsHashCode( javaType );
		}
	}


	public static class FallbackJavaTypeDescriptor<T> extends AbstractTypeDescriptor<T> {
		protected FallbackJavaTypeDescriptor(final Class<T> type) {
			super( type, createMutabilityPlan( type ) );
		}

		@SuppressWarnings("unchecked")
		private static <T> MutabilityPlan<T> createMutabilityPlan(final Class<T> type) {
			if ( type.isAnnotationPresent( Immutable.class ) ) {
				return ImmutableMutabilityPlan.INSTANCE;
			}
			// MutableMutabilityPlan is the "safest" option, but we do not necessarily know how to deepCopy etc...
			return new MutableMutabilityPlan<T>() {
				@Override
				protected T deepCopyNotNull(T value) {
					throw new HibernateException(
							"Not known how to deep copy value of type: [" + type
									.getName() + "]"
					);
				}
			};
		}

		@Override
		public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
			return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.JAVA_OBJECT );
		}

		@Override
		public String toString(T value) {
			return value == null ? "<null>" : value.toString();
		}

		@Override
		public T fromString(String string) {
			throw new HibernateException(
					"Not known how to convert String to given type [" + getJavaType().getName() + "]"
			);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
			return (X) value;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <X> T wrap(X value, WrapperOptions options) {
			return (T) value;
		}
	}
}
