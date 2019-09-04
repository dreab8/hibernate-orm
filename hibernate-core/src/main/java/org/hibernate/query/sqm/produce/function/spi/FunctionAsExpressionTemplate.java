/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.spi;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.consume.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class FunctionAsExpressionTemplate
		extends AbstractSelfRenderingFunctionTemplate
		implements SelfRenderingFunctionSupport {

	private static final Logger log = Logger.getLogger( FunctionAsExpressionTemplate.class );

	private final String expressionStart;
	private final String argumentSeparator;
	private final String expressionEnd;

	public FunctionAsExpressionTemplate(
			String expressionStart,
			String argumentSeparator,
			String expressionEnd,
			FunctionReturnTypeResolver returnTypeResolver,
			ArgumentsValidator argumentsValidator,
			String name) {
		super( name, returnTypeResolver, argumentsValidator );
		this.expressionStart = expressionStart;
		this.argumentSeparator = argumentSeparator;
		this.expressionEnd = expressionEnd;
	}

	@Override
	protected SelfRenderingFunctionSupport getRenderingFunctionSupport(
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<?> resolvedReturnType,
			QueryEngine queryEngine) {
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> sqlAstArguments,
			SqlAstWalker walker,
			SessionFactoryImplementor sessionFactory) {
		sqlAppender.appendSql( expressionStart );

		if ( sqlAstArguments.isEmpty() ) {
			log.debugf( "No arguments found for FunctionAsExpressionTemplate, this is most likely a query syntax error" );
		}
		else {
			// render the first argument..
			renderArgument( sqlAppender, sqlAstArguments.get( 0 ), walker, sessionFactory );

			// render the rest of the arguments, preceded by the separator
			for ( int i = 1; i < sqlAstArguments.size(); i++ ) {
				sqlAppender.appendSql( argumentSeparator );
				renderArgument( sqlAppender, sqlAstArguments.get( i ), walker, sessionFactory );
			}
		}

		sqlAppender.appendSql( expressionEnd );
	}

	/**
	 * Called from {@link #render} to render an argument.
	 *
	 * @param sqlAppender The sql appender to append the rendered argument.
	 * @param sqlAstArgument The argument being processed.
	 * @param walker The walker to use for rendering {@link SqlAstNode} expressions
	 * @param sessionFactory The session factory
	 */
	protected void renderArgument(
			SqlAppender sqlAppender,
			SqlAstNode sqlAstArgument,
			SqlAstWalker walker,
			SessionFactoryImplementor sessionFactory) {
		sqlAstArgument.accept( walker );
	}
}
