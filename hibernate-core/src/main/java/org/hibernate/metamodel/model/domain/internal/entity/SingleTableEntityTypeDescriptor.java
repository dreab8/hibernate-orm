/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.entity;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.boot.model.domain.spi.EntityMappingImplementor;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.jdbc.Expectation;
import org.hibernate.jdbc.Expectations;
import org.hibernate.jdbc.TooManyRowsAffectedException;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractEntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.StateArrayContributor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.SqlDeleteToJdbcDeleteConverter;
import org.hibernate.sql.ast.consume.spi.UpdateToJdbcUpdateConverter;
import org.hibernate.sql.ast.produce.internal.SqlAstDeleteDescriptorImpl;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.LiteralParameter;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcMutation;
import org.hibernate.sql.exec.spi.JdbcUpdate;

/**
 * @author Steve Ebersole
 */
public class SingleTableEntityTypeDescriptor<T> extends AbstractEntityTypeDescriptor<T> {
	private final boolean isJpaCacheComplianceEnabled;

	public SingleTableEntityTypeDescriptor(
			EntityMappingImplementor bootMapping,
			IdentifiableTypeDescriptor<? super T> superTypeDescriptor,
			RuntimeModelCreationContext creationContext) throws HibernateException {
		super( bootMapping, superTypeDescriptor, creationContext );
		isJpaCacheComplianceEnabled = creationContext.getSessionFactory()
				.getSessionFactoryOptions()
				.getJpaCompliance()
				.isJpaCacheComplianceEnabled();
	}


	// `select ... from Person p order by p`


	@Override
	public SqmNavigableReference createSqmExpression(SqmPath lhs, SqmCreationState creationState) {
		if ( getHierarchy().getDiscriminatorDescriptor() == null ) {
			throw new UnsupportedOperationException( "Entity [" + getEntityName() + "] is not inherited" );
		}

		//noinspection unchecked
		return new SqmBasicValuedSimplePath(
				new NavigablePath( getNavigableName() + DiscriminatorDescriptor.NAVIGABLE_NAME ),
				getHierarchy().getDiscriminatorDescriptor(),
				null,
				creationState.getCreationContext().getQueryEngine().getCriteriaBuilder()
		);
	}

	@Override
	public List<ColumnReference> resolveColumnReferences(
			ColumnReferenceQualifier qualifier,
			SqlAstCreationState creationState) {
		return getIdentifierDescriptor().resolveColumnReferences( qualifier, creationState );
	}

	@Override
	public String asLoggableText() {
		return String.format( "SingleTableEntityDescriptor<%s>", getEntityName() );
	}

	@Override
	public void delete(
			Object id,
			Object version,
			Object object,
			SharedSessionContractImplementor session)
			throws HibernateException {

		// todo (6.0) - initial basic pass at entity deletes
		// todo (6.0) - take into account version

		final Object unresolvedId = getIdentifierDescriptor().unresolve( id, session );
		final ExecutionContext executionContext = getExecutionContext( session );

		delete( unresolvedId, executionContext, session );
	}

	private void delete(
			Object unresolvedId,
			ExecutionContext executionContext,
			SharedSessionContractImplementor session) {
		deleteSecondaryTables( session, unresolvedId, executionContext );

		deleteRootTable( session, unresolvedId, executionContext );
	}

	private void deleteRootTable(
			SharedSessionContractImplementor session,
			Object unresolvedId,
			ExecutionContext executionContext) {
		final TableReference tableReference = new TableReference( getPrimaryTable(), null, false );

		final Junction identifierJunction = new Junction( Junction.Nature.CONJUNCTION );
		getIdentifierDescriptor().dehydrate(
				unresolvedId,
				(jdbcValue, type, boundColumn) ->
						identifierJunction.add(
								new ComparisonPredicate(
										new ColumnReference( boundColumn ), ComparisonOperator.EQUAL,
										new LiteralParameter(
												jdbcValue,
												boundColumn.getExpressableType(),
												Clause.DELETE,
												session.getFactory().getTypeConfiguration()
										)
								)
						)
				,
				Clause.DELETE,
				session
		);

		executeDelete( executionContext, tableReference, identifierJunction );
	}

