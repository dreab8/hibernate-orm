/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.mapper.spi;

import org.hibernate.dialect.Dialect;

/**
 * A formatter object for creating JDBC literals of a given type.
 * <p/>
 * Generally this is obtained from the {@link Type#getJdbcLiteralFormatter} method
 * and would be specific to that Java+SQL type combo.
 *
 * @see org.hibernate.type.descriptor.spi.sql.JdbcLiteralFormatter
 * @see Type#getJdbcLiteralFormatter
 *
 * @author Steve Ebersole
 */
public interface JdbcLiteralFormatter<T> {
	String toJdbcLiteral(T value, Dialect dialect);
}
