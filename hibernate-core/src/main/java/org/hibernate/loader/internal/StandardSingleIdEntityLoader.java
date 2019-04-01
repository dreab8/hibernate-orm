/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.LoadQueryInfluencers.InternalFetchProfileType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.spi.SingleIdEntityLoader;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.SqlAstSelectToJdbcSelectConverter;
import org.hibernate.sql.ast.produce.internal.SqlAstQuerySpecProcessingStateImpl;
import org.hibernate.sql.ast.produce.internal.SqlAstSelectDescriptorImpl;
import org.hibernate.sql.ast.produce.metamodel.internal.LoadIdParameter;
import org.hibernate.sql.ast.produce.metamodel.internal.SelectByEntityIdentifierBuilder;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.MetamodelSelectBuilder;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.spi.FromClauseAccess;
import org.hibernate.sql.ast.produce.spi.FromClauseIndex;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.produce.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.produce.spi.SqlAstSelectDescriptor;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.produce.spi.SqlSelectionExpression;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.internal.LoadParameterBindingContext;
import org.hibernate.sql.exec.internal.RowTransformerPassThruImpl;
import org.hibernate.sql.exec.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.exec.internal.StandardJdbcParameterImpl;
import org.hibernate.sql.exec.spi.DomainParameterBindingContext;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.SqlSelection;

/**
 * @author Steve Ebersole
 */
public class StandardSingleIdEntityLoader<T> implements SingleIdEntityLoader<T> {
	private final EntityTypeDescriptor<T> entityDescriptor;

	private final SqlAstSelectDescriptor databaseSnapshotSelectAst;
	private LoadIdParameter idParameter;

	private EnumMap<LockMode,JdbcSelect> selectByLockMode = new EnumMap<>( LockMode.class );
	private EnumMap<InternalFetchProfileType,JdbcSelect> selectByInternalCascadeProfile;

	public StandardSingleIdEntityLoader(EntityTypeDescriptor<T> entityDescriptor) {
		this.entityDescriptor = entityDescriptor;

		this.databaseSnapshotSelectAst = generateDatabaseSnapshotSelect( entityDescriptor );

// todo (6.0) : re-enable this pre-caching after model processing is more fully complete
//		ParameterBindingContext context = new TemplateParameterBindingContext( entityDescriptor.getFactory(), 1 );
//		final JdbcSelect base = createJdbcSelect( LockOptions.READ, LoadQueryInfluencers.NONE, context );
//
//		selectByLockMode.put( LockMode.NONE, base );
//		selectByLockMode.put( LockMode.READ, base );
	}

	@Override
	public EntityTypeDescriptor<T> getLoadedNavigable() {
		return entityDescriptor;
	}

	@Override
	public T load(Object id, LockOptions lockOptions, SharedSessionContractImplementor session) {
		final DomainParameterBindingContext parameterBindingContext = new LoadParameterBindingContext(
				session.getFactory(),
				id
		);

		final JdbcSelect jdbcSelect = resolveJdbcSelect(
				lockOptions,
				session
		);

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl();
		entityDescriptor.getHierarchy().getIdentifierDescriptor().dehydrate(
				entityDescriptor.getHierarchy().getIdentifierDescriptor().unresolve( id, session ),
//				id,
				new ExpressableType.JdbcValueCollector() {
					private int count = 0;

					@Override
					public void collect(Object jdbcValue, SqlExpressableType type, Column boundColumn) {
						jdbcParameterBindings.addBinding(
								new StandardJdbcParameterImpl(
										count++,
										type,
										Clause.WHERE,
										session.getFactory().getTypeConfiguration()
								),
								new JdbcParameterBinding() {
									@Override
									public SqlExpressableType getBindType() {
										return type;
									}

									@Override
									public Object getBindValue() {
										return jdbcValue;
									}
								}
						);
					}
				},
				Clause.WHERE,
				session
		);

		final List<T> list = JdbcSelectExecutorStandardImpl.INSTANCE.list(
				jdbcSelect,
				jdbcParameterBindings,
				new ExecutionContext() {
					@Override
					public SharedSessionContractImplementor getSession() {
						return session;
					}

					@Override
					public QueryOptions getQueryOptions() {
						return QueryOptions.NONE;
					}

					@Override
					public DomainParameterBindingContext getDomainParameterBindingContext() {
						return parameterBindingContext;
					}

					@Override
					public Callback getCallback() {
						return null;
					}
				},
				RowTransformerSingularReturnImpl.instance()
		);

		if ( list.isEmpty() ) {
			return null;
		}

		final T entityInstance = list.get( 0 );
		return entityInstance;
	}

