/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.predicate;

import java.util.Set;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;

/**
 * @author Steve Ebersole
 */
public class GroupedPredicate implements Predicate {
	private final Predicate subPredicate;
	private final Set<String> affectedTableNames;

	public GroupedPredicate(Predicate subPredicate) {
		this.subPredicate = subPredicate;
		affectedTableNames = subPredicate.getAffectedTableNames();
	}

	public Predicate getSubPredicate() {
		return subPredicate;
	}

	@Override
	public boolean isEmpty() {
		return subPredicate.isEmpty();
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitGroupedPredicate( this );
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return subPredicate.getExpressionType();
	}

	@Override
	public Set<String> getAffectedTableNames() {
		return affectedTableNames;
	}
}
