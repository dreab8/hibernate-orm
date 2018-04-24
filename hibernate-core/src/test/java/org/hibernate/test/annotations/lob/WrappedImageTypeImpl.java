/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.lob;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.descriptor.java.internal.ByteArrayJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.LongVarbinarySqlDescriptor;

/**
 * A type that maps JDBC {@link java.sql.Types#LONGVARBINARY LONGVARBINARY} and {@code Byte[]}
 * 
 * @author Strong Liu
 */
public class WrappedImageTypeImpl extends BasicTypeImpl<Byte[]> {
	public static final WrappedImageTypeImpl INSTANCE = new WrappedImageTypeImpl();

	public WrappedImageTypeImpl() {
		super( LongVarbinarySqlDescriptor.INSTANCE, ByteArrayJavaDescriptor.INSTANCE );
	}

	public String getName() {
		// todo name these annotation types for addition to the registry
		return null;
	}
}