	private JdbcSelect resolveJdbcSelect(
			LockOptions lockOptions,
			SharedSessionContractImplementor session) {
		final LoadQueryInfluencers loadQueryInfluencers = session.getLoadQueryInfluencers();
		if ( entityDescriptor.isAffectedByEnabledFilters( session ) ) {
			// special case of not-cacheable based on enabled filters effecting this load.
			//
			// This case is special because the filters need to be applied in order to
			// 		properly restrict the SQL/JDBC results.  For this reason it has higher
			// 		precedence than even "internal" fetch profiles.
			return createJdbcSelect( lockOptions, loadQueryInfluencers, session.getSessionFactory() );
		}

		if ( loadQueryInfluencers.getEnabledInternalFetchProfileType() != null ) {
			if ( LockMode.UPGRADE.greaterThan( lockOptions.getLockMode() ) ) {
				if ( selectByInternalCascadeProfile == null ) {
					selectByInternalCascadeProfile = new EnumMap<>( InternalFetchProfileType.class );
				}
				return selectByInternalCascadeProfile.computeIfAbsent(
						loadQueryInfluencers.getEnabledInternalFetchProfileType(),
						internalFetchProfileType -> createJdbcSelect( lockOptions, loadQueryInfluencers, session.getSessionFactory() )
				);
			}
		}

		// otherwise see if the loader for the requested load can be cached - which
		// 		also means we should look in the cache for an existing one

		final boolean cacheable = determineIfCacheable( lockOptions, loadQueryInfluencers );

		if ( cacheable ) {
			return selectByLockMode.computeIfAbsent(
					lockOptions.getLockMode(),
					lockMode -> createJdbcSelect( lockOptions, loadQueryInfluencers, session.getSessionFactory() )
			);
		}

		return createJdbcSelect(
				lockOptions,
				loadQueryInfluencers,
				session.getSessionFactory()
		);

	}

	private JdbcSelect createJdbcSelect(
			LockOptions lockOptions,
			LoadQueryInfluencers queryInfluencers,
			SessionFactoryImplementor sessionFactory) {
		final MetamodelSelectBuilder selectBuilder = new SelectByEntityIdentifierBuilder(
				entityDescriptor.getFactory(),
				entityDescriptor
		);
		final SqlAstSelectDescriptor selectDescriptor = selectBuilder
				.generateSelectStatement( 1, queryInfluencers, lockOptions );


		return SqlAstSelectToJdbcSelectConverter.interpret(
				selectDescriptor,
				sessionFactory
		);
	}

	@SuppressWarnings("RedundantIfStatement")
	private boolean determineIfCacheable(LockOptions lockOptions, LoadQueryInfluencers loadQueryInfluencers) {
		if ( entityDescriptor.isAffectedByEntityGraph( loadQueryInfluencers ) ) {
			return false;
		}

		if ( lockOptions.getTimeOut() != LockOptions.WAIT_FOREVER ) {
			return false;
		}

		return true;
	}

