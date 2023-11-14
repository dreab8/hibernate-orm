/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.predicate;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.select.SelectStatement;

/**
 * @author Steve Ebersole
 */
public class ComparisonPredicate implements Predicate {
	private final Expression leftHandExpression;
	private final ComparisonOperator operator;
	private final Expression rightHandExpression;
	private final JdbcMappingContainer expressionType;
	private Set<String> affectedTableNames;

	public ComparisonPredicate(
			Expression leftHandExpression,
			ComparisonOperator operator,
			Expression rightHandExpression) {
		this( leftHandExpression, operator, rightHandExpression, null );
	}

	public ComparisonPredicate(
			Expression leftHandExpression,
			ComparisonOperator operator,
			Expression rightHandExpression,
			JdbcMappingContainer expressionType) {
		this.leftHandExpression = leftHandExpression;
		this.operator = operator;
		this.rightHandExpression = rightHandExpression;
		this.expressionType = expressionType;
		affectedTableNames = new HashSet<>();
		if ( leftHandExpression instanceof SelectStatement ) {
			affectedTableNames.addAll( ( (SelectStatement) leftHandExpression ).getAffectedTableNames() );
		}

		if ( rightHandExpression instanceof SelectStatement ) {
			affectedTableNames.addAll( ( (SelectStatement) rightHandExpression ).getAffectedTableNames() );
		}

		if ( affectedTableNames == null ) {
			affectedTableNames = Collections.emptySet();
		}
	}

	public Expression getLeftHandExpression() {
		return leftHandExpression;
	}

	public Expression getRightHandExpression() {
		return rightHandExpression;
	}

	public ComparisonOperator getOperator() {
		return operator;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public Set<String> getAffectedTableNames() {
		return affectedTableNames;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitRelationalPredicate( this );
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return expressionType;
	}
}
