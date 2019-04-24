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
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.boot.model.domain.spi.EntityMappingImplementor;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.classic.Lifecycle;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.ValueInclusion;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.jdbc.Expectation;
import org.hibernate.jdbc.Expectations;
import org.hibernate.jdbc.TooManyRowsAffectedException;
import org.hibernate.loader.internal.TemplateParameterBindingContext;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.RepresentationMode;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.spi.AbstractEntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.StateArrayContributor;
import org.hibernate.metamodel.model.domain.spi.TenantDiscrimination;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.JoinedTableBinding;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmNavigableReference;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.InsertToJdbcInsertConverter;
import org.hibernate.sql.ast.consume.spi.SqlDeleteToJdbcDeleteConverter;
import org.hibernate.sql.ast.consume.spi.UpdateToJdbcUpdateConverter;
import org.hibernate.sql.ast.produce.internal.SqlAstDeleteDescriptorImpl;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.ast.tree.delete.DeleteStatement;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.LiteralParameter;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.insert.InsertStatement;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.hibernate.sql.ast.tree.update.UpdateStatement;
import org.hibernate.sql.exec.spi.DomainParameterBindingContext;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcMutation;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcUpdate;

/**
 * @author Steve Ebersole
 */
public class SingleTableEntityTypeDescriptor<T> extends AbstractEntityTypeDescriptor<T> {
	private Boolean hasCollections;
	private final boolean isJpaCacheComplianceEnabled;
	private final boolean lifecycleImplementor;


