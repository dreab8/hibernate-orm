/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Comparator;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.RowVersionTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.VarbinarySqlDescriptor;
import org.hibernate.type.internal.BasicTypeImpl;

/**
 * A type that maps between a {@link java.sql.Types#VARBINARY VARBINARY} and {@code byte[]}
 * specifically for entity versions/timestamps.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class RowVersionTypeImpl
		extends BasicTypeImpl<byte[]>
		implements VersionType<byte[]> {

	public static final RowVersionTypeImpl INSTANCE = new RowVersionTypeImpl();

	public String getName() {
		return "row_version";
	}

	public RowVersionTypeImpl() {
		super( VarbinarySqlDescriptor.INSTANCE, RowVersionTypeDescriptor.INSTANCE );
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName() };
	}

	@Override
	public byte[] seed(SharedSessionContractImplementor session) {
		// Note : simply returns null for seed() and next() as the only known
		// 		application of binary types for versioning is for use with the
		// 		TIMESTAMP datatype supported by Sybase and SQL Server, which
		// 		are completely db-generated values...
		return null;
	}

	@Override
	public byte[] next(byte[] current, SharedSessionContractImplementor session) {
		return current;
	}

	@Override
	public Comparator<byte[]> getComparator() {
		return getJavaTypeDescriptor().getComparator();
	}
}
