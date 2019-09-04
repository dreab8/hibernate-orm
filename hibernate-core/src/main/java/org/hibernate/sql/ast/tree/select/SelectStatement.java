/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.select;

import org.hibernate.sql.ast.tree.Statement;

/**
 * @author Steve Ebersole
 */
public class SelectStatement implements Statement {
	private final QuerySpec querySpec;

	public SelectStatement(QuerySpec querySpec) {
		this.querySpec = querySpec;
	}

	public QuerySpec getQuerySpec() {
		return querySpec;
	}
}