	public SingleTableEntityTypeDescriptor(
			EntityMappingImplementor bootMapping,
			IdentifiableTypeDescriptor<? super T> superTypeDescriptor,
			RuntimeModelCreationContext creationContext) throws HibernateException {
		super( bootMapping, superTypeDescriptor, creationContext );
		isJpaCacheComplianceEnabled = creationContext.getSessionFactory()
				.getSessionFactoryOptions()
				.getJpaCompliance()
				.isJpaCacheComplianceEnabled();
		if ( getRepresentationStrategy().getMode() == RepresentationMode.MAP ) {
			lifecycleImplementor = false;
		}
		else {
			lifecycleImplementor = Lifecycle.class.isAssignableFrom( bootMapping.getMappedClass() );
		}
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

	protected Object insertInternal(
			Object id,
			Object[] fields,
			Object object,
			SharedSessionContractImplementor session) {
		// generate id if needed
		if ( id == null ) {
			final IdentifierGenerator generator = getHierarchy().getIdentifierDescriptor().getIdentifierValueGenerator();
			if ( generator != null ) {
				id = generator.generate( session, object );
			}
		}

//		final Object unresolvedId = getHierarchy().getIdentifierDescriptor().unresolve( id, session );
		final Object unresolvedId = id;
		final ExecutionContext executionContext = getExecutionContext( session );

		// for now - just root table
		// for now - we also regenerate these SQL AST objects each time - we can cache these
		executeInsert( fields, session, unresolvedId, executionContext, new TableReference( getPrimaryTable(), null, false) );

		getSecondaryTableBindings().forEach(
				tableBindings -> executeJoinTableInsert(
						fields,
						session,
						unresolvedId,
						executionContext,
						tableBindings
				)
		);

		return id;
	}

	private void executeJoinTableInsert(
			Object[] fields,
			SharedSessionContractImplementor session,
			Object unresolvedId,
			ExecutionContext executionContext,
			JoinedTableBinding tableBindings) {
		if ( tableBindings.isInverse() ) {
			return;
		}

		final TableReference tableReference = new TableReference( tableBindings.getReferringTable(), null , tableBindings.isOptional());
		final ValuesNullChecker jdbcValuesToInsert = new ValuesNullChecker();
		final InsertStatement insertStatement = new InsertStatement( tableReference );

		visitStateArrayContributors(
				contributor -> {
					final int position = contributor.getStateArrayPosition();
					final Object domainValue = fields[position];
					contributor.dehydrate(
							// todo (6.0) : fix this - specifically this isInstance check is bad
							// 		sometimes the values here are unresolved and sometimes not;
							//		need a way to ensure they are always one form or the other
							//		during these calls (ideally unresolved)
							contributor.getJavaTypeDescriptor().isInstance( domainValue )
									? contributor.unresolve( domainValue, session )
									: domainValue,
							(jdbcValue, type, boundColumn) -> {
								if ( boundColumn.getSourceTable().equals( tableReference.getTable() ) ) {
									if ( jdbcValue != null ) {
										jdbcValuesToInsert.setNotAllNull();
										addInsertColumn( session, insertStatement, jdbcValue, boundColumn, type );
									}
								}
							},
							Clause.INSERT,
							session
					);
				}
		);

		if ( jdbcValuesToInsert.areAllNull() ) {
			return;
		}

		getHierarchy().getIdentifierDescriptor().dehydrate(
				// NOTE : at least according to the argument name (`unresolvedId`), the
				// 		incoming id value should already be unresolved - so do not
				// 		unresolve it again
				getHierarchy().getIdentifierDescriptor().unresolve( unresolvedId, session ),
				//unresolvedId,
				(jdbcValue, type, boundColumn) -> {
					final Column referringColumn = tableBindings.getJoinForeignKey()
							.getColumnMappings()
							.findReferringColumn( boundColumn );
					addInsertColumn(
							session,
							insertStatement,
							jdbcValue,
							referringColumn,
							boundColumn.getExpressableType()
					);
				},
				Clause.INSERT,
				session
		);

		final TenantDiscrimination tenantDiscrimination = getHierarchy().getTenantDiscrimination();
		if ( tenantDiscrimination != null ) {
			addInsertColumn(
					session,
					insertStatement,
					tenantDiscrimination.unresolve( session.getTenantIdentifier(), session ),
					tenantDiscrimination.getBoundColumn(),
					tenantDiscrimination.getBoundColumn().getExpressableType()
			);
		}

		executeInsert( executionContext, insertStatement );
	}

	private void executeInsert(
			Object[] fields,
			SharedSessionContractImplementor session,
			Object unresolvedId,
			ExecutionContext executionContext,
			TableReference tableReference) {

		final InsertStatement insertStatement = new InsertStatement( tableReference );
		// todo (6.0) : account for non-generated identifiers
		// todo (6.0) : account for post-insert generated identifiers

		final EntityIdentifier<Object, Object> identifierDescriptor = getHierarchy().getIdentifierDescriptor();
		identifierDescriptor.dehydrate(
				// NOTE : at least according to the argument name (`unresolvedId`), the
				// 		incoming id value should already be unresolved - so do not
				// 		unresolve it again
				identifierDescriptor.unresolve( unresolvedId, session ),
				//unresolvedId,
				(jdbcValue, type, boundColumn) -> {
					insertStatement.addTargetColumnReference( new ColumnReference( boundColumn ) );
					insertStatement.addValue(
							new LiteralParameter(
									jdbcValue,
									boundColumn.getExpressableType(),
									Clause.INSERT,
									session.getFactory().getTypeConfiguration()
							)
					);
				},
				Clause.INSERT,
				session
		);

		final DiscriminatorDescriptor<Object> discriminatorDescriptor = getHierarchy().getDiscriminatorDescriptor();
		if ( discriminatorDescriptor != null ) {
			addInsertColumn(
					session,
					insertStatement,
					discriminatorDescriptor.unresolve( getDiscriminatorValue(), session ),
					discriminatorDescriptor.getBoundColumn(),
					discriminatorDescriptor.getBoundColumn().getExpressableType()
			);
		}

		final TenantDiscrimination tenantDiscrimination = getHierarchy().getTenantDiscrimination();
		if ( tenantDiscrimination != null ) {
			addInsertColumn(
					session,
					insertStatement,
					tenantDiscrimination.unresolve( session.getTenantIdentifier(), session ),
					tenantDiscrimination.getBoundColumn(),
					tenantDiscrimination.getBoundColumn().getExpressableType()
			);
		}

		visitStateArrayContributors(
				contributor -> {
					final int position = contributor.getStateArrayPosition();
					final Object domainValue = fields[position];
					contributor.dehydrate(
							// todo (6.0) : fix this - specifically this isInstance check is bad
							// 		sometimes the values here are unresolved and sometimes not;
							//		need a way to ensure they are always one form or the other
							//		during these calls (ideally unresolved)
							contributor.getJavaTypeDescriptor().isInstance( domainValue )
									? contributor.unresolve( domainValue, session )
									: domainValue,
							(jdbcValue, type, boundColumn) -> {
								if ( boundColumn.getSourceTable().equals( tableReference.getTable() ) ) {
									addInsertColumn( session, insertStatement, jdbcValue, boundColumn, type );
								}
							},
							Clause.INSERT,
							session
					);
				}
		);

		executeInsert( executionContext, insertStatement );
	}

	private void executeInsert(ExecutionContext executionContext, InsertStatement insertStatement) {
		JdbcMutation jdbcInsert = InsertToJdbcInsertConverter.createJdbcInsert(
				insertStatement,
				executionContext.getSession().getSessionFactory()
		);
		executeOperation( jdbcInsert, (rows, prepareStatement) -> {}, executionContext );
	}

	private void addInsertColumn(
			SharedSessionContractImplementor session,
			InsertStatement insertStatement,
			Object jdbcValue,
			Column referringColumn,
			SqlExpressableType expressableType) {
		if ( jdbcValue != null ) {
			insertStatement.addTargetColumnReference( new ColumnReference( referringColumn ) );
			insertStatement.addValue(
					new LiteralParameter(
							jdbcValue,
							expressableType,
							Clause.INSERT,
							session.getFactory().getTypeConfiguration()
					)
			);
		}
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

		final Object unresolvedId = getHierarchy().getIdentifierDescriptor().unresolve( id, session );
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
		getHierarchy().getIdentifierDescriptor().dehydrate(
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
			getHierarchy().getIdentifierDescriptor().dehydrate(
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
		final Object unresolvedId = getHierarchy().getIdentifierDescriptor().unresolve( id, session );
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
			getHierarchy().getIdentifierDescriptor().dehydrate(
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

	private int executeOperation(
			JdbcMutation operation,
			BiConsumer<Integer, PreparedStatement> checker,
			ExecutionContext executionContext) {
		final JdbcMutationExecutor executor = JdbcMutationExecutor.WITH_AFTER_STATEMENT_CALL;
		return executor.execute(
				operation,
				JdbcParameterBindings.NO_BINDINGS,
				executionContext,
				(rows, preparestatement) -> checker.accept( rows, preparestatement )
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

	private ExecutionContext getExecutionContext(SharedSessionContractImplementor session) {
		return new ExecutionContext() {
			private final DomainParameterBindingContext parameterBindingContext = new TemplateParameterBindingContext( session.getFactory() );

			@Override
			public SharedSessionContractImplementor getSession() {
				return session;
			}

			@Override
			public QueryOptions getQueryOptions() {
				return new QueryOptionsImpl();
			}

			@Override
			public DomainParameterBindingContext getDomainParameterBindingContext() {
				return parameterBindingContext;
			}

			@Override
			public Callback getCallback() {
				return afterLoadAction -> {
				};
			}
		};
	}

	@Override
	public ValueInclusion[] getPropertyInsertGenerationInclusions() {
		throw new NotYetImplementedFor6Exception( getClass() );

	}

	@Override
	public ValueInclusion[] getPropertyUpdateGenerationInclusions() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean[] getPropertyUpdateability() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean[] getPropertyVersionability() {
		boolean[] propertyVersionability = new boolean[getStateArrayContributors().size()];
		visitStateArrayContributors(
				contributor -> {
					final int position = contributor.getStateArrayPosition();
					propertyVersionability[position] = contributor.isIncludedInOptimisticLocking();
				}
		);
		return propertyVersionability;
	}

	@Override
	public boolean[] getPropertyLaziness() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public CascadeStyle[] getPropertyCascadeStyles() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean hasCascades() {
		for ( StateArrayContributor contributor : getStateArrayContributors() ) {
			CascadeStyle cascadeStyle = contributor.getCascadeStyle();
			if ( CascadeStyles.NONE != cascadeStyle ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getIdentifierPropertyName() {
		return getHierarchy().getIdentifierDescriptor().getNavigableName();
	}

	@Override
	public boolean isCacheInvalidationRequired() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean isLazyPropertiesCacheable() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public CacheEntryStructure getCacheEntryStructure() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public CacheEntry buildCacheEntry(
			Object entity, Object[] state, Object version, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Serializable getIdByUniqueKey(
			Serializable key, String uniquePropertyName, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Object getCurrentVersion(Object id, SharedSessionContractImplementor session)
			throws HibernateException {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Object forceVersionIncrement(
			Object id, Object currentVersion, SharedSessionContractImplementor session)
			throws HibernateException {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean isInstrumented() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Boolean isTransient(Object object, SharedSessionContractImplementor session) throws HibernateException {
		final Object id = getHierarchy().getIdentifierDescriptor().extractIdentifier( object );

		// we *always* assume an instance with a null
		// identifier or no identifier property is unsaved.
		if ( id == null ) {
			return Boolean.TRUE;
		}

		// check the version unsaved-value, if appropriate
		final Object version = getVersion( object );
		if ( getHierarchy().getVersionDescriptor() != null ) {
			// let this take precedence if defined, since it works for assigned identifiers
			// todo (6.0) - this may require some more work to handle proper comparisons.
			return getHierarchy().getVersionDescriptor().getUnsavedValue() == version;
		}

		// check the id unsaved-value
		Boolean result = getHierarchy().getIdentifierDescriptor().getUnsavedValue().isUnsaved( id );
		if ( result != null ) {
			return result;
		}

		// check to see if it is in the second-level cache
		if ( session.getCacheMode().isGetEnabled() && canReadFromCache() ) {
			// todo (6.0) - support reading from the cache
			throw new NotYetImplementedFor6Exception( getClass() );
		}

		return null;
	}

	@Override
	public Object[] getPropertyValuesToInsert(
			Object object,
			Map mergeMap,
			SharedSessionContractImplementor session) throws HibernateException {
		final Object[] stateArray = new Object[ getStateArrayContributors().size() ];
		visitStateArrayContributors(
				contributor -> {
					stateArray[ contributor.getStateArrayPosition() ] = contributor.getPropertyAccess().getGetter().getForInsert(
							object,
							mergeMap,
							session
					);
				}
		);

		return stateArray;
	}

	@Override
	public void processInsertGeneratedProperties(
			Object id, Object entity, Object[] state, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void processUpdateGeneratedProperties(
			Object id, Object entity, Object[] state, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Class getMappedClass() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	public boolean implementsLifecycle() {
		return lifecycleImplementor;
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

	@Override
	public int[] resolveAttributeIndexes(String[] attributeNames) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean canUseReferenceCacheEntries() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void registerAffectingFetchProfile(String fetchProfileName) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean hasCollections() {
		// todo (6.0) : do this init up front?
		if ( hasCollections == null ) {
			hasCollections = false;
			controlledVisitAttributes(
					attr -> {
						if ( attr instanceof PluralPersistentAttribute ) {
							hasCollections = true;
							return false;
						}
						else if ( attr instanceof SingularPersistentAttributeEmbedded ) {
							( (SingularPersistentAttributeEmbedded) attr ).getEmbeddedDescriptor().controlledVisitAttributes(
									embeddedAttribute -> {
										if ( embeddedAttribute instanceof PluralPersistentAttribute ) {
											hasCollections = true;
											return false;
										}
										return true;
									}
							);
						}

						return true;
					}
			);
		}

		return hasCollections;
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

	private class ValuesNullChecker {
		private boolean allNull = true;

		private void setNotAllNull(){
			allNull = false;
		}

		public boolean areAllNull(){
			return allNull;
		}
	}
}
