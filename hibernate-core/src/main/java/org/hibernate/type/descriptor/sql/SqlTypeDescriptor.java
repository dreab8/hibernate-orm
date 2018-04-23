/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql;

import java.io.Serializable;

/**
 * Descriptor for the <tt>SQL</tt>/<tt>JDBC</tt> side of a value mapping.
 * <p/>
 * NOTE : Implementations should be registered with the {@link SqlTypeDescriptor}.  The built-in Hibernate
 * implementations register themselves on construction.
 *
 * @author Steve Ebersole
 */
public interface SqlTypeDescriptor extends Serializable {

	/**
	 * Return the {@linkplain java.sql.Types JDBC type-code} for the column mapped by this type.
	 *
	 * @return typeCode The JDBC type-code
	 */
	int getJdbcTypeCode();

	/**
	 * Return the {@linkplain java.sql.Types JDBC type-code} for the column mapped by this type.
	 *
	 * @return typeCode The JDBC type-code
	 * @deprecated since 6.0, use {@link #getJdbcTypeCode()} instead.
	 */
	@Deprecated
	default int getSqlType(){
		return getJdbcTypeCode();
	}

	/**
	 * Is this descriptor available for remapping?
	 *
	 * @return {@code true} indicates this descriptor can be remapped; otherwise, {@code false}
	 *
	 * @see org.hibernate.type.descriptor.WrapperOptions#remapSqlTypeDescriptor
	 * @see org.hibernate.dialect.Dialect#remapSqlTypeDescriptor
	 */
	boolean canBeRemapped();
}
