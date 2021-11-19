/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.mapping.List;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.CollectionIdentifierDescriptor;
import org.hibernate.metamodel.mapping.CollectionMappingType;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.metamodel.mapping.ordering.OrderByFragment;
import org.hibernate.metamodel.mapping.ordering.OrderByFragmentTranslator;
import org.hibernate.metamodel.mapping.ordering.TranslationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAliasStemHelper;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.CollectionTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.OneToManyTableGroup;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.collection.internal.CollectionDomainResult;
import org.hibernate.sql.results.graph.collection.internal.DelayedCollectionFetch;
import org.hibernate.sql.results.graph.collection.internal.EagerCollectionFetch;
import org.hibernate.sql.results.graph.collection.internal.SelectEagerCollectionFetch;
import org.hibernate.tuple.ValueGeneration;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeMappingImpl
		extends AbstractAttributeMapping
		implements PluralAttributeMapping, FetchOptions {
	private static final Logger log = Logger.getLogger( PluralAttributeMappingImpl.class );

	public interface Aware {
		void injectAttributeMapping(PluralAttributeMapping attributeMapping);
	}

	@SuppressWarnings("rawtypes")
	private final CollectionMappingType collectionMappingType;
	private final int stateArrayPosition;
	private final PropertyAccess propertyAccess;
	private final StateArrayContributorMetadataAccess stateArrayContributorMetadataAccess;
	private final String referencedPropertyName;
	private final String mapKeyPropertyName;

	private final CollectionPart elementDescriptor;
	private final CollectionPart indexDescriptor;
	private final CollectionIdentifierDescriptor identifierDescriptor;
	private final FetchTiming fetchTiming;
	private final FetchStyle fetchStyle;

	private final String bidirectionalAttributeName;

	private final CollectionPersister collectionDescriptor;
	private final String separateCollectionTable;

	private final String sqlAliasStem;

	private final IndexMetadata indexMetadata;

	private ForeignKeyDescriptor fkDescriptor;

	private OrderByFragment orderByFragment;
	private OrderByFragment manyToManyOrderByFragment;

	@SuppressWarnings({"WeakerAccess", "rawtypes"})
	public PluralAttributeMappingImpl(
			String attributeName,
			Collection bootDescriptor,
			PropertyAccess propertyAccess,
			StateArrayContributorMetadataAccess stateArrayContributorMetadataAccess,
			CollectionMappingType collectionMappingType,
			int stateArrayPosition,
			CollectionPart elementDescriptor,
			CollectionPart indexDescriptor,
			CollectionIdentifierDescriptor identifierDescriptor,
			FetchTiming fetchTiming,
			FetchStyle fetchStyle,
			CascadeStyle cascadeStyle,
			ManagedMappingType declaringType,
			CollectionPersister collectionDescriptor) {
		super( attributeName, declaringType );
		this.propertyAccess = propertyAccess;
		this.stateArrayContributorMetadataAccess = stateArrayContributorMetadataAccess;
		this.collectionMappingType = collectionMappingType;
		this.stateArrayPosition = stateArrayPosition;
		this.elementDescriptor = elementDescriptor;
		this.indexDescriptor = indexDescriptor;
		this.identifierDescriptor = identifierDescriptor;
		this.fetchTiming = fetchTiming;
		this.fetchStyle = fetchStyle;
		this.collectionDescriptor = collectionDescriptor;
		this.referencedPropertyName = bootDescriptor.getReferencedPropertyName();

		if ( bootDescriptor instanceof Map ) {
			this.mapKeyPropertyName = ( (Map) bootDescriptor ).getMapKeyPropertyName();
		}
		else {
			this.mapKeyPropertyName = null;
		}

		this.bidirectionalAttributeName = StringHelper.subStringNullIfEmpty( bootDescriptor.getMappedByProperty(), '.');

		this.sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromAttributeName( attributeName );

		if ( bootDescriptor.isOneToMany() ) {
			separateCollectionTable = null;
		}
		else {
			separateCollectionTable = ( (Joinable) collectionDescriptor ).getTableName();
		}

		final int baseIndex;
		if ( bootDescriptor instanceof List ) {
			baseIndex = ( (List) bootDescriptor ).getBaseIndex();
		}
		else {
			baseIndex = -1;
		}
		indexMetadata = new IndexMetadata() {
			@Override
			public CollectionPart getIndexDescriptor() {
				return indexDescriptor;
			}

			@Override
			public int getListIndexBase() {
				return baseIndex;
			}

			@Override
			public String getIndexPropertyName() {
				return mapKeyPropertyName;
			}
		};

		if ( collectionDescriptor instanceof Aware ) {
			( (Aware) collectionDescriptor ).injectAttributeMapping( this );
		}

		if ( elementDescriptor instanceof Aware ) {
			( (Aware) elementDescriptor ).injectAttributeMapping( this );
		}

		if ( indexDescriptor instanceof Aware ) {
			( (Aware) indexDescriptor ).injectAttributeMapping( this );
		}
	}

	@Override
	public boolean isBidirectionalAttributeName(NavigablePath fetchablePath, ToOneAttributeMapping modelPart) {
		if ( bidirectionalAttributeName == null ) {
			// If the FK-target of the to-one mapping is the same as the FK-target of this plural mapping,
			// then we say this is bidirectional, given that this is only invoked for model parts of the collection elements
			return fkDescriptor.getTargetPart() == modelPart.getForeignKeyDescriptor().getTargetPart();
		}
		return fetchablePath.getUnaliasedLocalName().endsWith( bidirectionalAttributeName );
	}

	@SuppressWarnings("unused")
	public void finishInitialization(
			Property bootProperty,
			Collection bootDescriptor,
			MappingModelCreationProcess creationProcess) {
		final boolean hasOrder = bootDescriptor.getOrderBy() != null;
		final boolean hasManyToManyOrder = bootDescriptor.getManyToManyOrdering() != null;

		if ( hasOrder || hasManyToManyOrder ) {
			final TranslationContext context = () -> collectionDescriptor.getFactory().getSessionFactoryOptions().getJpaCompliance();

			if ( hasOrder ) {
				if ( log.isDebugEnabled() ) {
					log.debugf(
							"Translating order-by fragment [%s] for collection role : %s",
							bootDescriptor.getOrderBy(),
							collectionDescriptor.getRole()
					);
				}
				orderByFragment = OrderByFragmentTranslator.translate(
						bootDescriptor.getOrderBy(),
						this,
						context
				);
			}

			if ( hasManyToManyOrder ) {
				if ( log.isDebugEnabled() ) {
					log.debugf(
							"Translating many-to-many order-by fragment [%s] for collection role : %s",
							bootDescriptor.getOrderBy(),
							collectionDescriptor.getRole()
					);
				}
				manyToManyOrderByFragment = OrderByFragmentTranslator.translate(
						bootDescriptor.getManyToManyOrdering(),
						this,
						context
				);
			}
		}
	}

	@Override
	public NavigableRole getNavigableRole() {
		return getCollectionDescriptor().getNavigableRole();
	}

	@Override
	@SuppressWarnings("rawtypes")
	public CollectionMappingType getMappedType() {
		return collectionMappingType;
	}

	@Override
	public ForeignKeyDescriptor getKeyDescriptor() {
		return fkDescriptor;
	}

	@Override
	public CollectionPersister getCollectionDescriptor() {
		return collectionDescriptor;
	}

	@Override
	public CollectionPart getElementDescriptor() {
		return elementDescriptor;
	}

	@Override
	public CollectionPart getIndexDescriptor() {
		return indexDescriptor;
	}

	@Override
	public IndexMetadata getIndexMetadata() {
		return indexMetadata;
	}

	@Override
	public CollectionIdentifierDescriptor getIdentifierDescriptor() {
		return identifierDescriptor;
	}

	@Override
	public OrderByFragment getOrderByFragment() {
		return orderByFragment;
	}

	@Override
	public OrderByFragment getManyToManyOrderByFragment() {
		return manyToManyOrderByFragment;
	}

	@Override
	public String getSeparateCollectionTable() {
		return separateCollectionTable;
	}

	@Override
	public boolean containsTableReference(String tableExpression) {
		return tableExpression.equals( separateCollectionTable );
	}

	@Override
	public int getStateArrayPosition() {
		return stateArrayPosition;
	}

	@Override
	public StateArrayContributorMetadataAccess getAttributeMetadataAccess() {
		return stateArrayContributorMetadataAccess;
	}

	@Override
	public PropertyAccess getPropertyAccess() {
		return propertyAccess;
	}

	@Override
	public ValueGeneration getValueGeneration() {
		// can never be a generated value
		return NoValueGeneration.INSTANCE;
	}

	@Override
	public String getFetchableName() {
		return getAttributeName();
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return this;
	}

	@Override
	public FetchStyle getStyle() {
		return fetchStyle;
	}

	@Override
	public FetchTiming getTiming() {
		return fetchTiming;
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		final TableGroup collectionTableGroup = creationState.getSqlAstCreationState()
				.getFromClauseAccess()
				.getTableGroup( navigablePath );

		assert collectionTableGroup != null;

		// This is only used for collection initialization where we know the owner is available, so we mark it as visited
		// which will cause bidirectional to-one associations to be treated as such and avoid a join
		creationState.registerVisitedAssociationKey( fkDescriptor.getAssociationKey(), navigablePath );

		//noinspection unchecked
		return new CollectionDomainResult( navigablePath, this, resultVariable, tableGroup, creationState );
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();

		creationState.registerVisitedAssociationKey( fkDescriptor.getAssociationKey(), fetchablePath );

		if ( fetchTiming == FetchTiming.IMMEDIATE ) {
			if ( selected ) {
				final TableGroup collectionTableGroup = resolveCollectionTableGroup(
						fetchParent,
						fetchablePath,
						creationState,
						sqlAstCreationState
				);

				return new EagerCollectionFetch(
						fetchablePath,
						this,
						collectionTableGroup,
						fetchParent,
						creationState
				);
			}
			else {
				return createSelectEagerCollectionFetch(
						fetchParent,
						fetchablePath,
						creationState,
						sqlAstCreationState
				);
			}
		}

		if ( getCollectionDescriptor().getCollectionType().hasHolder() ) {
			return createSelectEagerCollectionFetch( fetchParent, fetchablePath, creationState, sqlAstCreationState );
		}

		return createDelayedCollectionFetch( fetchParent, fetchablePath, creationState, sqlAstCreationState );
	}

	private Fetch createSelectEagerCollectionFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			DomainResultCreationState creationState, SqlAstCreationState sqlAstCreationState) {
		if ( referencedPropertyName != null ) {
			resolveCollectionTableGroup(
					fetchParent,
					fetchablePath,
					creationState,
					sqlAstCreationState
			);

			final DomainResult<?> collectionKeyDomainResult = getKeyDescriptor().createTargetDomainResult(
					fetchablePath,
					sqlAstCreationState.getFromClauseAccess().getTableGroup( fetchParent.getNavigablePath() ),
					creationState
			);

			return new SelectEagerCollectionFetch( fetchablePath, this, collectionKeyDomainResult, fetchParent );

		}
		return new SelectEagerCollectionFetch( fetchablePath, this, null, fetchParent );
	}

	private TableGroup resolveCollectionTableGroup(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			DomainResultCreationState creationState, SqlAstCreationState sqlAstCreationState) {
		final FromClauseAccess fromClauseAccess = sqlAstCreationState.getFromClauseAccess();
		return fromClauseAccess.resolveTableGroup(
				fetchablePath,
				p -> {
					final TableGroup lhsTableGroup = fromClauseAccess.getTableGroup(
							fetchParent.getNavigablePath() );
					final TableGroupJoin tableGroupJoin = createTableGroupJoin(
							fetchablePath,
							lhsTableGroup,
							null,
							SqlAstJoinType.LEFT,
							true,
							false,
							creationState.getSqlAstCreationState()
					);
					lhsTableGroup.addTableGroupJoin( tableGroupJoin );
					return tableGroupJoin.getJoinedGroup();
				}
		);
	}

	private Fetch createDelayedCollectionFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			DomainResultCreationState creationState,
			SqlAstCreationState sqlAstCreationState) {
		final DomainResult<?> collectionKeyDomainResult;
		// Lazy property. A null foreign key domain result will lead to
		// returning a domain result assembler that returns LazyPropertyInitializer.UNFETCHED_PROPERTY
		final EntityMappingType containingEntityMapping = findContainingEntityMapping();
		if ( fetchParent.getReferencedModePart() == containingEntityMapping
				&& containingEntityMapping.getEntityPersister().getPropertyLaziness()[getStateArrayPosition()] ) {
			collectionKeyDomainResult = null;
		}
		else {
			collectionKeyDomainResult = getKeyDescriptor().createTargetDomainResult(
					fetchablePath,
					sqlAstCreationState.getFromClauseAccess().getTableGroup( fetchParent.getNavigablePath() ),
					creationState
			);
		}
		return new DelayedCollectionFetch(
				fetchablePath,
				this,
				fetchParent,
				collectionKeyDomainResult
		);
	}

	@Override
	public String getSqlAliasStem() {
		return sqlAliasStem;
	}

	@Override
	public SqlAstJoinType getDefaultSqlAstJoinType(TableGroup parentTableGroup) {
		return SqlAstJoinType.LEFT;
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAstJoinType sqlAstJoinType,
			boolean fetched,
			boolean addsPredicate,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			FromClauseAccess fromClauseAccess,
			SqlAstCreationContext creationContext) {
		final java.util.List<Predicate> predicates = new ArrayList<>( 2 );
		final TableGroup tableGroup = createRootTableGroupJoin(
				navigablePath,
				lhs,
				explicitSourceAlias,
				sqlAstJoinType,
				fetched,
				predicates::add,
				aliasBaseGenerator,
				sqlExpressionResolver,
				fromClauseAccess,
				creationContext
		);
		final TableGroupJoin tableGroupJoin = new TableGroupJoin(
				navigablePath,
				sqlAstJoinType,
				tableGroup,
				null
		);
		predicates.forEach( tableGroupJoin::applyPredicate );
		return tableGroupJoin;
	}

	@Override
	public TableGroup createRootTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAstJoinType sqlAstJoinType,
			boolean fetched,
			Consumer<Predicate> predicateConsumer,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			FromClauseAccess fromClauseAccess,
			SqlAstCreationContext creationContext) {
		final CollectionPersister collectionDescriptor = getCollectionDescriptor();
		final TableGroup tableGroup;
		if ( collectionDescriptor.isOneToMany() ) {
			tableGroup = createOneToManyTableGroup(
					lhs.canUseInnerJoins() && sqlAstJoinType == SqlAstJoinType.INNER,
					navigablePath,
					fetched,
					explicitSourceAlias,
					aliasBaseGenerator.createSqlAliasBase( getSqlAliasStem() ),
					sqlExpressionResolver,
					fromClauseAccess,
					creationContext
			);
		}
		else {
			tableGroup = createCollectionTableGroup(
					lhs.canUseInnerJoins() && sqlAstJoinType == SqlAstJoinType.INNER,
					navigablePath,
					fetched,
					explicitSourceAlias,
					aliasBaseGenerator.createSqlAliasBase( getSqlAliasStem() ),
					sqlExpressionResolver,
					fromClauseAccess,
					creationContext
			);
		}

		if ( predicateConsumer != null ) {
			predicateConsumer.accept(
					getKeyDescriptor().generateJoinPredicate(
							lhs,
							tableGroup,
							sqlAstJoinType,
							sqlExpressionResolver,
							creationContext
					)
			);
		}
		return tableGroup;
	}

	public void setForeignKeyDescriptor(ForeignKeyDescriptor fkDescriptor) {
		this.fkDescriptor = fkDescriptor;
	}

	private TableGroup createOneToManyTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			boolean fetched,
			String sourceAlias,
			SqlAliasBase sqlAliasBase,
			SqlExpressionResolver sqlExpressionResolver,
			FromClauseAccess fromClauseAccess,
			SqlAstCreationContext creationContext) {
		final TableGroup elementTableGroup = ( (EntityCollectionPart) elementDescriptor ).createTableGroupInternal(
				canUseInnerJoins,
				navigablePath.append( CollectionPart.Nature.ELEMENT.getName() ),
				fetched,
				sourceAlias,
				sqlAliasBase,
				sqlExpressionResolver,
				creationContext
		);
		final OneToManyTableGroup tableGroup = new OneToManyTableGroup(
				this,
				elementTableGroup,
				creationContext.getSessionFactory()
		);

		if ( indexDescriptor instanceof TableGroupJoinProducer ) {
			final TableGroupJoin tableGroupJoin = ( (TableGroupJoinProducer) indexDescriptor ).createTableGroupJoin(
					navigablePath.append( CollectionPart.Nature.INDEX.getName() ),
					tableGroup,
					null,
					SqlAstJoinType.INNER,
					fetched,
					false,
					stem -> sqlAliasBase,
					sqlExpressionResolver,
					fromClauseAccess,
					creationContext
			);
			tableGroup.registerIndexTableGroup( tableGroupJoin );
		}

		return tableGroup;
	}

	private TableGroup createCollectionTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			boolean fetched,
			String sourceAlias,
			SqlAliasBase sqlAliasBase,
			SqlExpressionResolver sqlExpressionResolver,
			FromClauseAccess fromClauseAccess,
			SqlAstCreationContext creationContext) {
		assert !getCollectionDescriptor().isOneToMany();

		final String collectionTableName = ( (Joinable) collectionDescriptor ).getTableName();
		final TableReference collectionTableReference = new TableReference(
				collectionTableName,
				sqlAliasBase.generateNewAlias(),
				true,
				creationContext.getSessionFactory()
		);

		final CollectionTableGroup tableGroup = new CollectionTableGroup(
				canUseInnerJoins,
				navigablePath,
				this,
				fetched,
				sourceAlias,
				collectionTableReference,
				true,
				sqlAliasBase,
				s -> false,
				null,
				creationContext.getSessionFactory()
		);

		if ( elementDescriptor instanceof TableGroupJoinProducer ) {
			final TableGroupJoin tableGroupJoin = ( (TableGroupJoinProducer) elementDescriptor ).createTableGroupJoin(
					navigablePath.append( CollectionPart.Nature.ELEMENT.getName() ),
					tableGroup,
					null,
					SqlAstJoinType.INNER,
					fetched,
					false,
					stem -> sqlAliasBase,
					sqlExpressionResolver,
					fromClauseAccess,
					creationContext
			);
			tableGroup.registerElementTableGroup( tableGroupJoin );
		}

		if ( indexDescriptor instanceof TableGroupJoinProducer ) {
			final TableGroupJoin tableGroupJoin = ( (TableGroupJoinProducer) indexDescriptor ).createTableGroupJoin(
					navigablePath.append( CollectionPart.Nature.INDEX.getName() ),
					tableGroup,
					null,
					SqlAstJoinType.INNER,
					fetched,
					false,
					stem -> sqlAliasBase,
					sqlExpressionResolver,
					fromClauseAccess,
					creationContext
			);
			tableGroup.registerIndexTableGroup( tableGroupJoin );
		}

		return tableGroup;
	}

	@Override
	public TableGroup createRootTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			String explicitSourceAlias,
			Supplier<Consumer<Predicate>> additionalPredicateCollectorAccess,
			SqlAstCreationState creationState,
			SqlAstCreationContext creationContext) {
		return createRootTableGroup(
				canUseInnerJoins,
				navigablePath,
				explicitSourceAlias,
				additionalPredicateCollectorAccess,
				creationState.getSqlAliasBaseGenerator().createSqlAliasBase( getSqlAliasStem() ),
				creationState.getSqlExpressionResolver(),
				creationState.getFromClauseAccess(),
				creationContext
		);
	}

	@Override
	public TableGroup createRootTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			String explicitSourceAlias,
			Supplier<Consumer<Predicate>> additionalPredicateCollectorAccess,
			SqlAliasBase sqlAliasBase,
			SqlExpressionResolver expressionResolver,
			FromClauseAccess fromClauseAccess,
			SqlAstCreationContext creationContext) {
		if ( getCollectionDescriptor().isOneToMany() ) {
			return createOneToManyTableGroup(
					canUseInnerJoins,
					navigablePath,
					false,
					explicitSourceAlias,
					sqlAliasBase,
					expressionResolver,
					fromClauseAccess,
					creationContext
			);
		}
		else {
			return createCollectionTableGroup(
					canUseInnerJoins,
					navigablePath,
					false,
					explicitSourceAlias,
					sqlAliasBase,
					expressionResolver,
					fromClauseAccess,
					creationContext
			);
		}
	}

	@Override
	public boolean isAffectedByEnabledFilters(LoadQueryInfluencers influencers) {
		return getCollectionDescriptor().isAffectedByEnabledFilters( influencers );
	}

	@Override
	public boolean isAffectedByEntityGraph(LoadQueryInfluencers influencers) {
		return getCollectionDescriptor().isAffectedByEntityGraph( influencers );
	}

	@Override
	public boolean isAffectedByEnabledFetchProfiles(LoadQueryInfluencers influencers) {
		return getCollectionDescriptor().isAffectedByEnabledFetchProfiles( influencers );
	}

	@Override
	public String getRootPathName() {
		return getCollectionDescriptor().getRole();
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		final CollectionPart.Nature nature = CollectionPart.Nature.fromName( name );
		if ( nature == CollectionPart.Nature.ELEMENT ) {
			return elementDescriptor;
		}

		if ( nature == CollectionPart.Nature.INDEX ) {
			return indexDescriptor;
		}

		if ( nature == CollectionPart.Nature.ID ) {
			return identifierDescriptor;
		}

		if ( elementDescriptor instanceof EntityCollectionPart ) {
			return ( (EntityCollectionPart) elementDescriptor ).findSubPart( name );
		}

		if ( elementDescriptor instanceof EmbeddedCollectionPart ) {
			return ( (EmbeddedCollectionPart) elementDescriptor ).findSubPart( name, treatTargetType );
		}

		if ( elementDescriptor instanceof DiscriminatedCollectionPart ) {
			return ( (DiscriminatedCollectionPart) elementDescriptor ).findSubPart( name, treatTargetType );
		}

		return null;
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState) {
		elementDescriptor.applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		elementDescriptor.applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
	}

	@Override
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		consumer.accept( elementDescriptor );
		if ( indexDescriptor != null ) {
			consumer.accept( indexDescriptor );
		}
	}

	@Override
	public int getJdbcTypeCount() {
		int span = elementDescriptor.getJdbcTypeCount();
		if ( indexDescriptor != null ) {
			span += indexDescriptor.getJdbcTypeCount();
		}
		return span;
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		int span = elementDescriptor.forEachJdbcType( offset, action );
		if ( indexDescriptor != null ) {
			span += indexDescriptor.forEachJdbcType( offset + span, action );
		}
		return span;
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return elementDescriptor.disassemble( value,session );
	}

	@Override
	public int getNumberOfFetchables() {
		return indexDescriptor == null ? 1 : 2;
	}

	@Override
	public String toString() {
		return "PluralAttribute(" + getCollectionDescriptor().getRole() + ")";
	}
}
