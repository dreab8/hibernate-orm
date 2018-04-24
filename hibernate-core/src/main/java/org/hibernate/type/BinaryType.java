/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.internal.PrimitiveByteArrayJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.VarbinarySqlDescriptor;
import org.hibernate.type.internal.BasicTypeImpl;

/**
 * A type that maps between a {@link java.sql.Types#VARBINARY VARBINARY} and {@code byte[]}
 *
 * Implementation of the {@link VersionType} interface should be considered deprecated.
 * For binary entity versions/timestamps, {@link RowVersionType} should be used instead.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class BinaryType
		extends BasicTypeImpl<byte[]>
		 {

	public static final BinaryType INSTANCE = new BinaryType();

	public String getName() {
		return "binary";
	}

	public BinaryType() {
		super( VarbinarySqlDescriptor.INSTANCE, PrimitiveByteArrayJavaDescriptor.INSTANCE );
	}

//	@Override
//	public String[] getRegistrationKeys() {
//		return new String[] { getName(), "byte[]", byte[].class.getName() };
//	}

}
