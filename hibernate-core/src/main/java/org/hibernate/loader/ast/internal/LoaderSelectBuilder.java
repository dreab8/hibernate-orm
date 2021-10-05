/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.LockOptions;
import org.hibernate.collection.spi.BagSemantics;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.EffectiveEntityGraph;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.FilterHelper;
import org.hibernate.loader.ast.spi.Loadable;
import org.hibernate.loader.ast.spi.Loader;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.mapping.internal.NonAggregatedIdentifierMappingImpl;
import org.hibernate.metamodel.mapping.internal.SimpleForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.OrderByFragment;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.query.ComparisonOperator;
import org.hibernate.query.EntityIdentifierNavigablePath;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SimpleFromClauseAccessImpl;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.RootTableGroupProducer;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.results.graph.BiDirectionalFetch;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.EntityGraphTraversalState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.collection.internal.CollectionDomainResult;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.internal.StandardEntityGraphTraversalStateImpl;

import org.jboss.logging.Logger;

import static org.hibernate.query.results.ResultsHelper.attributeName;
import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * Builder for SQL AST trees used by {@link Loader} implementations.
 *
 * @author Steve Ebersole
 * @author Nahtan Xu
 */
public class LoaderSelectBuilder {
	private static final Logger log = Logger.getLogger( LoaderSelectBuilder.class );

	/**
	 * Create an SQL AST select-statement for a select by unique key based on matching one-or-more keys
	 *
	 * @param loadable The root Loadable
	 * @param partsToSelect Parts of the Loadable to select.  Null/empty indicates to select the Loadable itself
	 * @param restrictedPart Part to base the where-clause restriction on
	 * @param cachedDomainResult DomainResult to be used.  Null indicates to generate the DomainResult
	 * @param numberOfKeysToLoad How many keys should be accounted for in the where-clause restriction?
	 * @param loadQueryInfluencers Any influencers (entity graph, fetch profile) to account for
	 * @param lockOptions Pessimistic lock options to apply
	 * @param jdbcParameterConsumer Consumer for all JdbcParameter references created
	 * @param sessionFactory The SessionFactory
	 */
	public static SelectStatement createSelectByUniqueKey(
			Loadable loadable,
			List<? extends ModelPart> partsToSelect,
			ModelPart restrictedPart,
			DomainResult<?> cachedDomainResult,
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions,
			Consumer<JdbcParameter> jdbcParameterConsumer,
			SessionFactoryImplementor sessionFactory) {
		final LoaderSelectBuilder process = new LoaderSelectBuilder(
				sessionFactory,
				loadable,
				partsToSelect,
				Collections.singletonList( restrictedPart ),
				cachedDomainResult,
				numberOfKeysToLoad,
				loadQueryInfluencers,
				lockOptions,
				determineGraphTraversalState( loadQueryInfluencers ),
				true,
				jdbcParameterConsumer
		);

		return process.generateSelect();
	}

	/**
	 * Create an SQL AST select-statement based on matching one-or-more keys
	 *
	 * @param loadable The root Loadable
	 * @param partsToSelect Parts of the Loadable to select.  Null/empty indicates to select the Loadable itself
	 * @param restrictedPart Part to base the where-clause restriction on
	 * @param cachedDomainResult DomainResult to be used.  Null indicates to generate the DomainResult
	 * @param numberOfKeysToLoad How many keys should be accounted for in the where-clause restriction?
	 * @param loadQueryInfluencers Any influencers (entity graph, fetch profile) to account for
	 * @param lockOptions Pessimistic lock options to apply
	 * @param jdbcParameterConsumer Consumer for all JdbcParameter references created
	 * @param sessionFactory The SessionFactory
	 */
	public static SelectStatement createSelect(
			Loadable loadable,
			List<? extends ModelPart> partsToSelect,
			ModelPart restrictedPart,
			DomainResult<?> cachedDomainResult,
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions,
			Consumer<JdbcParameter> jdbcParameterConsumer,
			SessionFactoryImplementor sessionFactory) {
		final LoaderSelectBuilder process = new LoaderSelectBuilder(
				sessionFactory,
				loadable,
				partsToSelect,
				restrictedPart,
				cachedDomainResult,
				numberOfKeysToLoad,
				loadQueryInfluencers,
				lockOptions,
				jdbcParameterConsumer
		);

		return process.generateSelect();
	}

	public static SelectStatement createSelect(
			Loadable loadable,
			List<? extends ModelPart> partsToSelect,
			List<ModelPart> restrictedParts,
			DomainResult<?> cachedDomainResult,
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions,
			Consumer<JdbcParameter> jdbcParameterConsumer,
			SessionFactoryImplementor sessionFactory) {
		final LoaderSelectBuilder process = new LoaderSelectBuilder(
				sessionFactory,
				loadable,
				partsToSelect,
				restrictedParts,
				cachedDomainResult,
				numberOfKeysToLoad,
				loadQueryInfluencers,
				lockOptions,
				jdbcParameterConsumer
		);

		return process.generateSelect();
	}

