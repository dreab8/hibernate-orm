/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.List;
import java.util.Objects;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.mapping.ColumnConsumer;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableResultImpl;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Andrea Boriero
 */
public class EmbeddedForeignKeyDescriptor implements ForeignKeyDescriptor, ModelPart {

	private final String keyColumnContainingTable;
	private final List<String> keyColumnExpressions;
	private final String targetColumnContainingTable;
	private final List<String> targetColumnExpressions;
	private final EmbeddableValuedModelPart mappingType;
	private final List<JdbcMapping> jdbcMappings;
	private final ForeignKeyDirection fKeyDirection;
	private final int hasCode;

	public EmbeddedForeignKeyDescriptor(
			ForeignKeyDirection fKeyDirection,
			String keyColumnContainingTable,
			List<String> keyColumnExpressions,
			String targetColumnContainingTable,
			List<String> targetColumnExpressions,
			EmbeddableValuedModelPart mappingType,
			List<JdbcMapping> jdbcMappings) {
		this.keyColumnContainingTable = keyColumnContainingTable;
		this.keyColumnExpressions = keyColumnExpressions;
		this.targetColumnContainingTable = targetColumnContainingTable;
		this.targetColumnExpressions = targetColumnExpressions;
		this.mappingType = mappingType;
		this.fKeyDirection = fKeyDirection;
		this.jdbcMappings = jdbcMappings;

		this.hasCode = Objects.hash(
				keyColumnContainingTable,
				keyColumnExpressions,
				targetColumnContainingTable,
				targetColumnExpressions
		);

	}

	@Override
	public ForeignKeyDirection getDirection() {
		return fKeyDirection;
	}

	@Override
	public DomainResult createCollectionFetchDomainResult(
			NavigablePath collectionPath, TableGroup tableGroup, DomainResultCreationState creationState) {
		return createDomainResult( collectionPath, tableGroup, creationState );
	}

	@Override
	public DomainResult createDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		//noinspection unchecked
		return new EmbeddableResultImpl(
				collectionPath,
				mappingType,
				null,
				creationState
		);
	}

	@Override
	public Predicate generateJoinPredicate(
			TableGroup lhs,
			TableGroup tableGroup,
			SqlAstJoinType sqlAstJoinType,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		TableReference lhsTableReference;
		TableReference rhsTableKeyReference;
		if ( targetColumnContainingTable.equals( keyColumnContainingTable ) ) {
			lhsTableReference = getTableReferenceWhenTargetEqualsKey( lhs, tableGroup, keyColumnContainingTable );

			rhsTableKeyReference = getTableReference(
					lhs,
					tableGroup,
					targetColumnContainingTable
			);
		}
		else {
			lhsTableReference = getTableReference( lhs, tableGroup, keyColumnContainingTable );

			rhsTableKeyReference = getTableReference(
					lhs,
					tableGroup,
					targetColumnContainingTable
			);
		}

		return generateJoinPredicate(
				lhsTableReference,
				rhsTableKeyReference,
				sqlAstJoinType,
				sqlExpressionResolver,
				creationContext
		);
	}

	@Override
	public Predicate generateJoinPredicate(
			TableReference lhs,
			TableReference rhs,
			SqlAstJoinType sqlAstJoinType,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		final String rhsTableExpression = rhs.getTableExpression();
		final String lhsTableExpression = lhs.getTableExpression();
		if ( lhsTableExpression.equals( keyColumnContainingTable ) ) {
			assert rhsTableExpression.equals( targetColumnContainingTable );
			return getPredicate( lhs, rhs, creationContext, keyColumnExpressions, targetColumnExpressions );
		}
		else {
			assert rhsTableExpression.equals( keyColumnContainingTable );
			return getPredicate( lhs, rhs, creationContext, targetColumnExpressions, keyColumnExpressions );
		}
	}

	private Predicate getPredicate(
			TableReference lhs,
			TableReference rhs,
			SqlAstCreationContext creationContext,
			List<String> lhsExpressions,
			List<String> rhsColumnExpressions) {
		final Junction predicate = new Junction( Junction.Nature.CONJUNCTION );
		for ( int i = 0; i < lhsExpressions.size(); i++ ) {
			JdbcMapping jdbcMapping = jdbcMappings.get( i );
			ComparisonPredicate comparisonPredicate =
					new ComparisonPredicate(
							new ColumnReference(
									lhs,
									lhsExpressions.get( i ),
									jdbcMapping,
									creationContext.getSessionFactory()
							),
							ComparisonOperator.EQUAL,
							new ColumnReference(
									rhs,
									rhsColumnExpressions.get( i ),
									jdbcMapping,
									creationContext.getSessionFactory()
							)
					);
			predicate.add( comparisonPredicate );
		}
		return predicate;
	}

	protected TableReference getTableReferenceWhenTargetEqualsKey(TableGroup lhs, TableGroup tableGroup, String table) {
		if ( tableGroup.getPrimaryTableReference().getTableExpression().equals( table ) ) {
			return tableGroup.getPrimaryTableReference();
		}
		if ( lhs.getPrimaryTableReference().getTableExpression().equals( table ) ) {
			return lhs.getPrimaryTableReference();
		}

		for ( TableReferenceJoin tableJoin : lhs.getTableReferenceJoins() ) {
			if ( tableJoin.getJoinedTableReference().getTableExpression().equals( table ) ) {
				return tableJoin.getJoinedTableReference();
			}
		}

		throw new IllegalStateException( "Could not resolve binding for table `" + table + "`" );
	}

	protected TableReference getTableReference(TableGroup lhs, TableGroup tableGroup, String table) {
		if ( lhs.getPrimaryTableReference().getTableExpression().equals( table ) ) {
			return lhs.getPrimaryTableReference();
		}
		else if ( tableGroup.getPrimaryTableReference().getTableExpression().equals( table ) ) {
			return tableGroup.getPrimaryTableReference();
		}

		final TableReference tableReference = lhs.resolveTableReference( table );
		if ( tableReference != null ) {
			return tableReference;
		}

		throw new IllegalStateException( "Could not resolve binding for table `" + table + "`" );
	}

	@Override
	public String getReferringTableExpression() {
		return keyColumnContainingTable;
	}

	@Override
	public String getTargetTableExpression() {
		return targetColumnContainingTable;
	}

	@Override
	public void visitReferringColumns(ColumnConsumer consumer) {
		for ( int i = 0; i < keyColumnExpressions.size(); i++ ) {
			consumer.accept( keyColumnContainingTable, keyColumnExpressions.get( i ), jdbcMappings.get( i ) );
		}
	}

	@Override
	public void visitTargetColumns(ColumnConsumer consumer) {
		for ( int i = 0; i < keyColumnExpressions.size(); i++ ) {
			consumer.accept( targetColumnContainingTable, targetColumnExpressions.get( i ), jdbcMappings.get( i ) );
		}
	}

	@Override
	public boolean areTargetColumnNamesEqualsTo(String[] columnNames) {
		int length = columnNames.length;
		if ( length != targetColumnExpressions.size() ) {
			return false;
		}
		for ( int i = 0; i < length; i++ ) {
			if ( !targetColumnExpressions.contains( columnNames[i] ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public MappingType getPartMappingType() {
		throw new HibernateException( "Unexpected call to SimpleForeignKeyDescriptor#getPartMappingType" );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return mappingType.getJavaTypeDescriptor();
	}

	@Override
	public NavigableRole getNavigableRole() {
		throw new UnsupportedOperationException();
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		throw new UnsupportedOperationException();
	}

}
