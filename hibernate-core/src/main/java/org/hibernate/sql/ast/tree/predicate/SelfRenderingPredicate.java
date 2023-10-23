/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.predicate;

import java.util.Collections;
import java.util.Set;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.select.SelectStatement;

/**
 *
 * @author Chris Cranford
 */
public class SelfRenderingPredicate implements Predicate {
	private final SelfRenderingExpression selfRenderingExpression;
	private final Set<String> affectedTableNames;

	public SelfRenderingPredicate(SelfRenderingExpression expression) {
		this.selfRenderingExpression = expression;
		if ( expression instanceof SelectStatement ) {
			affectedTableNames = ( (SelectStatement) expression ).getAffectedTableNames();
		}
		else {
			affectedTableNames = Collections.emptySet();
		}
	}

	public SelfRenderingExpression getSelfRenderingExpression() {
		return selfRenderingExpression;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitSelfRenderingPredicate( this );
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return selfRenderingExpression.getExpressionType();
	}

	@Override
	public Set<String> getAffectedTableNames() {
		return affectedTableNames;
	}
}
