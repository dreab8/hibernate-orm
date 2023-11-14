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
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.select.SelectStatement;

/**
 * @author Steve Ebersole
 */
public class LikePredicate extends AbstractPredicate {
	private final Expression matchExpression;
	private final Expression pattern;
	private final Expression escapeCharacter;
	private final boolean isCaseSensitive;
	private final Set<String> affectedTableNames;

	public LikePredicate(Expression matchExpression, Expression pattern) {
		this( matchExpression, pattern, null );
	}

	public LikePredicate(
			Expression matchExpression,
			Expression pattern,
			Expression escapeCharacter) {
		this( matchExpression, pattern, escapeCharacter, false );
	}

	public LikePredicate(
			Expression matchExpression,
			Expression pattern,
			Expression escapeCharacter,
			boolean negated) {
		this( matchExpression, pattern, escapeCharacter, negated, true, null );
	}

	public LikePredicate(
			Expression matchExpression,
			Expression pattern,
			Expression escapeCharacter,
			boolean negated,
			boolean isCaseSensitive,
			JdbcMappingContainer expressionType) {
		super( expressionType, negated );
		this.matchExpression = matchExpression;
		this.pattern = pattern;
		this.escapeCharacter = escapeCharacter;
		this.isCaseSensitive = isCaseSensitive;
		if ( matchExpression instanceof SelectStatement ) {
			affectedTableNames = ( (SelectStatement) matchExpression ).getAffectedTableNames();
		}
		else {
			affectedTableNames = Collections.emptySet();
		}
	}

	public Expression getMatchExpression() {
		return matchExpression;
	}

	public Expression getPattern() {
		return pattern;
	}

	public Expression getEscapeCharacter() {
		return escapeCharacter;
	}

	public boolean isCaseSensitive() {
		return isCaseSensitive;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitLikePredicate( this );
	}

	@Override
	public Set<String> getAffectedTableNames() {
		return affectedTableNames;
	}
}
