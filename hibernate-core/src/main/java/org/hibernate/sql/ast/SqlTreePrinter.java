/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast;

import java.util.Locale;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.SortOrder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.SqlAstTreeLogger;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.EntityTypeLiteral;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SelfRenderingExpression;
import org.hibernate.sql.ast.tree.expression.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.UnaryOperation;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.insert.InsertSelectStatement;
import org.hibernate.sql.ast.tree.predicate.BetweenPredicate;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.FilterPredicate;
import org.hibernate.sql.ast.tree.predicate.GroupedPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.predicate.NegatedPredicate;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.SelfRenderingPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;

/**
 * @author Steve Ebersole
 */
public class SqlTreePrinter implements SqlAstWalker {
	public static void print(Statement sqlAstStatement) {
		if ( ! SqlAstTreeLogger.DEBUG_ENABLED ) {
			return;
		}

		final SqlTreePrinter printer = new SqlTreePrinter();
		printer.visitStatement( sqlAstStatement );

		SqlAstTreeLogger.INSTANCE.debugf( "SQL AST Tree:\n" + printer.buffer.toString() );
	}

	private final StringBuffer buffer = new StringBuffer();
	private int depth = 2;

	private SqlTreePrinter() {
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		// should not be needed.
		// todo (6.0) : in fact we really should consider dropping this from the interface
		return null;
	}

	private void visitStatement(Statement sqlAstStatement) {
		if ( sqlAstStatement instanceof SelectStatement ) {
			logNode(
					"select-statement",
					() -> visitQuerySpec( ( (SelectStatement) sqlAstStatement ).getQuerySpec() )
			);
		}
		else if ( sqlAstStatement instanceof DeleteStatement ) {
			final DeleteStatement deleteStatement = (DeleteStatement) sqlAstStatement;
			logNode(
					"delete-statement",
					() -> {
						logNode(
								"target",
								() -> logNode( deleteStatement.getTargetTable().toString() )
						);
						logNode(
								"where",
								() -> {
									if ( deleteStatement.getRestriction() != null ) {
										deleteStatement.getRestriction().accept( this );
									}
								}
						);
					}
			);
		}
		else if ( sqlAstStatement instanceof UpdateStatement ) {
			final UpdateStatement updateStatement = (UpdateStatement) sqlAstStatement;
			logNode(
					"update-statement",
					() -> {
						logNode(
								"target",
								() -> logNode( updateStatement.getTargetTable().toString() )
						);
						logNode(
								"set",
								() -> {
									for ( Assignment assignment : updateStatement.getAssignments() ) {
										logNode(
												"assignment",
												() -> assignment.accept( this ),
												true
										);
									}
								}
						);
						logNode(
								"where",
								() -> {
									if ( updateStatement.getRestriction() != null ) {
										updateStatement.getRestriction().accept( this );
									}
								}
						);
					}
			);
		}
		else if ( sqlAstStatement instanceof InsertSelectStatement ) {
			final InsertSelectStatement insertSelectStatement = (InsertSelectStatement) sqlAstStatement;
			logNode(
					"insert-select-statement",
					() -> {
						logNode(
								"target",
								() -> logNode( insertSelectStatement.getTargetTable().toString() )
						);
						logNode(
								"into",
								() -> {
									for ( ColumnReference spec : insertSelectStatement.getTargetColumnReferences() ) {
										logNode(
												"target-column",
												() -> spec.accept( this )
										);
									}
								}
						);
						logNode(
								"select",
								() -> visitQuerySpec( insertSelectStatement.getSourceSelectStatement() )
						);
					}
			);
		}
		else {
			throw new UnsupportedOperationException( "Printing for this type of SQL AST not supported : " + sqlAstStatement );
		}
	}

	private void logNode(String text) {
		logWithIndentation( "-> [%s]", text );
	}

	private void logNode(String pattern, Object arg) {
		logWithIndentation( "-> [" + String.format( pattern, arg ) + ']');
	}

	private void logNode(String text, Runnable subTreeHandler) {
		logNode( text, subTreeHandler, false );
	}

