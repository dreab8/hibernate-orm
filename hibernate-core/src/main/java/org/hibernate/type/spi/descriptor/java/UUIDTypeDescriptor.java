/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.java;

import java.util.Comparator;
import java.util.TimeZone;
import java.util.UUID;

import org.hibernate.type.spi.descriptor.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.spi.descriptor.WrapperOptions;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;

/**
 * Descriptor for {@link TimeZone} handling.
 *
 * @author Steve Ebersole
 */
public class UUIDTypeDescriptor extends AbstractTypeDescriptor<UUID> {
	public static final UUIDTypeDescriptor INSTANCE = new UUIDTypeDescriptor();

	public static class UUIDComparator implements Comparator<UUID> {
		public static final UUIDComparator INSTANCE = new UUIDComparator();

		public int compare(UUID o1, UUID o2) {
			return o1.compareTo( o2 );
		}
	}

	public UUIDTypeDescriptor() {
		super( UUID.class );
	}

	public String toString(UUID value) {
		return value.toString();
	}

	public UUID fromString(String string) {
		return UUID.fromString( string );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return StringTypeDescriptor.INSTANCE.getJdbcRecommendedSqlType( context );
	}

	@Override
	public Comparator<UUID> getComparator() {
		return UUIDComparator.INSTANCE;
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(UUID value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) toString( value );
		}
		throw unknownUnwrap( type );
	}

	public <X> UUID wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isInstance( value ) ) {
			return fromString( (String) value );
		}
		throw unknownWrap( value.getClass() );
	}
}
