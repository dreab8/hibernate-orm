/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.NClob;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.internal.NClobJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.NClobSqlDescriptor;
import org.hibernate.type.internal.BasicTypeImpl;

/**
 * A type that maps between {@link java.sql.Types#CLOB CLOB} and {@link java.sql.Clob}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class NClobTypeImpl extends BasicTypeImpl<NClob> {
	public static final NClobTypeImpl INSTANCE = new NClobTypeImpl();

	public NClobTypeImpl() {
		super( NClobSqlDescriptor.DEFAULT, NClobJavaDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "nclob";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	protected NClob getReplacement(NClob original, NClob target, SharedSessionContractImplementor session) {
		return session.getJdbcServices().getJdbcEnvironment().getDialect().getLobMergeStrategy().mergeNClob( original, target, session );
	}

}
