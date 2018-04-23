/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;

/**
 * Models the commonality between a column and a formula (computed value).
 */
public interface Selectable extends MappedColumn {
	/**
	 * @deprecated since 6.0
	 */
	@Deprecated
	String getAlias(Dialect dialect);
	/**
	 * @deprecated since 6.0
	 */
	@Deprecated
	String getAlias(Dialect dialect, Table table);

	/**
	 * @deprecated since 6.0, use {@link #getText()} instead.
	 */
	@Deprecated
	String getText(Dialect dialect);
	String getText();
}