	/**
	 * Create an SQL AST select-statement used for subselect-based CollectionLoader
	 *
	 * @param attributeMapping The plural-attribute being loaded
	 * @param subselect The subselect details to apply
	 * @param cachedDomainResult DomainResult to be used.  Null indicates to generate the DomainResult?
	 * @param loadQueryInfluencers Any influencers (entity graph, fetch profile) to account for
	 * @param lockOptions Pessimistic lock options to apply
	 * @param jdbcParameterConsumer Consumer for all JdbcParameter references created
	 * @param sessionFactory The SessionFactory
	 *
	 * @see CollectionLoaderSubSelectFetch
	 */
	public static SelectStatement createSubSelectFetchSelect(
			PluralAttributeMapping attributeMapping,
			SubselectFetch subselect,
			DomainResult cachedDomainResult,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions,
			Consumer<JdbcParameter> jdbcParameterConsumer,
			SessionFactoryImplementor sessionFactory) {
		final LoaderSelectBuilder process = new LoaderSelectBuilder(
				sessionFactory,
				attributeMapping,
				null,
				attributeMapping.getKeyDescriptor(),
				cachedDomainResult,
				-1,
				loadQueryInfluencers,
				lockOptions,
				jdbcParameterConsumer
		);

		return process.generateSelect( subselect );
	}

	private final SqlAstCreationContext creationContext;
	private final Loadable loadable;
	private final List<? extends ModelPart> partsToSelect;
	private final List<ModelPart> restrictedParts;
	private final DomainResult cachedDomainResult;
	private final int numberOfKeysToLoad;
	private final boolean forceIdentifierSelection;
	private final LoadQueryInfluencers loadQueryInfluencers;
	private final LockOptions lockOptions;
	private final Consumer<JdbcParameter> jdbcParameterConsumer;
	private final EntityGraphTraversalState entityGraphTraversalState;

	private int fetchDepth;
	private Map<OrderByFragment, TableGroup> orderByFragments;
	private boolean hasCollectionJoinFetches;
	private String currentBagRole;

	private LoaderSelectBuilder(
			SqlAstCreationContext creationContext,
			Loadable loadable,
			List<? extends ModelPart> partsToSelect,
			List<ModelPart> restrictedParts,
			DomainResult cachedDomainResult,
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions,
			EntityGraphTraversalState entityGraphTraversalState,
			boolean forceIdentifierSelection,
			Consumer<JdbcParameter> jdbcParameterConsumer) {
		this.creationContext = creationContext;
		this.loadable = loadable;
		this.partsToSelect = partsToSelect;
		this.restrictedParts = restrictedParts;
		this.cachedDomainResult = cachedDomainResult;
		this.numberOfKeysToLoad = numberOfKeysToLoad;
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.lockOptions = lockOptions;
		this.entityGraphTraversalState = entityGraphTraversalState;
		this.forceIdentifierSelection = forceIdentifierSelection;
		this.jdbcParameterConsumer = jdbcParameterConsumer;
	}

	private LoaderSelectBuilder(
			SqlAstCreationContext creationContext,
			Loadable loadable,
			List<? extends ModelPart> partsToSelect,
			List<ModelPart> restrictedParts,
			DomainResult cachedDomainResult,
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions,
			Consumer<JdbcParameter> jdbcParameterConsumer) {
		this(
				creationContext,
				loadable,
				partsToSelect,
				restrictedParts,
				cachedDomainResult,
				numberOfKeysToLoad,
				loadQueryInfluencers,
				lockOptions != null ? lockOptions : LockOptions.NONE,
				determineGraphTraversalState( loadQueryInfluencers ),
				determineWhetherToForceIdSelection( numberOfKeysToLoad, restrictedParts ),
				jdbcParameterConsumer
		);
	}

	private LoaderSelectBuilder(
			SqlAstCreationContext creationContext,
			Loadable loadable,
			List<? extends ModelPart> partsToSelect,
			ModelPart restrictedPart,
			DomainResult cachedDomainResult,
			int numberOfKeysToLoad,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions,
			Consumer<JdbcParameter> jdbcParameterConsumer) {
		this(
				creationContext,
				loadable,
				partsToSelect,
				Arrays.asList( restrictedPart ),
				cachedDomainResult,
				numberOfKeysToLoad,
				loadQueryInfluencers,
				lockOptions,
				jdbcParameterConsumer
		);
	}

