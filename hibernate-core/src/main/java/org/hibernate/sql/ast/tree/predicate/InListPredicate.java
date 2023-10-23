/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.predicate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.select.SelectStatement;

/**
 * @author Steve Ebersole
 */
public class InListPredicate extends AbstractPredicate {
	private final Expression testExpression;
	private final List<Expression> listExpressions;
	private Set<String> affectedTableNames;

	public InListPredicate(Expression testExpression) {
		this( testExpression, new ArrayList<>() );
	}

	public InListPredicate(Expression testExpression, boolean negated, JdbcMappingContainer expressionType) {
		this( testExpression, new ArrayList<>(), negated, expressionType );
	}

	public InListPredicate(Expression testExpression, Expression... listExpressions) {
		this( testExpression, ArrayHelper.toExpandableList( listExpressions ) );
	}

	public InListPredicate(
			Expression testExpression,
			List<Expression> listExpressions) {
		this( testExpression, listExpressions, false, null );
	}

	public InListPredicate(
			Expression testExpression,
			List<Expression> listExpressions,
			boolean negated,
			JdbcMappingContainer expressionType) {
		super( expressionType, negated );
		this.testExpression = testExpression;
		this.listExpressions = listExpressions;
		if ( testExpression instanceof SelectStatement ) {
			affectedTableNames = ( (SelectStatement) testExpression ).getAffectedTableNames();
		}
		for ( Expression expression : listExpressions ) {
			if ( testExpression instanceof SelectStatement ) {
				if ( affectedTableNames == null ) {
					affectedTableNames = new HashSet<>();
				}
				affectedTableNames.addAll( ( (SelectStatement) expression ).getAffectedTableNames() );
			}
		}
		if ( affectedTableNames == null ) {
			affectedTableNames = Collections.emptySet();
		}
	}

	public Expression getTestExpression() {
		return testExpression;
	}

	public List<Expression> getListExpressions() {
		return listExpressions;
	}

	public void addExpression(Expression expression) {
		listExpressions.add( expression );
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitInListPredicate( this );
	}

	@Override
	public Set<String> getAffectedTableNames() {
		return affectedTableNames;
	}
}