	private void logNode(String text, Runnable subTreeHandler, boolean indentContinuation) {
		logWithIndentation( "-> [%s]", text );
		depth++;

		try {
			if ( indentContinuation ) {
				depth++;
			}
			subTreeHandler.run();
		}
		catch (Exception e) {
			SqlAstTreeLogger.INSTANCE.debugf( e, "Error processing node {%s}", text );
		}
		finally {
			if ( indentContinuation ) {
				depth--;
			}
		}

		depth--;
		logWithIndentation( "<- [%s]", text );
	}

	private void logWithIndentation(Object line) {
		pad( depth );
		buffer.append( line ).append( '\n' );
	}

	private void logWithIndentation(String pattern, Object arg1) {
		logWithIndentation( String.format( pattern, arg1 ) );
	}

	private void logWithIndentation(String pattern, Object arg1, Object arg2) {
		logWithIndentation( String.format( pattern, arg1, arg2 ) );
	}

	private void logWithIndentation(String pattern, Object... args) {
		logWithIndentation(  String.format( pattern, args ) );
	}

	private void pad(int depth) {
		for ( int i = 0; i < depth; i++ ) {
			buffer.append( "  " );
		}
	}

	@Override
	public void visitAssignment(Assignment assignment) {

	}

	@Override
	public void visitQuerySpec(QuerySpec querySpec) {
		logNode(
				"query-spec",
				() -> {
					visitSelectClause( querySpec.getSelectClause() );
					visitFromClause( querySpec.getFromClause() );

					if ( querySpec.getWhereClauseRestrictions() != null && ! querySpec.getWhereClauseRestrictions().isEmpty() ) {
						logNode(
								"where",
								() -> querySpec.getWhereClauseRestrictions().accept( this )
						);
					}

					// todo (6.0) : group-by
					// todo (6.0) : having

					if ( ! querySpec.getSortSpecifications().isEmpty() ) {
						logNode(
								"order-by",
								() -> {
									for ( SortSpecification sortSpecification : querySpec.getSortSpecifications() ) {
										visitSortSpecification( sortSpecification );
									}
								}
						);
					}

					if ( querySpec.getLimitClauseExpression() != null ) {
						logNode(
								"limit",
								() -> querySpec.getLimitClauseExpression().accept( this )
						);
					}

					if ( querySpec.getOffsetClauseExpression() != null ) {
						logNode(
								"offset",
								() -> querySpec.getOffsetClauseExpression().accept( this )

						);
					}
				}
		);
	}

	@Override
	public void visitSortSpecification(SortSpecification sortSpecification) {
		logNode(
				"sort",
				() -> {
					sortSpecification.getSortExpression().accept( this );
					logNode(
							sortSpecification.getSortOrder() == null
									? SortOrder.ASCENDING.name()
									: sortSpecification.getSortOrder().name()
					);
				}
		);
	}

	@Override
	public void visitLimitOffsetClause(QuerySpec querySpec) {
		throw new UnsupportedOperationException( "Unexpected call to #visitLimitOffsetClause" );
	}

	@Override
	public void visitSelectClause(SelectClause selectClause) {
		logNode(
				"select",
				() -> {
					for ( SqlSelection sqlSelection : selectClause.getSqlSelections() ) {
						visitSqlSelection( sqlSelection );
					}
				}
		);
	}

	@Override
	public void visitSqlSelection(SqlSelection sqlSelection) {
		logWithIndentation( "selection - " + sqlSelection );
	}

	@Override
	public void visitFromClause(FromClause fromClause) {
		logNode(
				"from",
				() -> fromClause.visitRoots( this::visitTableGroup )
		);
	}

	@Override
	public void visitTableGroup(TableGroup tableGroup) {
		logNode(
				"table-group",
				() -> {
					visitTableReference( tableGroup.getPrimaryTableReference() );

					for ( TableReferenceJoin join : tableGroup.getTableReferenceJoins() ) {
						visitTableReferenceJoin( join );
					}

					tableGroup.visitTableGroupJoins( this::visitTableGroupJoin );
				}
		);
	}

