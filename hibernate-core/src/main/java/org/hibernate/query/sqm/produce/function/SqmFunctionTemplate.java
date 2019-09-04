/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.Template;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Extension for supplying support for non-standard (ANSI SQL) functions
 * in HQL and Criteria queries.
 * <p/>
 * Ultimately acts as a factory for SQM function expressions.
 *
 * @author David Channon
 * @author Steve Ebersole
 */
public interface SqmFunctionTemplate {
	/**
	 * Generate an SqmExpression instance from this template.
	 * <p/>
	 * Note that this returns SqmExpression rather than the more
	 * restrictive SqmFunctionExpression to allow implementors
	 * to transform the source function expression into any
	 * "expressable form".
	 */
	<T> SelfRenderingSqmFunction<T> makeSqmFunctionExpression(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine);

	default <T> SelfRenderingSqmFunction<T> makeSqmFunctionExpression(
			SqmTypedNode<?> argument,
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine) {
		return makeSqmFunctionExpression(
				singletonList(argument),
				impliedResultType,
				queryEngine
		);
	}

	default <T> SelfRenderingSqmFunction<T> makeSqmFunctionExpression(
			AllowableFunctionReturnType<T> impliedResultType,
			QueryEngine queryEngine) {
		return makeSqmFunctionExpression(
				emptyList(),
				impliedResultType,
				queryEngine
		);
	}

	/**
	 * Will a call to the described function always include
	 * parentheses?
	 * <p>
	 * SqmFunctionTemplate is generally used for rendering of a function.
	 * However there are cases where Hibernate needs to consume a fragment
	 * and decide if a token represents a function name.  In cases where
	 * the token is followed by an open-paren we can safely assume the
	 * token is a function name.  However, if the next token is not an
	 * open-paren, the token can still represent a function provided that
	 * the function has a "no paren" form in the case of no arguments.  E.g.
	 * Many databases do not require parentheses on functions like
	 * `current_timestamp`, etc.  This method helps account for those
	 * cases.
	 * <p>
	 * Note that the most common case, by far, is that a function will always
	 * include the parentheses - therefore this return is defined as true by
	 * default.
	 */
	default boolean alwaysIncludesParentheses() {
		return true;
	}
}
