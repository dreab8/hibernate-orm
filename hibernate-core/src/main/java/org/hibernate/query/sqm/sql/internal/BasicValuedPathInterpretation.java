/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.domain.internal.DomainModelHelper;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.sql.SqlAstCreationState;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.results.internal.ScalarDomainResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class BasicValuedPathInterpretation<T> implements AssignableSqmPathInterpretation<T>, DomainResultProducer<T> {
	/**
	 * Static factory
	 */
	public static <T> BasicValuedPathInterpretation<T> from(
			SqmBasicValuedSimplePath<T> sqmPath,
			SqlAstCreationState sqlAstCreationState,
			SemanticQueryWalker sqmWalker) {
		final SqmPath<?> lhs = sqmPath.getLhs();
		assert lhs != null;

		final BasicValuedModelPart mapping = (BasicValuedModelPart) DomainModelHelper.resolveMappingModelExpressable( sqmPath, sqlAstCreationState );

		final TableGroup tableGroup = sqlAstCreationState.getFromClauseAccess().findTableGroup( sqmPath.getLhs().getNavigablePath() );

		// 1) "column info"
		//		a) name
		// 		b) JdbcMapping


		return new BasicValuedPathInterpretation<>( sqmPath, mapping, tableGroup );
	}

	private final SqmBasicValuedSimplePath<T> sqmPath;
	private final BasicValuedModelPart mapping;
	private final TableGroup tableGroup;

	private Expression expression;

	private BasicValuedPathInterpretation(
			SqmBasicValuedSimplePath<T> sqmPath,
			BasicValuedModelPart mapping,
			TableGroup tableGroup) {
		this.sqmPath = sqmPath;
		this.mapping = mapping;
		this.tableGroup = tableGroup;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return sqmPath.getNavigablePath();
	}

	@Override
	public SqmPathSource<T> getSqmPathSource() {
		return sqmPath.getReferencedPathSource();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainResultProducer

	@Override
	public DomainResultProducer<T> getDomainResultProducer(
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState) {
		return this;
	}

	@Override
	public DomainResult<T> createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		return mapping.createDomainResult( getNavigablePath(), tableGroup, resultVariable, creationState );
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		mapping.applySqlSelections( getNavigablePath(), tableGroup, creationState );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqmExpressionInterpretation

	@Override
	public Expression toSqlExpression(
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void applySqlAssignments(
			Expression newValueExpression,
			AssignmentContext assignmentProcessingState,
			Consumer<Assignment> assignmentConsumer,
			SqlAstCreationContext creationContext) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