	private void deleteSecondaryTables(
			SharedSessionContractImplementor session,
			Object unresolvedId,
			ExecutionContext executionContext) {
		getSecondaryTableBindings().forEach( secondaryTable -> {
			final TableReference secondaryTableReference = new TableReference(
					secondaryTable.getReferringTable(),
					null,
					secondaryTable.isOptional()
			);
			final Junction identifierJunction = new Junction( Junction.Nature.CONJUNCTION );
			getIdentifierDescriptor().dehydrate(
					unresolvedId,
					(jdbcValue, type, boundColumn) -> {
						final Column referringColumn = secondaryTable.getJoinForeignKey()
								.getColumnMappings()
								.findReferringColumn( boundColumn );
						identifierJunction.add(
								new ComparisonPredicate(
										new ColumnReference( referringColumn ), ComparisonOperator.EQUAL,
										new LiteralParameter(
												jdbcValue,
												boundColumn.getExpressableType(),
												Clause.DELETE,
												session.getFactory().getTypeConfiguration()
										)
								)
						);
					},
					Clause.DELETE,
					session
			);

			executeDelete( executionContext, secondaryTableReference, identifierJunction );
		} );
	}

	private void executeDelete(
			ExecutionContext executionContext,
			TableReference tableReference,
			Junction identifierJunction) {
		final DeleteStatement deleteStatement = new DeleteStatement( tableReference, identifierJunction );

		final JdbcMutation delete = SqlDeleteToJdbcDeleteConverter.interpret(
				new SqlAstDeleteDescriptorImpl(
						deleteStatement,
						Collections.singleton(
								deleteStatement.getTargetTable().getTable().getTableExpression()
						)
				),
				executionContext.getSession().getSessionFactory()
		);

		executeOperation( delete, (rows, prepareStatement) -> {}, executionContext );
	}

	@Override
	public void update(
			Object id,
			Object[] fields,
			int[] dirtyFields,
			boolean hasDirtyCollection,
			Object[] oldFields,
			Object oldVersion,
			Object object,
			Object rowId,
			SharedSessionContractImplementor session) throws HibernateException {

		// todo (6.0) - initial basic pass at entity updates
		// todo (6.0) - apply any pre-update in-memory value generation
		EntityEntry entry = session.getPersistenceContext().getEntry( object );

		if ( entry == null && !getJavaTypeDescriptor().getMutabilityPlan().isMutable() ) {
			throw new IllegalStateException( "Updating immutable entity that is not in session yet!" );
		}
		final Object unresolvedId = getIdentifierDescriptor().unresolve( id, session );
		final ExecutionContext executionContext = getExecutionContext( session );

		Table primaryTable = getPrimaryTable();

		final TableReference tableReference = new TableReference( primaryTable, null, false );
		if ( isTableNeedUpdate( tableReference, dirtyFields, hasDirtyCollection, true ) ) {
			final boolean isRowToInsert = updateInternal(
					fields,
					dirtyFields,
					oldFields,
					session,
					unresolvedId,
					executionContext,
					tableReference,
					Expectations.appropriateExpectation( rootUpdateResultCheckStyle )
			);
			if ( isRowToInsert ) {
				if ( isRowToInsert ) {
					executeInsert(
							fields,
							session,
							unresolvedId,
							executionContext,
							tableReference
					);
				}
			}
		}

		getSecondaryTableBindings().forEach(
				secondaryTable -> {
					final TableReference secondaryTableReference = new TableReference(
							secondaryTable.getReferringTable(),
							null,
							secondaryTable.isOptional()
					);
					if (
							!secondaryTable.isInverse()
									&& isTableNeedUpdate(
									secondaryTableReference,
									dirtyFields,
									hasDirtyCollection,
									false
							)
					) {
						final boolean isRowToInsert = updateInternal(
								fields,
								dirtyFields,
								oldFields,
								session,
								unresolvedId,
								executionContext,
								secondaryTableReference,
								Expectations.appropriateExpectation( secondaryTable.getUpdateResultCheckStyle() )
						);

						if ( isRowToInsert ) {
							executeJoinTableInsert(
									fields,
									session,
									unresolvedId,
									executionContext,
									secondaryTable
							);
						}
					}
				}
		);
	}

