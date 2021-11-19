/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.Internal;
import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.mapping.Association;
import org.hibernate.metamodel.mapping.AssociationKey;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.NonAggregatedIdentifierMappingImpl;
import org.hibernate.query.EntityIdentifierNavigablePath;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.query.results.dynamic.LegacyFetchResolver;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.ResultsLogger;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.results.ResultsHelper.attributeName;

/**
 * @author Steve Ebersole
 */
@Internal
public class DomainResultCreationStateImpl
		implements DomainResultCreationState, SqlAstCreationState, SqlAstProcessingState, SqlExpressionResolver {

	private final String stateIdentifier;
	private final FromClauseAccessImpl fromClauseAccess;

	private final JdbcValuesMetadata jdbcResultsMetadata;
	private final Consumer<SqlSelection> sqlSelectionConsumer;
	private final Map<String, SqlSelectionImpl> sqlSelectionMap = new HashMap<>();
	private boolean allowPositionalSelections = true;

	private final SqlAliasBaseManager sqlAliasBaseManager;

	private final LegacyFetchResolverImpl legacyFetchResolver;
	private final SessionFactoryImplementor sessionFactory;

	private final Stack<Function<String, FetchBuilder>> fetchBuilderResolverStack = new StandardStack<>( fetchableName -> null );
	private final Stack<NavigablePath> relativePathStack = new StandardStack<>();
	private Map<String, LockMode> registeredLockModes;
	private boolean processingKeyFetches = false;
	private boolean resolvingCircularFetch;
	private ForeignKeyDescriptor.Nature currentlyResolvingForeignKeySide;

	public DomainResultCreationStateImpl(
			String stateIdentifier,
			JdbcValuesMetadata jdbcResultsMetadata,
			Map<String, Map<String, DynamicFetchBuilderLegacy>> legacyFetchBuilders,
			Consumer<SqlSelection> sqlSelectionConsumer,
			SessionFactoryImplementor sessionFactory) {
		this.stateIdentifier = stateIdentifier;
		this.jdbcResultsMetadata = jdbcResultsMetadata;
		this.sqlSelectionConsumer = sqlSelectionConsumer;
		this.fromClauseAccess = new FromClauseAccessImpl();
		this.sqlAliasBaseManager = new SqlAliasBaseManager();

		this.legacyFetchResolver = new LegacyFetchResolverImpl( legacyFetchBuilders );

		this.sessionFactory = sessionFactory;
	}

	public LegacyFetchResolver getLegacyFetchResolver() {
		return legacyFetchResolver;
	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	public int getNumberOfProcessedSelections() {
		return sqlSelectionMap.size();
	}

	public boolean arePositionalSelectionsAllowed() {
		return allowPositionalSelections;
	}

	public void disallowPositionalSelections() {
		ResultsLogger.LOGGER.debugf( "Disallowing positional selections : %s", stateIdentifier );
		this.allowPositionalSelections = false;
	}

	public JdbcValuesMetadata getJdbcResultsMetadata() {
		return jdbcResultsMetadata;
	}

	public NavigablePath getCurrentRelativePath() {
		return relativePathStack.getCurrent();
	}

	public void pushExplicitFetchMementoResolver(Function<String, FetchBuilder> resolver) {
		fetchBuilderResolverStack.push( resolver );
	}

	public Function<String, FetchBuilder> getCurrentExplicitFetchMementoResolver() {
		return fetchBuilderResolverStack.getCurrent();
	}

	public Function<String, FetchBuilder> popExplicitFetchMementoResolver() {
		return fetchBuilderResolverStack.pop();
	}

	@SuppressWarnings( "unused" )
	public void withExplicitFetchMementoResolver(Function<String, FetchBuilder> resolver, Runnable runnable) {
		pushExplicitFetchMementoResolver( resolver );
		try {
			runnable.run();
		}
		finally {
			final Function<String, FetchBuilder> popped = popExplicitFetchMementoResolver();
			assert popped == resolver;
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainResultCreationState

	@Override
	public FromClauseAccessImpl getFromClauseAccess() {
		return fromClauseAccess;
	}

	@Override
	public DomainResultCreationStateImpl getSqlAstCreationState() {
		return this;
	}

	@Override
	public SqlAliasBaseManager getSqlAliasBaseManager() {
		return sqlAliasBaseManager;
	}

	@Override
	public boolean forceIdentifierSelection() {
		return true;
	}

	@Override
	public ModelPart resolveModelPart(NavigablePath navigablePath) {
		final TableGroup tableGroup = fromClauseAccess.findTableGroup( navigablePath );
		if ( tableGroup != null ) {
			return tableGroup.getModelPart();
		}

		if ( navigablePath.getParent() != null ) {
			final TableGroup parentTableGroup = fromClauseAccess.findTableGroup( navigablePath.getParent() );
			if ( parentTableGroup != null ) {
				return parentTableGroup.getModelPart().findSubPart( navigablePath.getLocalName(), null );
			}
		}

		return null;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlAstCreationState

	@Override
	public DomainResultCreationStateImpl getSqlExpressionResolver() {
		return getCurrentProcessingState();
	}

	@Override
	public void registerLockMode(String identificationVariable, LockMode explicitLockMode) {
		if (registeredLockModes == null ) {
			registeredLockModes = new HashMap<>();
		}
		registeredLockModes.put( identificationVariable, explicitLockMode );
	}

	public Map<String, LockMode> getRegisteredLockModes() {
		return registeredLockModes;
	}

	@Override
	public DomainResultCreationStateImpl getCurrentProcessingState() {
		return this;
	}

	public SqlAstCreationContext getCreationContext() {
		return getSessionFactory();
	}

	@Override
	public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
		return sqlAliasBaseManager;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlAstProcessingState

	@Override
	public SqlAstProcessingState getParentState() {
		return null;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlExpressionResolver

	@Override
	public Expression resolveSqlExpression(
			String key,
			Function<SqlAstProcessingState, Expression> creator) {
		final SqlSelectionImpl existing = sqlSelectionMap.get( key );
		if ( existing != null ) {
			return existing;
		}

		final Expression created = creator.apply( this );

		if ( created instanceof SqlSelectionImpl ) {
			sqlSelectionMap.put( key, (SqlSelectionImpl) created );
			sqlSelectionConsumer.accept( (SqlSelectionImpl) created );
		}
		else if ( created instanceof ColumnReference ) {
			final ColumnReference columnReference = (ColumnReference) created;
			final String columnExpression = columnReference.getColumnExpression();
			final int jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( columnExpression );
			final int valuesArrayPosition = ResultsHelper.jdbcPositionToValuesArrayPosition( jdbcPosition );

			final SqlSelectionImpl sqlSelection = new SqlSelectionImpl(
					valuesArrayPosition,
					columnReference.getJdbcMapping()
			);

			sqlSelectionMap.put( key, sqlSelection );
			sqlSelectionConsumer.accept( sqlSelection );

			return sqlSelection;
		}

		return created;
	}

	@Override
	public SqlSelection resolveSqlSelection(
			Expression expression,
			JavaType javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		if ( expression == null ) {
			throw new IllegalArgumentException( "Expression cannot be null" );
		}
		assert expression instanceof SqlSelectionImpl;
		return (SqlSelection) expression;
	}

	private static class LegacyFetchResolverImpl implements LegacyFetchResolver {
		private final Map<String,Map<String, DynamicFetchBuilderLegacy>> legacyFetchBuilders;

		public LegacyFetchResolverImpl(Map<String, Map<String, DynamicFetchBuilderLegacy>> legacyFetchBuilders) {
			this.legacyFetchBuilders = legacyFetchBuilders;
		}

		@Override
		public DynamicFetchBuilderLegacy resolve(String ownerTableAlias, String fetchedPartPath) {
			if ( legacyFetchBuilders == null ) {
				return null;
			}

			final Map<String, DynamicFetchBuilderLegacy> fetchBuilders = legacyFetchBuilders.get( ownerTableAlias );
			if ( fetchBuilders == null ) {
				return null;
			}

			return fetchBuilders.get( fetchedPartPath );
		}
	}

	@Override
	public List<Fetch> visitFetches(FetchParent fetchParent) {
		final FetchableContainer fetchableContainer = fetchParent.getReferencedMappingContainer();

		final List<Fetch> fetches = CollectionHelper.arrayList( fetchableContainer.getNumberOfFetchables() );

		final Consumer<Fetchable> fetchableConsumer = fetchable -> {
			final String fetchableName = fetchable.getFetchableName();
			final NavigablePath fetchPath = fetchParent.resolveNavigablePath( fetchable );
			final NavigablePath relativePath = relativePathStack.isEmpty()
					? new NavigablePath( fetchableName )
					: relativePathStack.getCurrent().append( fetchableName );
			// todo (6.0): figure out if we can somehow create the navigable paths in a better way
			if ( fetchable instanceof Association && fetchable.getMappedFetchOptions().getTiming() == FetchTiming.DELAYED ) {
				final Association association = (Association) fetchable;
				final ForeignKeyDescriptor foreignKeyDescriptor = association.getForeignKeyDescriptor();
				if ( foreignKeyDescriptor.getPartMappingType() instanceof EmbeddableMappingType ) {
					relativePathStack.push( relativePath.append( ( (EmbeddableMappingType) foreignKeyDescriptor.getPartMappingType() ).getPartName() ) );
				}
				else {
					relativePathStack.push( relativePath.append( foreignKeyDescriptor.getPartName() ) );
				}
			}
			else {
				relativePathStack.push( relativePath );
			}
			try {
				final FetchBuilder explicitFetchBuilder = fetchBuilderResolverStack
						.getCurrent()
						.apply( relativePath.getFullPath() );
				final FetchBuilder fetchBuilder;
				if ( explicitFetchBuilder != null ) {
					fetchBuilder = explicitFetchBuilder;
				}
				else {
					final DynamicFetchBuilderLegacy fetchBuilderLegacy = legacyFetchResolver.resolve(
							fromClauseAccess.findTableGroup( fetchParent.getNavigablePath() )
									.getPrimaryTableReference()
									.getIdentificationVariable(),
							fetchableName
					);
					if ( fetchBuilderLegacy == null ) {
						fetchBuilder = Builders.implicitFetchBuilder( fetchPath, fetchable, this );
					}
					else {
						fetchBuilder = fetchBuilderLegacy;
					}
				}
				final Fetch fetch = fetchBuilder.buildFetch(
						fetchParent,
						fetchPath,
						jdbcResultsMetadata,
						(s, s2) -> {
							throw new UnsupportedOperationException();
						},
						this
				);
				fetches.add( fetch );
			}
			finally {
				relativePathStack.pop();
			}
		};

		boolean previous = this.processingKeyFetches;
		this.processingKeyFetches = true;

		if ( fetchableContainer instanceof EntityValuedModelPart ) {
			final EntityValuedModelPart entityValuedFetchable = (EntityValuedModelPart) fetchableContainer;
			final EntityIdentifierMapping identifierMapping = entityValuedFetchable.getEntityMappingType().getIdentifierMapping();
			final boolean idClass = identifierMapping instanceof NonAggregatedIdentifierMappingImpl;
			if ( idClass ) {
				relativePathStack.push(
						new EntityIdentifierNavigablePath(
								relativePathStack.getCurrent(),
								attributeName( identifierMapping )
						)
				);
			}

			try {
				if ( identifierMapping instanceof FetchableContainer ) {
					// essentially means the entity has a composite id - ask the embeddable to visit its fetchables
					( (FetchableContainer) identifierMapping ).visitFetchables( fetchableConsumer, null );
				}
				else {
					fetchableConsumer.accept( (Fetchable) identifierMapping );
				}
			}
			finally {
				this.processingKeyFetches = previous;
				if ( idClass ) {
					this.relativePathStack.pop();
				}
			}
		}

		fetchableContainer.visitKeyFetchables( fetchableConsumer, null );
		fetchableContainer.visitFetchables( fetchableConsumer, null );
		return fetches;
	}

	@Override
	public boolean isResolvingCircularFetch() {
		return resolvingCircularFetch;
	}

	@Override
	public void setResolvingCircularFetch(boolean resolvingCircularFetch) {
		this.resolvingCircularFetch = resolvingCircularFetch;
	}

	@Override
	public ForeignKeyDescriptor.Nature getCurrentlyResolvingForeignKeyPart() {
		return currentlyResolvingForeignKeySide;
	}

	@Override
	public void setCurrentlyResolvingForeignKeyPart(ForeignKeyDescriptor.Nature currentlyResolvingForeignKeySide) {
		this.currentlyResolvingForeignKeySide = currentlyResolvingForeignKeySide;
	}

}
