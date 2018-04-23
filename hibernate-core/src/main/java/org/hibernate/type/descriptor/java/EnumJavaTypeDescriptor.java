/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.sql.Types;
import javax.persistence.EnumType;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Describes a Java Enum type.
 *
 * @author Steve Ebersole
 */
public class EnumJavaTypeDescriptor<T extends Enum> extends AbstractTypeDescriptor<T> {
	// The recommended Jdbc type code used for EnumType.ORDINAL
	public final static int ORDINAL_JDBC_TYPE_CODE = Types.INTEGER;

	@SuppressWarnings("unchecked")
	public EnumJavaTypeDescriptor(Class<T> type) {
		super( type, ImmutableMutabilityPlan.INSTANCE );
		//JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( this );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		if ( context.getEnumeratedType() != null && context.getEnumeratedType() == EnumType.STRING ) {
			return context.isNationalized()
					? context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.NVARCHAR )
					: context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.VARCHAR );
		}
		else {
			return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( ORDINAL_JDBC_TYPE_CODE );
		}
	}

	@Override
	public String toString(T value) {
		return value == null ? "<null>" : value.name();
	}

	@Override
	@SuppressWarnings("unchecked")
	public T fromString(String string) {
		return string == null ? null : (T) Enum.valueOf( getJavaType(), string );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
		if ( String.class.equals( type ) ) {
			return (X) toName( value );
		}
		else if ( Integer.class.isInstance( type ) ) {
			return (X) toOrdinal( value );
		}

		return (X) value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> T wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		else if ( String.class.isInstance( value ) ) {
			return fromName( (String) value );
		}
		else if ( Integer.class.isInstance( value ) ) {
			return fromOrdinal( (Integer) value );
		}

		return (T) value;
	}


	public <E extends Enum> Integer toOrdinal(E domainForm) {
		if ( domainForm == null ) {
			return null;
		}
		return domainForm.ordinal();
	}

	@SuppressWarnings("unchecked")
	public <E extends Enum> E fromOrdinal(Integer relationalForm) {
		if ( relationalForm == null ) {
			return null;
		}
		return (E) getJavaType().getEnumConstants()[ relationalForm ];
	}

	@SuppressWarnings("unchecked")
	public T fromName(String relationalForm) {
		if ( relationalForm == null ) {
			return null;
		}
		return (T) Enum.valueOf( getJavaType(), relationalForm.trim() );
	}

	public String toName(T domainForm) {
		if ( domainForm == null ) {
			return null;
		}
		return domainForm.name();
	}
}