	@Override
	public void visitTableGroupJoin(TableGroupJoin tableGroupJoin) {
		logNode(
				"table-group-join",
				() -> {
					visitTableGroup( tableGroupJoin.getJoinedGroup() );
					logNode( tableGroupJoin.getJoinType().getText() );
					logNode(
							"on",
							() -> tableGroupJoin.getPredicate().accept( this )
					);
				}
		);
	}

	@Override
	public void visitTableReference(TableReference tableReference) {
		logNode( tableReference.getTableExpression() + " as " + tableReference.getIdentificationVariable() );
	}

	@Override
	public void visitTableReferenceJoin(TableReferenceJoin tableReferenceJoin) {
		logNode(
				"join",
				() -> {
					visitTableReference( tableReferenceJoin.getJoinedTableReference() );
					logNode( tableReferenceJoin.getJoinType().getText() );
					logNode(
							"on",
							() -> tableReferenceJoin.getJoinPredicate().accept( this )
					);
				}
		);
	}

	@Override
	public void visitColumnReference(ColumnReference columnReference) {
		logNode( columnReference.renderSqlFragment( getSessionFactory() ) );
	}

	@Override
	public void visitBinaryArithmeticExpression(BinaryArithmeticExpression expression) {
		logNode(
				expression.getOperator().getOperatorSqlTextString(),
				() -> {
					expression.getLeftHandOperand().accept( this );
					expression.getRightHandOperand().accept( this );
				}
		);
	}

	@Override
	public void visitSqlSelectionExpression(SqlSelectionExpression expression) {
		logNode( "selection-reference (%s)", expression.getSelection().getJdbcResultSetIndex() );
	}

	@Override
	public void visitEntityTypeLiteral(EntityTypeLiteral expression) {

	}

	@Override
	public void visitTuple(SqlTuple tuple) {
		logNode(
				"tuple",
				() -> tuple.getExpressions().forEach( expr -> expr.accept( this ) )
		);
	}

	@Override
	public void visitCaseSearchedExpression(CaseSearchedExpression caseSearchedExpression) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public void visitCaseSimpleExpression(CaseSimpleExpression caseSimpleExpression) {
		throw new NotYetImplementedFor6Exception();
	}

//	@Override
//	public void visitCoalesceFunction(CoalesceFunction coalesceExpression) {
//		throw new NotYetImplementedFor6Exception();
//	}
//
//	@Override
//	public void visitNamedParameter(NamedParameter namedParameter) {
//		logNode( "named-param (%s)", namedParameter.getName() );
//	}
//
//	@Override
//	public void visitGenericParameter(GenericParameter parameter) {
//		logNode( "generic-param" );
//	}
//
//	@Override
//	public void visitPositionalParameter(PositionalParameter parameter) {
//		logNode( "positional-param (%s)", parameter.getPosition() );
//	}

	@Override
	public void visitQueryLiteral(QueryLiteral queryLiteral) {
		logNode( "literal (" + queryLiteral.getValue() + ')' );
	}

	@Override
	public void visitUnaryOperationExpression(UnaryOperation operation) {
		logNode(
				operation.getOperator().name().toLowerCase( Locale.ROOT ),
				() -> operation.getOperand().accept( this )
		);
	}

	@Override
	public void visitBetweenPredicate(BetweenPredicate betweenPredicate) {
		logNode(
				"between",
				() -> {
					betweenPredicate.getExpression().accept( this );
					betweenPredicate.getLowerBound().accept( this );
					betweenPredicate.getUpperBound().accept( this );
				}
		);
	}

	@Override
	public void visitFilterPredicate(FilterPredicate filterPredicate) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public void visitGroupedPredicate(GroupedPredicate groupedPredicate) {
		logNode(
				"grouped-predicate",
				() -> groupedPredicate.getSubPredicate().accept( this )
		);
	}

	@Override
	public void visitInListPredicate(InListPredicate inListPredicate) {
		logNode(
				"in",
				() -> {
					inListPredicate.getTestExpression().accept( this );
					logNode(
							"list",
							() -> inListPredicate.getListExpressions().forEach( expr -> expr.accept( this ) )
					);
				}
		);
	}

	@Override
	public void visitInSubQueryPredicate(InSubQueryPredicate inSubQueryPredicate) {
		logNode(
				"in",
				() -> {
					inSubQueryPredicate.getTestExpression().accept( this );
					visitQuerySpec( inSubQueryPredicate.getSubQuery() );
				}
		);
	}

