/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.mapper.spi.basic;

import org.hibernate.type.descriptor.spi.java.ByteArrayTypeDescriptor;
import org.hibernate.type.descriptor.spi.sql.VarbinaryTypeDescriptor;
import org.hibernate.type.mapper.spi.JdbcLiteralFormatter;

/**
 * A type mapping {@link java.sql.Types#VARBINARY VARBINARY} and {@link Byte Byte[]}
 * 
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class WrapperBinaryType extends BasicTypeImpl<Byte[]> {
	public static final WrapperBinaryType INSTANCE = new WrapperBinaryType();

	public WrapperBinaryType() {
		super( ByteArrayTypeDescriptor.INSTANCE, VarbinaryTypeDescriptor.INSTANCE );
	}

	public String getName() {
		//TODO find a decent name beforeQuery documenting
		return "wrapper-binary";
	}

	@Override
	public JdbcLiteralFormatter<Byte[]> getJdbcLiteralFormatter() {
		// no literal support for binary data
		return null;
	}
}
