/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities;

import org.hibernate.envers.RevisionType;
import org.hibernate.type.descriptor.java.internal.EnumJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.TinyIntSqlDescriptor;

/**
 * @author Chris Cranford
 */
public class RevisionTypeJavaDescriptor extends EnumJavaDescriptor<RevisionType> {
	public static final RevisionTypeJavaDescriptor INSTANCE = new RevisionTypeJavaDescriptor();

	private RevisionTypeJavaDescriptor() {
		super( RevisionType.class );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return TinyIntSqlDescriptor.INSTANCE;
	}

	@Override
	public <X> X unwrap(RevisionType value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( RevisionType.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( Byte.class.isAssignableFrom( type ) ) {
			return (X) value.getRepresentation();
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> RevisionType wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( RevisionType.class.isInstance( value ) ) {
			return (RevisionType) value;
		}
		if ( Byte.class.isInstance( value ) ) {
			return RevisionType.fromRepresentation( value );
		}
		throw unknownWrap( value.getClass() );
	}
}
