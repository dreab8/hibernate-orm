/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.io.Reader;
import java.io.Serializable;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Comparator;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.engine.jdbc.NClobImplementer;
import org.hibernate.engine.jdbc.NClobProxy;
import org.hibernate.engine.jdbc.WrappedNClob;
import org.hibernate.engine.jdbc.internal.CharacterStreamImpl;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Descriptor for {@link java.sql.NClob} handling.
 * <p/>
 * Note, {@link java.sql.NClob nclobs} really are mutable (their internal state can in fact be mutated).  We simply
 * treat them as immutable because we cannot properly check them for changes nor deep copy them.
 *
 * @author Steve Ebersole
 */
public class NClobJavaDescriptor extends AbstractBasicJavaDescriptor<NClob> {
	public static final NClobJavaDescriptor INSTANCE = new NClobJavaDescriptor();

	public static class NClobMutabilityPlan implements MutabilityPlan<NClob> {
		public static final NClobMutabilityPlan INSTANCE = new NClobMutabilityPlan();

		public boolean isMutable() {
			return false;
		}

		public NClob deepCopy(NClob value) {
			return value;
		}

		public Serializable disassemble(NClob value) {
			throw new UnsupportedOperationException( "Clobs are not cacheable" );
		}

		public NClob assemble(Serializable cached) {
			throw new UnsupportedOperationException( "Clobs are not cacheable" );
		}
	}

	public NClobJavaDescriptor() {
		super( NClob.class, NClobMutabilityPlan.INSTANCE );
	}

	public String toString(NClob value) {
		return LobStreamDataHelper.extractString( value );
	}

	public NClob fromString(String string) {
		return NClobProxy.generateProxy( string );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		final int jdbcCode;
		if ( context.isNationalized() ) {
			jdbcCode = Types.NCLOB;
		}
		else {
			jdbcCode = Types.CLOB;
		}
		return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( jdbcCode );
	}

	@Override
	public String extractLoggableRepresentation(NClob value) {
		return value == null ? "null" : "{nclob}";
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public Comparator<NClob> getComparator() {
		return IncomparableComparator.INSTANCE;
	}

	@Override
	public int extractHashCode(NClob value) {
		return System.identityHashCode( value );
	}

	@Override
	public boolean areEqual(NClob one, NClob another) {
		return one == another;
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(final NClob value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		try {
			if ( CharacterStream.class.isAssignableFrom( type ) ) {
				if ( NClobImplementer.class.isInstance( value ) ) {
					// if the incoming NClob is a wrapper, just pass along its BinaryStream
					return (X) ( (NClobImplementer) value ).getUnderlyingStream();
				}
				else {
					// otherwise we need to build a CharacterStream...
					return (X) new CharacterStreamImpl( LobStreamDataHelper.extractString( value.getCharacterStream() ) );
				}
			}
			else if (NClob.class.isAssignableFrom( type )) {
				final NClob nclob =  WrappedNClob.class.isInstance( value )
						? ( (WrappedNClob) value ).getWrappedNClob()
						: value;
				return (X) nclob;
			}
		}
		catch ( SQLException e ) {
			throw new HibernateException( "Unable to access nclob stream", e );
		}
		
		throw unknownUnwrap( type );
	}

	public <X> NClob wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		// Support multiple return types from
		// org.hibernate.type.descriptor.sql.ClobJavaDescriptor
		if ( NClob.class.isAssignableFrom( value.getClass() ) ) {
			return options.getLobCreator().wrap( (NClob) value );
		}
		else if ( Reader.class.isAssignableFrom( value.getClass() ) ) {
			Reader reader = (Reader) value;
			return options.getLobCreator().createNClob( LobStreamDataHelper.extractString( reader ) );
		}

		throw unknownWrap( value.getClass() );
	}
}