	@Override
	public Object[] loadDatabaseSnapshot(Object id, SharedSessionContractImplementor session) {
		final JdbcSelect jdbcSelect = SqlAstSelectToJdbcSelectConverter.interpret(
				databaseSnapshotSelectAst,
				session.getSessionFactory()
		);

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl();
		entityDescriptor.getHierarchy().getIdentifierDescriptor().dehydrate(
				id,
				(jdbcValue, type, boundColumn) -> jdbcParameterBindings.addBinding(
						idParameter,
						new JdbcParameterBinding() {
							@Override
							public SqlExpressableType getBindType() {
								return type;
							}

							@Override
							public Object getBindValue() {
								return jdbcValue;
							}
						}
				),
				Clause.WHERE,
				session
		);

		final List<T> list = JdbcSelectExecutorStandardImpl.INSTANCE.list(
				jdbcSelect,
				jdbcParameterBindings,
				getExecutionContext( session ),
				RowTransformerPassThruImpl.instance()
		);

		if ( list.isEmpty() ) {
			return null;
		}

		final int size = list.size();
		final Object[] values = new Object[size];
		for ( int i = 0; i < size; i++ ) {
			values[i] = list.get( i );
		}
		return values;
	}

	private ExecutionContext getExecutionContext(SharedSessionContractImplementor session) {
		final DomainParameterBindingContext parameterBindingContext = new DomainParameterBindingContext() {
			@Override
			public <X> List<X> getLoadIdentifiers() {
				return Collections.emptyList();
			}

			@Override
			public QueryParameterBindings getQueryParameterBindings() {
				return QueryParameterBindings.NO_PARAM_BINDINGS;
			}

			@Override
			public SessionFactoryImplementor getSessionFactory() {
				return session.getSessionFactory();
			}
		};

		return new ExecutionContext() {
			@Override
			public SharedSessionContractImplementor getSession() {
				return session;
			}

			@Override
			public QueryOptions getQueryOptions() {
				return QueryOptions.NONE;
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

	private SqlAstSelectDescriptor generateDatabaseSnapshotSelect(EntityTypeDescriptor<?> entityDescriptor) {
		final QuerySpec rootQuerySpec = new QuerySpec( true );
		final SelectStatement selectStatement = new SelectStatement( rootQuerySpec );
		final SelectClause selectClause = selectStatement.getQuerySpec().getSelectClause();

		final SqlAliasBaseGenerator aliasBaseGenerator = new SqlAliasBaseManager();

		final FromClauseIndex fromClauseIndex = new FromClauseIndex();

		SqlAstCreationState creationState = new SqlAstCreationState() {
			final SqlAstQuerySpecProcessingStateImpl processingState = new SqlAstQuerySpecProcessingStateImpl(
					rootQuerySpec,
					null,
					this,
					() -> null,
					() -> expression -> {},
					() -> sqlSelection -> {}
			);

			@Override
			public SqlAstCreationContext getCreationContext() {
				return entityDescriptor.getFactory();
			}

			@Override
			public SqlExpressionResolver getSqlExpressionResolver() {
				return processingState;
			}

			@Override
			public SqlAstProcessingState getCurrentProcessingState() {
				return processingState;
			}

			@Override
			public FromClauseAccess getFromClauseAccess() {
				return fromClauseIndex;
			}

			@Override
			public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
				return aliasBaseGenerator;
			}

			@Override
			public LockMode determineLockMode(String identificationVariable) {
				return null;
			}

			@Override
			public List<Fetch> visitFetches(FetchParent fetchParent) {
				return Collections.emptyList();
			}
		};

		final NavigablePath path = new NavigablePath( entityDescriptor.getEntityName() );
		final TableGroup rootTableGroup = entityDescriptor.createRootTableGroup(
				path,
				null,
				null,
				LockMode.NONE,
				creationState
		);
//		final EntityTableGroup rootTableGroup = entityDescriptor.createRootTableGroup(
//				new TableGroupInfo() {
//					@Override
//					public String getUniqueIdentifier() {
//						return "root";
//					}
//
//					@Override
//					public String getIdentificationVariable() {
//						return null;
//					}
//
//					@Override
//					public EntityTypeDescriptor getIntrinsicSubclassEntityMetadata() {
//						return entityDescriptor;
//					}
//
//					@Override
//					public NavigablePath getNavigablePath() {
//						return path;
//					}
//				},
//				new RootTableGroupContext() {
//					@Override
//					public void addRestriction(Predicate predicate) {
//						rootQuerySpec.addRestriction( predicate );
//					}
//
//					@Override
//					public QuerySpec getQuerySpec() {
//						return rootQuerySpec;
//					}
//
//					@Override
//					public TableSpace getTableSpace() {
//						return rootTableSpace;
//					}
//
//					@Override
//					public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
//						return aliasBaseGenerator;
//					}
//
//					@Override
//					public JoinType getTableReferenceJoinType() {
//						return null;
//					}
//
//					@Override
//					public LockOptions getLockOptions() {
//						return LockOptions.NONE;
//					}
//				}
//		);

		selectStatement.getQuerySpec().getFromClause().addRoot( rootTableGroup );

		final List<DomainResult> domainResults = new ArrayList<>();

		EntityIdentifier<Object, Object> identifierDescriptor = entityDescriptor.getHierarchy()
				.getIdentifierDescriptor();

		final List<ColumnReference> columnReferences = new ArrayList();
		final Position position = new Position();
		identifierDescriptor.visitColumns(
				(sqlExpressableType, column) -> {
					final Expression expression = rootTableGroup.qualify( column );
					ColumnReference columnReference;
					if ( !ColumnReference.class.isInstance( expression ) ) {
						columnReference = (ColumnReference) ( (SqlSelectionExpression) expression ).getExpression();
					}
					else {
						columnReference = (ColumnReference) expression;
					}
					columnReferences.add( columnReference );
					SqlSelection sqlSelection = new SqlSelectionImpl(
							position.getJdbcPosition(),
							position.getValuesArrayPosition(),
							columnReference,
							sqlExpressableType.getJdbcValueExtractor()
					);
					position.increase();
					selectClause.addSqlSelection( sqlSelection );

					domainResults.add( new BasicResultImpl(
							null,
							sqlSelection,
							sqlExpressableType
					) );
				},
				Clause.SELECT,
				null
		);

		final Expression idExpression;

		if ( columnReferences.size() == 1 ) {
			idExpression = columnReferences.get( 0 );
		}
		else {
			idExpression = new SqlTuple( columnReferences );
		}

		// todo (6.0) : is this correct and/or is there a better way to add the SqlSelection to the SelectClause?
		entityDescriptor.visitStateArrayContributors(
				stateArrayContributor ->
						stateArrayContributor.visitColumns(
								(sqlExpressableType, column) -> {
									ColumnReference columnReference;
									final Expression expression = rootTableGroup.qualify( column );

									if ( !ColumnReference.class.isInstance( expression ) ) {
										columnReference = (ColumnReference) ( (SqlSelectionExpression) expression ).getExpression();
									}
									else {
										columnReference = (ColumnReference) expression;
									}

									SqlSelection sqlSelection = new SqlSelectionImpl(
											position.getJdbcPosition(),
											position.getValuesArrayPosition() ,
											columnReference,
											sqlExpressableType.getJdbcValueExtractor()
									);
									position.increase();

									selectClause.addSqlSelection( sqlSelection );
									domainResults.add( new BasicResultImpl(
											null,
											sqlSelection,
											sqlExpressableType
									) );
								},
								Clause.SELECT,
								null
						)
		);

		idParameter = new LoadIdParameter(
				identifierDescriptor,
				entityDescriptor.getFactory().getTypeConfiguration()
		);
		rootQuerySpec.addRestriction(
				new ComparisonPredicate(
						idExpression,
						ComparisonOperator.EQUAL,
						idParameter
				)
		);

		return new SqlAstSelectDescriptorImpl(
				selectStatement,
				domainResults,
				entityDescriptor.getAffectedTableNames()
		);
	}

	public class Position {
		int jdbcPosition = 1;
		int valuesArrayPosition = 0;

		public void increase() {
			jdbcPosition++;
			valuesArrayPosition++;
		}

		public int getJdbcPosition() {
			return jdbcPosition;
		}

		public int getValuesArrayPosition() {
			return valuesArrayPosition;
		}
	}
}