	/**
	 *
	 * @return true if an insert operation is required
	 */
	private boolean updateInternal(
			Object[] fields,
			int[] dirtyFields,
			Object[] oldFields,
			SharedSessionContractImplementor session,
			Object unresolvedId,
			ExecutionContext executionContext,
			TableReference tableReference,
			Expectation expectation) {
		final boolean isRowToUpdate;
		final boolean isNullableTable = isNullableTable( tableReference );
		final boolean isFieldsAllNull = isAllNull( fields, tableReference.getTable() );
		if ( isNullableTable && oldFields != null && isAllNull( oldFields, tableReference.getTable() ) ) {
			isRowToUpdate = false;
		}
		else if ( isNullableTable && isFieldsAllNull ) {
			//if all fields are null, we might need to delete existing row
			isRowToUpdate = true;
			delete( unresolvedId, executionContext, session );
		}
		else {
			//there is probably a row there, so try to update
			//if no rows were updated, we will find out
			// TODO (6.0) : update should return a boolean value to be assigned to isRowToUpdate
			RowToUpdateChecker checker = new RowToUpdateChecker(
					unresolvedId,
					isNullableTable,
					expectation,
					getFactory(),
					this
			);
			executeUpdate(
					fields,
					oldFields,
					session,
					unresolvedId,
					executionContext,
					tableReference,
					checker
			);
			isRowToUpdate = checker.isRowToUpdate();
		}
		return !isRowToUpdate && !isFieldsAllNull;
	}

	private boolean isFieldValueChanged(Object[] fields, Object[] oldFields, int position) {
		if ( oldFields == null ) {
			return true;
		}
		final Object oldField = oldFields[position];
		final Object field = fields[position];

		if ( field == null && oldField == null ) {
			return false;
		}
		if ( field != null ) {
			return !field.equals( oldField );
		}
		return !oldField.equals( field );
	}

	private int executeUpdate(
			Object[] fields,
			Object[] oldFields,
			SharedSessionContractImplementor session,
			Object unresolvedId,
			ExecutionContext executionContext,
			TableReference tableReference,
			RowToUpdateChecker checker) {
		List<Assignment> assignments = new ArrayList<>();
		boolean anyFieldToUpdate = false;
		for ( int i = 0; i < fields.length; i++ ) {
			final StateArrayContributor contributor = getStateArrayContributors().get( i );
			final Object domainValue = fields[contributor.getStateArrayPosition()];
			List<Column> columns = contributor.getColumns();
			if ( contributor.isUpdatable() ) {
				if ( columns != null && !columns.isEmpty() && isFieldValueChanged( fields, oldFields, i ) ) {
					anyFieldToUpdate = true;
					contributor.dehydrate(
							contributor.unresolve( domainValue, session ),
							(jdbcValue, type, boundColumn) -> {
								if ( boundColumn.getSourceTable().equals( tableReference.getTable() ) ) {
									assignments.add(
											new Assignment(
													new ColumnReference( boundColumn ),
													new LiteralParameter(
															jdbcValue,
															boundColumn.getExpressableType(),
															Clause.UPDATE,
															session.getFactory().getTypeConfiguration()
													)
											)
									);
								}
							},
							Clause.UPDATE,
							session
					);
				}
			}
		}

		if ( anyFieldToUpdate ) {
			Junction identifierJunction = new Junction( Junction.Nature.CONJUNCTION );
			getIdentifierDescriptor().dehydrate(
					unresolvedId,
					(jdbcValue, type, boundColumn) ->
							identifierJunction.add(
									new ComparisonPredicate(
											new ColumnReference( boundColumn ), ComparisonOperator.EQUAL,
											new LiteralParameter(
													jdbcValue,
													boundColumn.getExpressableType(),
													Clause.WHERE,
													session.getFactory().getTypeConfiguration()
											)
									)
							)
					,
					Clause.WHERE,
					session
			);

			// todo (6.0) : depending on optimistic-lock strategy may need to adjust where clause

			final UpdateStatement updateStatement = new UpdateStatement(
					tableReference,
					assignments,
					identifierJunction
			);

			return executeUpdate( updateStatement, checker, executionContext );
		}

		return 0;
	}

