/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.mapper.spi.basic;

import java.sql.Blob;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.spi.java.BlobTypeDescriptor;
import org.hibernate.type.mapper.spi.JdbcLiteralFormatter;

/**
 * A type that maps between {@link java.sql.Types#BLOB BLOB} and {@link Blob}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class BlobType extends BasicTypeImpl<Blob> {
	public static final BlobType INSTANCE = new BlobType();

	public BlobType() {
		super( BlobTypeDescriptor.INSTANCE, org.hibernate.type.descriptor.spi.sql.BlobTypeDescriptor.DEFAULT );
	}

	@Override
	public String getName() {
		return "blob";
	}

	@Override
	public Blob getReplacement(Blob original, Blob target, SharedSessionContractImplementor session) {
		return session.getJdbcServices().getJdbcEnvironment().getDialect().getLobMergeStrategy().mergeBlob( original, target, session );
	}

	@Override
	public JdbcLiteralFormatter<Blob> getJdbcLiteralFormatter() {
		// no literal support for BLOB data
		return null;
	}

}
