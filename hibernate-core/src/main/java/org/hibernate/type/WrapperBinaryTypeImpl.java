/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;
import org.hibernate.type.descriptor.java.internal.ByteArrayJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.VarbinarySqlDescriptor;
import org.hibernate.type.internal.BasicTypeImpl;

/**
 * A type mapping {@link java.sql.Types#VARBINARY VARBINARY} and {@link Byte Byte[]}
 * 
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class WrapperBinaryTypeImpl extends BasicTypeImpl<Byte[]> {
	public static final WrapperBinaryTypeImpl INSTANCE = new WrapperBinaryTypeImpl();

	public WrapperBinaryTypeImpl() {
		super( VarbinarySqlDescriptor.INSTANCE, ByteArrayJavaDescriptor.INSTANCE );
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), "Byte[]", Byte[].class.getName() };
	}

	public String getName() {
		//TODO find a decent name before documenting
		return "wrapper-binary";
	}
}