	private static boolean determineWhetherToForceIdSelection(int numberOfKeysToLoad, List<ModelPart> restrictedParts) {
		if ( numberOfKeysToLoad > 1 ) {
			return true;
		}

		if ( restrictedParts.size() == 1 ) {
			final ModelPart restrictedPart = restrictedParts.get( 0 );
			if ( Objects.equals( restrictedPart.getPartName(), NaturalIdMapping.PART_NAME ) ) {
				return true;
			}
		}

		for ( ModelPart restrictedPart : restrictedParts ) {
			if ( restrictedPart instanceof ForeignKeyDescriptor || restrictedPart instanceof NonAggregatedIdentifierMappingImpl ) {
				return true;
			}
		}

		return false;
	}

	private static EntityGraphTraversalState determineGraphTraversalState(LoadQueryInfluencers loadQueryInfluencers) {
		if ( loadQueryInfluencers != null ) {
			final EffectiveEntityGraph effectiveEntityGraph = loadQueryInfluencers.getEffectiveEntityGraph();
			if ( effectiveEntityGraph != null ) {
				final GraphSemantic graphSemantic = effectiveEntityGraph.getSemantic();
				final RootGraphImplementor rootGraphImplementor = effectiveEntityGraph.getGraph();
				if ( graphSemantic != null && rootGraphImplementor != null ) {
					return new StandardEntityGraphTraversalStateImpl( graphSemantic, rootGraphImplementor );
				}
			}
		}
		return null;
	}

	private SelectStatement generateSelect() {
		if ( loadable instanceof PluralAttributeMapping ) {
			final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) loadable;
			if ( pluralAttributeMapping.getMappedType().getCollectionSemantics() instanceof BagSemantics ) {
				currentBagRole = pluralAttributeMapping.getNavigableRole().getNavigableName();
			}
		}
		final NavigablePath rootNavigablePath = new NavigablePath( loadable.getRootPathName() );

		final QuerySpec rootQuerySpec = new QuerySpec( true );
		final List<DomainResult<?>> domainResults;

		final LoaderSqlAstCreationState sqlAstCreationState = new LoaderSqlAstCreationState(
				rootQuerySpec,
				new SqlAliasBaseManager(),
				new SimpleFromClauseAccessImpl(),
				lockOptions,
				this::visitFetches,
				forceIdentifierSelection,
				creationContext
		);

		final TableGroup rootTableGroup = loadable.createRootTableGroup(
				true,
				rootNavigablePath,
				null,
				() -> rootQuerySpec::applyPredicate,
				sqlAstCreationState,
				creationContext
		);

		rootQuerySpec.getFromClause().addRoot( rootTableGroup );
		sqlAstCreationState.getFromClauseAccess().registerTableGroup( rootNavigablePath, rootTableGroup );

