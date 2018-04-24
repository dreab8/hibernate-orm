/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.internal.IntegerJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.IntegerSqlDescriptor;
import org.hibernate.type.internal.BasicTypeImpl;

/**
 * A type that maps between {@link java.sql.Types#INTEGER INTEGER} and @link Integer}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class IntegerType extends BasicTypeImpl<Integer>
		implements PrimitiveType<Integer> {

	public static final IntegerType INSTANCE = new IntegerType();

	public static final Integer ZERO = 0;

	public IntegerType() {
		super( IntegerSqlDescriptor.INSTANCE, IntegerJavaDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "integer";
	}

//	@Override
//	public String[] getRegistrationKeys() {
//		return new String[] {getName(), int.class.getName(), Integer.class.getName()};
//	}

	@Override
	public Serializable getDefaultValue() {
		return ZERO;
	}

	@Override
	public Class getPrimitiveClass() {
		return int.class;
	}

	@Override
	public String objectToSQLString(Integer value, Dialect dialect) throws Exception {
		return toString( value );
	}
}
