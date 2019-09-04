/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import org.hibernate.query.QueryLogger;

import org.jboss.logging.Logger;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

/**
 * Used to
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class HqlParseTreePrinter extends HqlParserBaseListener {
	private static final Logger HQL_LOGGER = QueryLogger.subLogger( "hql.parseTree" );
	private static final boolean LOG_DEBUG_ENABLED = HQL_LOGGER.isDebugEnabled();

	public static void logStatementParseTree(HqlParser parser) {
		if ( !LOG_DEBUG_ENABLED ) {
			return;
		}

		final HqlParseTreePrinter walker = new HqlParseTreePrinter( parser );
		ParseTreeWalker.DEFAULT.walk( walker, parser.statement() );

		HQL_LOGGER.debugf( "HQL parse-tree:\n%s", walker.buffer.toString() );

		parser.reset();
	}

	public static void logOrderByParseTree(HqlParser parser) {
		if ( !LOG_DEBUG_ENABLED ) {
			return;
		}

		final HqlParseTreePrinter walker = new HqlParseTreePrinter( parser );
		ParseTreeWalker.DEFAULT.walk( walker, parser.orderByClause() );

		HQL_LOGGER.debugf( "Mapping order-by parse-tree:\n%s", walker.buffer.toString() );

		parser.reset();
	}

	private final HqlParser parser;
	private final StringBuffer buffer = new StringBuffer();
	private int depth = 2;

	public HqlParseTreePrinter(HqlParser parser) {
		this.parser = parser;
	}

	private enum LineType {
		ENTER,
		EXIT
	}

	@Override
	public void enterEveryRule(ParserRuleContext ctx) {
		final String ruleName = parser.getRuleNames()[ctx.getRuleIndex()];

		if ( !ruleName.endsWith( "Keyword" ) ) {
			applyLine( LineType.ENTER, ruleName, ruleTypeName( ctx.getClass() ), ctx.getText() );
		}

		super.enterEveryRule( ctx );
	}

	private String ruleTypeName(Class<? extends ParserRuleContext> ruleCtxClass) {
		final String name = ruleCtxClass.getSimpleName();

		final int position = name.lastIndexOf( "Context" );
		if ( position > 1 ) {
			return name.substring( 0, position );
		}
		else {
			// todo (6.0) : does this ever happen?
			return name;
		}
	}

	private void applyLine(
			LineType lineType,
			String ruleName,
			String ruleTypeName,
			String ctxText) {
		applyLinePadding( lineType );

		buffer.append( '[' ).append( ruleName );

		if ( ! ruleTypeName.equalsIgnoreCase( ruleName ) ) {
			buffer.append( " (" ).append( ruleTypeName ).append( ')' );
		}

		buffer.append( ']' )
				.append( " - `" ).append( ctxText ).append( '`' )
				.append( '\n' );
	}

	private void applyLinePadding(LineType lineType) {
		if ( lineType == LineType.ENTER ) {
			pad( depth++ );
			buffer.append( "-> " );
		}
		else {
			pad( --depth );
			buffer.append( "<- " );
		}

	}

	private String pad(int depth) {
		for ( int i = 0; i < depth; i++ ) {
			buffer.append( "  " );
		}
		return buffer.toString();
	}

	@Override
	public void exitEveryRule(ParserRuleContext ctx) {
		super.exitEveryRule( ctx );

		final String ruleName = parser.getRuleNames()[ctx.getRuleIndex()];

		applyLine( LineType.EXIT, ruleName, ruleTypeName( ctx.getClass() ), ctx.getText() );
	}
}