	private int executeUpdate(
			UpdateStatement updateStatement,
			RowToUpdateChecker checker,
			ExecutionContext executionContext) {
		JdbcUpdate jdbcUpdate = UpdateToJdbcUpdateConverter.createJdbcUpdate(
				updateStatement,
				executionContext.getSession().getSessionFactory()
		);
		return executeOperation(
				jdbcUpdate,
				(rows, prepareStatement) -> checker.check( rows, prepareStatement ),
				executionContext
		);
	}

	protected final boolean isAllNull(Object[] fields, Table table) {
		final List<StateArrayContributor<?>> stateArrayContributors = getStateArrayContributors();
		for ( int i = 0; i < fields.length; i++ ) {
			if ( fields[i] != null ) {
				final List<Column> columns = stateArrayContributors.get( i ).getColumns();
				for ( Column column : columns ) {
					if ( column.getSourceTable().equals( table ) ) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private boolean isTableNeedUpdate(
			TableReference tableReference,
			int[] dirtyProperties,
			boolean hasDirtyCollection,
			boolean isRootTable) {
		if ( dirtyProperties == null ) {
			// TODO (6.0) : isTableNeedUpdate() to implement case dirtyProperties == null
			for ( Column column : tableReference.getTable().getColumns() ) {
				if ( column.isUpdatable() ) {
					return true;
				}
			}
			return false;
		}
		else {
			boolean tableNeedUpdate = false;
			final List<StateArrayContributor<?>> stateArrayContributors = getStateArrayContributors();
			for ( int property : dirtyProperties ) {
				final StateArrayContributor<?> contributor = stateArrayContributors.get( property );
				final List<Column> columns = contributor.getColumns();
				for ( Column column : columns ) {
					if ( column.getSourceTable().equals( tableReference.getTable() )
							&& contributor.isUpdatable() ) {
						tableNeedUpdate = true;
					}
				}
			}
			if ( isRootTable && getHierarchy().getVersionDescriptor() != null ) {
				tableNeedUpdate = tableNeedUpdate ||
						Versioning.isVersionIncrementRequired(
								dirtyProperties,
								hasDirtyCollection,
								getPropertyVersionability()
						);
			}
			return tableNeedUpdate;
		}
	}

	private boolean isNullableTable(TableReference tableReference) {
		return tableReference.isOptional() || isJpaCacheComplianceEnabled;
	}

	@Override
	public Serializable getIdByUniqueKey(
			Serializable key, String uniquePropertyName, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public EntityTypeDescriptor getSubclassEntityPersister(
			Object instance, SessionFactoryImplementor factory) {
		if ( getSubclassTypes().isEmpty() ) {
			return this;
		}
		else {
			throw new NotYetImplementedFor6Exception(  );
		}
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	private static class RowToUpdateChecker {
		private final Object id;
		private final boolean isNullableTable;
		private final Expectation expectation;
		private final SessionFactoryImplementor factory;
		private final EntityTypeDescriptor entityDescriptor;

		private boolean isRowToUpdate;

		public RowToUpdateChecker(
				Object id,
				boolean isNullableTable,
				Expectation expectation,
				SessionFactoryImplementor factory,
				EntityTypeDescriptor entityDescriptor) {
			this.id = id;
			this.isNullableTable = isNullableTable;
			this.expectation = expectation;
			this.factory = factory;
			this.entityDescriptor = entityDescriptor;
		}

		public void check(Integer rows, PreparedStatement preparedStatement) {
			try {
				expectation.verifyOutcome( rows, preparedStatement, -1 );
			}
			catch (StaleStateException e) {
				if ( isNullableTable ) {
					if ( factory.getStatistics().isStatisticsEnabled() ) {
						factory.getStatistics().optimisticFailure( entityDescriptor.getEntityName() );
					}
					throw new StaleObjectStateException( entityDescriptor.getEntityName(), id );
				}
				isRowToUpdate = false;
			}
			catch (TooManyRowsAffectedException e) {
				throw new HibernateException(
						"Duplicate identifier in table for: " +
								MessageHelper.infoString( entityDescriptor, id, factory )
				);
			}
			catch (Throwable t) {
				isRowToUpdate = false;
			}
			isRowToUpdate = true;
		}

		public boolean isRowToUpdate() {
			return isRowToUpdate;
		}
	}

}