	@Override
	public void visitJunction(Junction junction) {
		logNode(
				junction.getNature().name().toLowerCase(),
				() -> junction.getPredicates().forEach( predicate -> predicate.accept( this ) )
		);
	}

	@Override
	public void visitLikePredicate(LikePredicate likePredicate) {
		logNode(
				"like",
				() -> {
					likePredicate.getMatchExpression().accept( this );
					likePredicate.getPattern().accept( this );
					if ( likePredicate.getEscapeCharacter() != null ) {
						likePredicate.getEscapeCharacter().accept( this );
					}
				}
		);
	}

	@Override
	public void visitNegatedPredicate(NegatedPredicate negatedPredicate) {
		logNode(
				"not",
				() -> negatedPredicate.getPredicate().accept( this )
		);
	}

	@Override
	public void visitNullnessPredicate(NullnessPredicate nullnessPredicate) {
	}

	@Override
	public void visitRelationalPredicate(ComparisonPredicate comparisonPredicate) {
		logNode(
				comparisonPredicate.getOperator().name().toLowerCase( Locale.ROOT ),
				() -> {
					comparisonPredicate.getLeftHandExpression().accept( this );
					comparisonPredicate.getRightHandExpression().accept( this );
				}
		);
	}

	@Override
	public void visitSelfRenderingPredicate(SelfRenderingPredicate selfRenderingPredicate) {
	}

	@Override
	public void visitSelfRenderingExpression(SelfRenderingExpression expression) {

	}

//	@Override
//	public void visitNonStandardFunctionExpression(NonStandardFunction function) {
//
//	}
//
//	@Override
//	public void visitAbsFunction(AbsFunction function) {
//
//	}
//
//	@Override
//	public void visitAvgFunction(AvgFunction function) {
//
//	}
//
//	@Override
//	public void visitBitLengthFunction(BitLengthFunction function) {
//
//	}
//
//	@Override
//	public void visitCastFunction(CastFunction function) {
//
//	}
//
//	@Override
//	public void visitConcatFunction(ConcatFunction function) {
//
//	}
//
//	@Override
//	public void visitSubstrFunction(SubstrFunction function) {
//
//	}
//
//	@Override
//	public void visitCountFunction(CountFunction function) {
//		logNode(
//				"count",
//				() -> function.getArgument().accept( this )
//		);
//	}
//
//	@Override
//	public void visitCountStarFunction(CountStarFunction function) {
//		logNode( "count(*)" );
//	}
//
//	@Override
//	public void visitCurrentDateFunction(CurrentDateFunction function) {
//		logNode( "current_date" );
//	}
//
//	@Override
//	public void visitCurrentTimeFunction(CurrentTimeFunction function) {
//		logNode( "current_time" );
//	}
//
//	@Override
//	public void visitCurrentTimestampFunction(CurrentTimestampFunction function) {
//		logNode( "current_timestamp" );
//	}

//	@Override
//	public void visitExtractFunction(ExtractFunction extractFunction) {
//
//	}
//
//	@Override
//	public void visitLengthFunction(LengthFunction function) {
//
//	}
//
//	@Override
//	public void visitLocateFunction(LocateFunction function) {
//
//	}
//
//	@Override
//	public void visitLowerFunction(LowerFunction function) {
//
//	}
//
//	@Override
//	public void visitMaxFunction(MaxFunction function) {
//
//	}
//
//	@Override
//	public void visitMinFunction(MinFunction function) {
//
//	}
//
//	@Override
//	public void visitModFunction(ModFunction function) {
//
//	}
//
//	@Override
//	public void visitNullifFunction(NullifFunction function) {
//
//	}
//
//	@Override
//	public void visitSqrtFunction(SqrtFunction function) {
//
//	}
//
//	@Override
//	public void visitSumFunction(SumFunction function) {
//
//	}
//
//	@Override
//	public void visitTrimFunction(TrimFunction function) {
//
//	}
//
//	@Override
//	public void visitUpperFunction(UpperFunction function) {
//
//	}

}
