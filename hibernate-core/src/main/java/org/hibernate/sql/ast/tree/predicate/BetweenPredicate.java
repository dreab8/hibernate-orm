/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.predicate;

import java.util.Collections;
import java.util.Set;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.select.SelectStatement;

/**
 * @author Steve Ebersole
 */
public class BetweenPredicate extends AbstractPredicate {
	private final Expression expression;
	private final Expression lowerBound;
	private final Expression upperBound;
	private final Set<String> affectedTableNames;

	public BetweenPredicate(
			Expression expression,
			Expression lowerBound,
			Expression upperBound,
			boolean negated,
			JdbcMappingContainer expressionType) {
		super( expressionType, negated );
		this.expression = expression;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		if ( expression instanceof SelectStatement ) {
			affectedTableNames = ( (SelectStatement) expression ).getAffectedTableNames();
		}
		else {
			affectedTableNames = Collections.emptySet();
		}
	}

	public Expression getExpression() {
		return expression;
	}

	public Expression getLowerBound() {
		return lowerBound;
	}

	public Expression getUpperBound() {
		return upperBound;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitBetweenPredicate( this );
	}

	@Override
	public Set<String> getAffectedTableNames() {
		return affectedTableNames;
	}
}
