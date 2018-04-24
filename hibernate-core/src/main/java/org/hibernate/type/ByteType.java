/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.util.Comparator;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.internal.ByteJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.TinyIntSqlDescriptor;
import org.hibernate.type.internal.BasicTypeImpl;

/**
 * A type that maps between {@link java.sql.Types#TINYINT TINYINT} and {@link Byte}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class ByteType
		extends BasicTypeImpl<Byte>
		implements PrimitiveType<Byte> {

	public static final ByteType INSTANCE = new ByteType();

	private static final Byte ZERO = (byte) 0;

	public ByteType() {
		super( TinyIntSqlDescriptor.INSTANCE, ByteJavaDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "byte";
	}

//	@Override
//	public String[] getRegistrationKeys() {
//		return new String[] {getName(), byte.class.getName(), Byte.class.getName()};
//	}

	@Override
	public Serializable getDefaultValue() {
		return ZERO;
	}

	@Override
	public Class getPrimitiveClass() {
		return byte.class;
	}

	@Override
	public String objectToSQLString(Byte value, Dialect dialect) {
		return toString( value );
	}

	@Override
	public Byte fromStringValue(String xml) {
		return fromString( xml );
	}

}
