/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.spi;

import java.sql.Types;

/**
 * Descriptor for {@link Types#LONGVARCHAR LONGVARCHAR} handling.
 *
 * @author Steve Ebersole
 */
public class LongVarcharSqlDescriptor extends VarcharSqlDescriptor {
	public static final LongVarcharSqlDescriptor INSTANCE = new LongVarcharSqlDescriptor();

	public LongVarcharSqlDescriptor() {
	}

	@Override
	public int getSqlType() {
		return Types.LONGVARCHAR;
	}

	@Override
	public int getJdbcTypeCode() {
		return getSqlType();
	}
}