		if ( partsToSelect != null && !partsToSelect.isEmpty() ) {
			domainResults = new ArrayList<>( partsToSelect.size() );
			for ( ModelPart part : partsToSelect ) {
				final NavigablePath navigablePath = rootNavigablePath.append( part.getPartName() );
				final TableGroup tableGroup;
				if ( part instanceof RootTableGroupProducer ) {
					tableGroup = ( (RootTableGroupProducer) part ).createRootTableGroup(
							true,
							navigablePath,
							null,
							() -> rootQuerySpec::applyPredicate,
							sqlAstCreationState,
							creationContext
					);
					rootQuerySpec.getFromClause().addRoot( tableGroup );
					sqlAstCreationState.getFromClauseAccess().registerTableGroup( navigablePath, tableGroup );
				}
				else if ( part instanceof ToOneAttributeMapping ) {
					final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) part;
					final TableGroupJoin tableGroupJoin = toOneAttributeMapping.createTableGroupJoin(
							navigablePath,
							rootTableGroup,
							null,
							SqlAstJoinType.LEFT,
							true,
							sqlAstCreationState
					);
					tableGroup = tableGroupJoin.getJoinedGroup();
					sqlAstCreationState.getFromClauseAccess().registerTableGroup( navigablePath, tableGroup );
				}
				else {
					tableGroup = rootTableGroup;
				}
				domainResults.add(
						part.createDomainResult(
								navigablePath,
								tableGroup,
								null,
								sqlAstCreationState
						)
				);
			}
		}
		else {
			// use the one passed to the constructor or create one (maybe always create and pass?)
			//		allows re-use as they can be re-used to save on memory - they
			//		do not share state between

			//noinspection rawtypes
			final DomainResult domainResult;

			if ( this.cachedDomainResult != null ) {
				// used the one passed to the constructor
				domainResult = this.cachedDomainResult;
			}
			else {
				// create one
				domainResult = loadable.createDomainResult(
						rootNavigablePath,
						rootTableGroup,
						null,
						sqlAstCreationState
				);
			}

			//noinspection unchecked
			domainResults = Collections.singletonList( domainResult );
		}

		for ( ModelPart restrictedPart : restrictedParts ) {
			final int numberOfRestrictionColumns = restrictedPart.getJdbcTypeCount();

			applyRestriction(
					rootQuerySpec,
					rootNavigablePath,
					rootTableGroup,
					restrictedPart,
					numberOfRestrictionColumns,
					jdbcParameterConsumer,
					sqlAstCreationState
			);
		}

		if ( loadable instanceof PluralAttributeMapping ) {
			final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) loadable;
			applyFiltering( rootQuerySpec, rootTableGroup, pluralAttributeMapping );
			applyOrdering( rootTableGroup, pluralAttributeMapping );
		}
		else if ( loadable instanceof Joinable ) {
			applyFiltering( rootQuerySpec, rootTableGroup, (Joinable) loadable );
		}

		if ( orderByFragments != null ) {
			orderByFragments.forEach(
					(orderByFragment, tableGroup) -> orderByFragment.apply(
							rootQuerySpec,
							tableGroup,
							sqlAstCreationState
					)
			);
		}

		return new SelectStatement( rootQuerySpec, domainResults );
	}

	private void applyRestriction(
			QuerySpec rootQuerySpec,
			NavigablePath rootNavigablePath,
			TableGroup rootTableGroup,
			ModelPart modelPart,
			int numberColumns,
			Consumer<JdbcParameter> jdbcParameterConsumer,
			LoaderSqlAstCreationState sqlAstCreationState) {
		final SqlExpressionResolver sqlExpressionResolver = sqlAstCreationState.getSqlExpressionResolver();
		final NavigablePath navigablePath = rootNavigablePath.append( modelPart.getNavigableRole().getNavigableName() );

		if ( numberColumns == 1 ) {
			modelPart.forEachSelectable(
					(columnIndex, selection) -> {
						final TableReference tableReference = rootTableGroup.resolveTableReference(
								navigablePath, selection.getContainingTableExpression() );
						final ColumnReference columnRef =
								(ColumnReference) sqlExpressionResolver.resolveSqlExpression(
										createColumnReferenceKey( tableReference, selection.getSelectionExpression() ),
										p -> new ColumnReference(
												tableReference,
												selection,
												creationContext.getSessionFactory()
										)

								);
						if ( numberOfKeysToLoad == 1 ) {
							final JdbcParameter jdbcParameter = new JdbcParameterImpl( selection.getJdbcMapping() );
							jdbcParameterConsumer.accept( jdbcParameter );

							rootQuerySpec.applyPredicate(
									new ComparisonPredicate( columnRef, ComparisonOperator.EQUAL, jdbcParameter )
							);
						}
						else {
							final InListPredicate predicate = new InListPredicate( columnRef );
							for ( int i = 0; i < numberOfKeysToLoad; i++ ) {
								for ( int j = 0; j < numberColumns; j++ ) {
									final JdbcParameter jdbcParameter = new JdbcParameterImpl( columnRef.getJdbcMapping() );
									jdbcParameterConsumer.accept( jdbcParameter );
									predicate.addExpression( jdbcParameter );
								}
							}
							rootQuerySpec.applyPredicate( predicate );
						}
					}
			);

		}
		else {
			final List<ColumnReference> columnReferences = new ArrayList<>( numberColumns );

			modelPart.forEachSelectable(
					(columnIndex, selection) -> {
						final TableReference tableReference = rootTableGroup.resolveTableReference( navigablePath, selection.getContainingTableExpression() );
						columnReferences.add(
								(ColumnReference) sqlExpressionResolver.resolveSqlExpression(
										createColumnReferenceKey( tableReference, selection.getSelectionExpression() ),
										p -> new ColumnReference(
												tableReference,
												selection,
												creationContext.getSessionFactory()
										)
								)
						);
					}
			);

			final SqlTuple tuple = new SqlTuple( columnReferences, modelPart );
			final InListPredicate predicate = new InListPredicate( tuple );

			for ( int i = 0; i < numberOfKeysToLoad; i++ ) {
				final List<JdbcParameter> tupleParams = new ArrayList<>( numberColumns );
				for ( int j = 0; j < numberColumns; j++ ) {
					final ColumnReference columnReference = columnReferences.get( j );
					final JdbcParameter jdbcParameter = new JdbcParameterImpl( columnReference.getJdbcMapping() );
					jdbcParameterConsumer.accept( jdbcParameter );
					tupleParams.add( jdbcParameter );
				}
				final SqlTuple paramTuple = new SqlTuple( tupleParams, modelPart );
				predicate.addExpression( paramTuple );
			}

			rootQuerySpec.applyPredicate( predicate );
		}
	}

	private void applyFiltering(
			QuerySpec querySpec,
			TableGroup tableGroup,
			PluralAttributeMapping pluralAttributeMapping) {
		final CollectionPersister collectionPersister = pluralAttributeMapping.getCollectionDescriptor();
		final Joinable joinable = collectionPersister
					.getCollectionType()
					.getAssociatedJoinable( creationContext.getSessionFactory() );
		final Predicate filterPredicate = FilterHelper.createFilterPredicate(
				loadQueryInfluencers,
				joinable,
				tableGroup
		);
		if ( filterPredicate != null ) {
			querySpec.applyPredicate( filterPredicate );
		}
		if ( collectionPersister.isManyToMany() ) {
			assert joinable instanceof CollectionPersister;
			final Predicate manyToManyFilterPredicate = FilterHelper.createManyToManyFilterPredicate(
					loadQueryInfluencers,
					(CollectionPersister) joinable,
					tableGroup
			);
			if ( manyToManyFilterPredicate != null ) {
				assert tableGroup.getTableReferenceJoins().size() == 1;
				tableGroup.getTableReferenceJoins().get( 0 ).applyPredicate( manyToManyFilterPredicate );
			}
		}
	}

	private void applyFiltering(
			QuerySpec querySpec,
			TableGroup tableGroup,
			Joinable joinable) {
		final Predicate filterPredicate = FilterHelper.createFilterPredicate(
				loadQueryInfluencers,
				joinable,
				tableGroup
		);
		if ( filterPredicate != null ) {
			querySpec.applyPredicate( filterPredicate );
		}
	}

	private void applyOrdering(TableGroup tableGroup, PluralAttributeMapping pluralAttributeMapping) {
		if ( pluralAttributeMapping.getOrderByFragment() != null ) {
			applyOrdering( tableGroup, pluralAttributeMapping.getOrderByFragment() );
		}

		if ( pluralAttributeMapping.getManyToManyOrderByFragment() != null ) {
			applyOrdering( tableGroup, pluralAttributeMapping.getManyToManyOrderByFragment() );
		}
	}

	private void applyOrdering(TableGroup tableGroup, OrderByFragment orderByFragment) {
		if ( orderByFragments == null ) {
			orderByFragments = new LinkedHashMap<>();
		}
		orderByFragments.put( orderByFragment, tableGroup );
	}

	private List<Fetch> visitFetches(
			FetchParent fetchParent,
			QuerySpec querySpec,
			LoaderSqlAstCreationState creationState) {
		if ( log.isTraceEnabled() ) {
			log.tracef( "Starting visitation of FetchParent's Fetchables : %s", fetchParent.getNavigablePath() );
		}

		final List<Fetch> fetches = new ArrayList<>();
		final BiConsumer<Fetchable, Boolean> processor = createFetchableBiConsumer(
				fetchParent,
				querySpec,
				creationState,
				fetches
		);

		final FetchableContainer referencedMappingContainer = fetchParent.getReferencedMappingContainer();
		if ( fetchParent.getNavigablePath().getParent() != null ) {
			referencedMappingContainer.visitKeyFetchables(
					fetchable -> processor.accept( fetchable, true ), null );
		}
		referencedMappingContainer.visitFetchables(
				fetchable -> processor.accept( fetchable, false ), null );
		return fetches;
	}

	private BiConsumer<Fetchable, Boolean> createFetchableBiConsumer(
			FetchParent fetchParent,
			QuerySpec querySpec,
			LoaderSqlAstCreationState creationState,
			List<Fetch> fetches) {
		return (fetchable, isKeyFetchable) -> {
			final NavigablePath fetchablePath;

			if ( isKeyFetchable ) {
				final EntityIdentifierMapping identifierMapping;
				if ( fetchParent instanceof BiDirectionalFetch ) {
					final BiDirectionalFetch parentAsBiDirectionalFetch = (BiDirectionalFetch) fetchParent;
					final Fetchable biDirectionalFetchedMapping = parentAsBiDirectionalFetch.getFetchedMapping();
					if ( biDirectionalFetchedMapping instanceof EntityValuedFetchable ) {
						identifierMapping = ( (EntityValuedFetchable) biDirectionalFetchedMapping )
								.getEntityMappingType()
								.getIdentifierMapping();
					}
					else {
						identifierMapping = null;
					}
				}
				else {
					final FetchableContainer fetchableContainer = fetchParent.getReferencedMappingContainer();
					if ( fetchableContainer instanceof EntityValuedModelPart ) {
						final EntityValuedModelPart entityValuedModelPart = (EntityValuedModelPart) fetchableContainer;
						identifierMapping = entityValuedModelPart.getEntityMappingType().getIdentifierMapping();
					}
					else {
						identifierMapping = null;
					}
				}

				if ( identifierMapping != null ) {
					fetchablePath = new EntityIdentifierNavigablePath(
							fetchParent.getNavigablePath(),
							attributeName( identifierMapping )
					);
				}
				else {
					fetchablePath = fetchParent.resolveNavigablePath( fetchable );
				}
			}
			else {
				fetchablePath = fetchParent.resolveNavigablePath( fetchable );
			}

			FetchTiming fetchTiming = fetchable.getMappedFetchOptions().getTiming();
			boolean joined = fetchable.getMappedFetchOptions().getStyle() == FetchStyle.JOIN;
			boolean explicitFetch = false;
			EntityGraphTraversalState.TraversalResult traversalResult = null;

			if ( !( fetchable instanceof CollectionPart ) ) {
				// 'entity graph' takes precedence over 'fetch profile'
				if ( entityGraphTraversalState != null ) {
					traversalResult = entityGraphTraversalState.traverse( fetchParent, fetchable, isKeyFetchable );
					fetchTiming = traversalResult.getFetchTiming();
					joined = traversalResult.isJoined();
					explicitFetch = true;
				}
				else if ( loadQueryInfluencers.hasEnabledFetchProfiles() ) {
					// There is no point in checking the fetch profile if it can't affect this fetchable
					if ( fetchTiming != FetchTiming.IMMEDIATE || fetchable.incrementFetchDepth() ) {
						final String fetchableRole = fetchable.getNavigableRole().getFullPath();

						for ( String enabledFetchProfileName : loadQueryInfluencers.getEnabledFetchProfileNames() ) {
							final FetchProfile enabledFetchProfile = creationContext.getSessionFactory()
									.getFetchProfile( enabledFetchProfileName );
							final org.hibernate.engine.profile.Fetch profileFetch = enabledFetchProfile.getFetchByRole(
									fetchableRole );

							if ( profileFetch != null ) {
								fetchTiming = FetchTiming.IMMEDIATE;
								joined = joined || profileFetch.getStyle() == org.hibernate.engine.profile.Fetch.Style.JOIN;
								explicitFetch = true;
							}
						}
					}
				}
				else if ( loadQueryInfluencers.getEnabledCascadingFetchProfile() != null ) {
					final CascadeStyle cascadeStyle = ( (AttributeMapping) fetchable ).getAttributeMetadataAccess()
							.resolveAttributeMetadata( fetchable.findContainingEntityMapping() )
							.getCascadeStyle();
					final CascadingAction cascadingAction = loadQueryInfluencers.getEnabledCascadingFetchProfile()
							.getCascadingAction();
					if ( cascadeStyle == null || cascadeStyle.doCascade( cascadingAction ) ) {
						fetchTiming = FetchTiming.IMMEDIATE;
						// In 5.x the CascadeEntityJoinWalker only join fetched the first collection fetch
						joined = !hasCollectionJoinFetches;
					}
				}
			}

			final String previousBagRole = currentBagRole;
			final String bagRole;
			if ( fetchable instanceof PluralAttributeMapping
					&& ( (PluralAttributeMapping) fetchable ).getMappedType()
					.getCollectionSemantics() instanceof BagSemantics ) {
				bagRole = fetchable.getNavigableRole().getNavigableName();
			}
			else {
				bagRole = null;
			}

			if ( joined && previousBagRole != null && bagRole != null ) {
				// Avoid join fetching multiple bags
				joined = false;
			}

			boolean changeFetchDepth = !( fetchable instanceof BasicValuedModelPart )
					&& !( fetchable instanceof EmbeddedAttributeMapping )
					&& !( fetchable instanceof CollectionPart );

			try {
				if ( changeFetchDepth ) {
					fetchDepth++;
				}

				// There is no need to check for circular fetches if this is an explicit fetch
				if ( !explicitFetch && !creationState.isResolvingCircularFetch() ) {
					final Fetch biDirectionalFetch = fetchable.resolveCircularFetch(
							fetchablePath,
							fetchParent,
							fetchTiming,
							creationState
					);

					if ( biDirectionalFetch != null ) {
						fetches.add( biDirectionalFetch );
						return;
					}
				}

				final Integer maximumFetchDepth = creationContext.getMaximumFetchDepth();

				if ( maximumFetchDepth != null ) {
					if ( fetchDepth == maximumFetchDepth + 1 ) {
						joined = false;
					}
					else if ( fetchDepth > maximumFetchDepth + 1 ) {
						if ( !( fetchable instanceof BasicValuedModelPart ) && !( fetchable instanceof EmbeddedAttributeMapping ) ) {
							return;
						}
					}
				}
				if ( joined ) {
					// For join fetches we remember the currentBagRole so that we can avoid multiple bag fetches
					if ( bagRole != null ) {
						currentBagRole = bagRole;
					}
				}
				else {
					// For non-join fetches, we reset the currentBagRole and set it to the previous value in the finally block
					currentBagRole = null;
				}
				final Fetch fetch = fetchParent.generateFetchableFetch(
						fetchable,
						fetchablePath,
						fetchTiming,
						joined,
						null,
						creationState
				);

				if ( fetch.getTiming() == FetchTiming.IMMEDIATE && fetchable instanceof PluralAttributeMapping ) {
					final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) fetchable;
					if ( joined ) {
						hasCollectionJoinFetches = true;
						final TableGroup joinTableGroup = creationState.getFromClauseAccess()
								.getTableGroup( fetchablePath );
						applyFiltering(
								querySpec,
								joinTableGroup,
								pluralAttributeMapping
						);
						applyOrdering(
								querySpec,
								fetchablePath,
								pluralAttributeMapping,
								creationState
						);
					}
				}

				fetches.add( fetch );
			}
			finally {
				if ( changeFetchDepth ) {
					fetchDepth--;
				}
				// Only set the currentBagRole to the previous value for non-join fetches,
				// otherwise we could run into a multiple bag fetch situation
				if ( !joined ) {
					currentBagRole = previousBagRole;
				}
				if ( entityGraphTraversalState != null && traversalResult != null ) {
					entityGraphTraversalState.backtrack( traversalResult.getPreviousContext() );
				}
			}
		};
	}

	private void applyOrdering(
			QuerySpec ast,
			NavigablePath navigablePath,
			PluralAttributeMapping pluralAttributeMapping,
			LoaderSqlAstCreationState sqlAstCreationState) {
		assert pluralAttributeMapping.getAttributeName().equals( navigablePath.getLocalName() );

		final TableGroup tableGroup = sqlAstCreationState.getFromClauseAccess().getTableGroup( navigablePath );
		assert tableGroup != null;

		applyOrdering( tableGroup, pluralAttributeMapping );
	}

	private SelectStatement generateSelect(SubselectFetch subselect) {
		// todo (6.0) : i think we may even be able to convert this to a join by piecing together
		//		parts from the subselect-fetch sql-ast..

		// todo (6.0) : ^^ another interesting idea is to use `partsToSelect` here relative to the owner
		//		- so `loadable` is the owner entity-descriptor and the `partsToSelect` is the collection

		assert loadable instanceof PluralAttributeMapping;

		final PluralAttributeMapping attributeMapping = (PluralAttributeMapping) loadable;

		final QuerySpec rootQuerySpec = new QuerySpec( true );

		final NavigablePath rootNavigablePath = new NavigablePath( loadable.getRootPathName() );

		final LoaderSqlAstCreationState sqlAstCreationState = new LoaderSqlAstCreationState(
				rootQuerySpec,
				new SqlAliasBaseManager(),
				new SimpleFromClauseAccessImpl(),
				lockOptions,
				this::visitFetches,
				numberOfKeysToLoad > 1,
				creationContext
		);

		// todo (6.0) : I think we want to continue to assign aliases to these table-references.  we just want
		//  	to control how that gets rendered in the walker

		final TableGroup rootTableGroup = loadable.createRootTableGroup(
				true,
				rootNavigablePath,
				null,
				() -> rootQuerySpec::applyPredicate,
				sqlAstCreationState,
				creationContext
		);

		rootQuerySpec.getFromClause().addRoot( rootTableGroup );
		sqlAstCreationState.getFromClauseAccess().registerTableGroup( rootNavigablePath, rootTableGroup );

		// generate and apply the restriction
		applySubSelectRestriction(
				rootQuerySpec,
				rootNavigablePath,
				rootTableGroup,
				subselect,
				sqlAstCreationState
		);

		// NOTE : no need to check - we are explicitly processing a plural-attribute
		applyFiltering( rootQuerySpec, rootTableGroup, attributeMapping );
		applyOrdering( rootTableGroup, attributeMapping );

		// register the jdbc-parameters
		subselect.getLoadingJdbcParameters().forEach( jdbcParameterConsumer );

		return new SelectStatement(
				rootQuerySpec,
				Collections.singletonList(
						new CollectionDomainResult(
								rootNavigablePath,
								attributeMapping,
								null,
								rootTableGroup,
								sqlAstCreationState
						)
				)
		);
	}

	private void applySubSelectRestriction(
			QuerySpec querySpec,
			NavigablePath rootNavigablePath,
			TableGroup rootTableGroup,
			SubselectFetch subselect,
			LoaderSqlAstCreationState sqlAstCreationState) {
		final SqlAstCreationContext sqlAstCreationContext = sqlAstCreationState.getCreationContext();
		final SessionFactoryImplementor sessionFactory = sqlAstCreationContext.getSessionFactory();

		assert loadable instanceof PluralAttributeMapping;

		final PluralAttributeMapping attributeMapping = (PluralAttributeMapping) loadable;
		final ForeignKeyDescriptor fkDescriptor = attributeMapping.getKeyDescriptor();
		final NavigablePath navigablePath = rootNavigablePath.append( attributeMapping.getAttributeName() );

		final Expression fkExpression;

		final int jdbcTypeCount = fkDescriptor.getJdbcTypeCount();
		if ( jdbcTypeCount == 1 ) {
			assert fkDescriptor instanceof SimpleForeignKeyDescriptor;
			final SimpleForeignKeyDescriptor simpleFkDescriptor = (SimpleForeignKeyDescriptor) fkDescriptor;
			fkExpression = sqlAstCreationState.getSqlExpressionResolver().resolveSqlExpression(
					createColumnReferenceKey(
							simpleFkDescriptor.getContainingTableExpression(),
							simpleFkDescriptor.getSelectionExpression()
					),
					sqlAstProcessingState -> new ColumnReference(
							rootTableGroup.resolveTableReference( navigablePath, simpleFkDescriptor.getContainingTableExpression() ),
							simpleFkDescriptor.getSelectionExpression(),
							false,
							null,
							null,
							simpleFkDescriptor.getJdbcMapping(),
							this.creationContext.getSessionFactory()
					)
			);
		}
		else {
			final List<ColumnReference> columnReferences = new ArrayList<>( jdbcTypeCount );
			fkDescriptor.forEachSelectable(
					(columnIndex, selection) ->
							columnReferences.add(
									(ColumnReference) sqlAstCreationState.getSqlExpressionResolver()
											.resolveSqlExpression(
													createColumnReferenceKey(
															selection.getContainingTableExpression(),
															selection.getSelectionExpression()
													),
													sqlAstProcessingState -> new ColumnReference(
															rootTableGroup.resolveTableReference( navigablePath, selection.getContainingTableExpression() ),
															selection,
															this.creationContext.getSessionFactory()
													)
											)
							)
			);

			fkExpression = new SqlTuple( columnReferences, fkDescriptor );
		}

		querySpec.applyPredicate(
				new InSubQueryPredicate(
						fkExpression,
						generateSubSelect(
								attributeMapping,
								rootTableGroup,
								subselect,
								jdbcTypeCount,
								sqlAstCreationState,
								sessionFactory
						),
						false
				)
		);
	}

	private QueryPart generateSubSelect(
			PluralAttributeMapping attributeMapping,
			TableGroup rootTableGroup,
			SubselectFetch subselect,
			int jdbcTypeCount,
			LoaderSqlAstCreationState creationState,
			SessionFactoryImplementor sessionFactory) {
		final ForeignKeyDescriptor fkDescriptor = attributeMapping.getKeyDescriptor();

		final QuerySpec subQuery = new QuerySpec( false );

		final QuerySpec loadingSqlAst = subselect.getLoadingSqlAst();

		// todo (6.0) : we need to find the owner's TableGroup in the `loadingSqlAst`
		final TableGroup ownerTableGroup = subselect.getOwnerTableGroup();

		// transfer the from-clause
		loadingSqlAst.getFromClause().visitRoots( subQuery.getFromClause()::addRoot );

		final SqlExpressionResolver sqlExpressionResolver = creationState.getSqlExpressionResolver();
		final NavigablePath navigablePath = ownerTableGroup.getNavigablePath().append( attributeMapping.getAttributeName() );

		fkDescriptor.visitTargetSelectables(
				(valuesPosition, selection) -> {
					// for each column, resolve a SqlSelection and add it to the sub-query select-clause
					final TableReference tableReference = ownerTableGroup.resolveTableReference( navigablePath, selection.getContainingTableExpression() );
					final Expression expression = sqlExpressionResolver.resolveSqlExpression(
							createColumnReferenceKey( tableReference, selection.getSelectionExpression() ),
							sqlAstProcessingState -> new ColumnReference(
									tableReference,
									selection,
									sessionFactory
							)
					);
					subQuery.getSelectClause().addSqlSelection(
							new SqlSelectionImpl(
									valuesPosition + 1,
									valuesPosition,
									expression
							)
					);
				}
		);

		// transfer the restriction
		subQuery.applyPredicate( loadingSqlAst.getWhereClauseRestrictions() );

		return subQuery;
	}
}

