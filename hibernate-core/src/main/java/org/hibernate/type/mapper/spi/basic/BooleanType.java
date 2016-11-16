/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.mapper.spi.basic;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.spi.java.basic.BooleanTypeDescriptor;
import org.hibernate.type.descriptor.spi.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.spi.sql.SqlTypeDescriptor;
import org.hibernate.type.mapper.spi.JdbcLiteralFormatter;

/**
 * A type that maps between {@link java.sql.Types#BOOLEAN BOOLEAN} and {@link Boolean}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class BooleanType
		extends BasicTypeImpl<Boolean>
		implements JdbcLiteralFormatter<Boolean> {
	public static final BooleanType INSTANCE = new BooleanType();

	public BooleanType() {
		this( BooleanTypeDescriptor.INSTANCE, org.hibernate.type.descriptor.spi.sql.BooleanTypeDescriptor.INSTANCE );
	}

	protected BooleanType(JavaTypeDescriptor<Boolean> javaTypeDescriptor, SqlTypeDescriptor sqlTypeDescriptor) {
		super( javaTypeDescriptor, sqlTypeDescriptor );
	}

	@Override
	public String getName() {
		return "boolean";
	}

	@Override
	public JdbcLiteralFormatter<Boolean> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toJdbcLiteral(Boolean value, Dialect dialect) {
		// We could also defer to the Dialect here, as in:
		//
		// return dialect.toBooleanValueString( value == null || value );
		//
		// which would ensure that the literal would "work" on that Dialect, but
		// considering that the Dialect should have been involved somehow already
		// in distinguishing the specific BasicType<Boolean> to use, I think
		// being more Type-specific is better
		return value == null || value ? "TRUE" : "FALSE";
	}
}
