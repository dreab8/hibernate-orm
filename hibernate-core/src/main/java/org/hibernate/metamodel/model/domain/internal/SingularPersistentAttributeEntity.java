/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.persistence.EntityNotFoundException;
import javax.persistence.TemporalType;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.internal.NonNullableTransientDependencies;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.internal.StandardSingleUniqueKeyEntityLoader;
import org.hibernate.loader.spi.SingleEntityLoader;
import org.hibernate.loader.spi.SingleUniqueKeyEntityLoader;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.ToOne;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.internal.entity.ToOneJoinCollectorImpl;
import org.hibernate.metamodel.model.domain.spi.AbstractNonIdSingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.metamodel.model.domain.spi.DomainModelHelper;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierComposite;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.NonIdPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.StateArrayContributor;
import org.hibernate.metamodel.model.domain.spi.TableReferenceJoinCollector;
import org.hibernate.metamodel.model.relational.internal.ColumnMappingImpl;
import org.hibernate.metamodel.model.relational.internal.ColumnMappingsImpl;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.ForeignKey.ColumnMappings;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmSingularJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.metamodel.spi.Joinable;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.produce.spi.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.internal.domain.entity.DelayedEntityFetch;
import org.hibernate.sql.results.internal.domain.entity.EntityFetchImpl;
import org.hibernate.sql.results.internal.domain.entity.ImmediatePkEntityFetch;
import org.hibernate.sql.results.internal.domain.entity.ImmediateUkEntityFetch;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.EntityFetch;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.ImmutableMutabilityPlan;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.metamodel.model.domain.spi.CollectionElement.NAVIGABLE_NAME;
import static org.hibernate.metamodel.model.domain.spi.EntityIdentifier.NAVIGABLE_ID;
import static org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute.SingularAttributeClassification.MANY_TO_ONE;
import static org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute.SingularAttributeClassification.ONE_TO_ONE;


/**
 * @author Steve Ebersole
 */
public class SingularPersistentAttributeEntity<O, J>
		extends AbstractNonIdSingularPersistentAttribute<O, J>
		implements EntityValuedNavigable<J>,
		Joinable<O,J>,
		Fetchable<J>,
		AllowableParameterType<J>,
		TableGroupJoinProducer {

	private final SingularAttributeClassification classification;
	private final PersistentAttributeType persistentAttributeType;
	private final boolean isLogicalOneToOne;
	private final boolean constrained;
	private final NavigableRole navigableRole;
	private final String sqlAliasStem;
	private final EntityTypeDescriptor<J> entityDescriptor;
	private final String referencedUkAttributeName;
	private final FetchStrategy fetchStrategy;

	private final NotFoundAction notFoundAction;
	private final CascadeStyle cascadeStyle;

	private StateArrayContributor referencedUkAttribute;
	private SingleEntityLoader singleEntityLoader;
	private ForeignKey foreignKey;
	private String mappedBy;

	public SingularPersistentAttributeEntity(
			ManagedTypeDescriptor<O> runtimeModelContainer,
			PersistentAttributeMapping bootModelAttribute,
			PropertyAccess propertyAccess,
			Disposition disposition,
			SingularAttributeClassification classification,
			RuntimeModelCreationContext context) {
		super( runtimeModelContainer, bootModelAttribute, propertyAccess, disposition );
		this.classification = classification;
		this.navigableRole = runtimeModelContainer.getNavigableRole().append( bootModelAttribute.getName() );
		this.mappedBy = bootModelAttribute.getMappedBy();

		final ToOne valueMapping = (ToOne) bootModelAttribute.getValueMapping();
		referencedUkAttributeName = valueMapping.getReferencedPropertyName();

		if ( valueMapping.getReferencedEntityName() == null ) {
			throw new MappingException(
					"Name of target entity of a to-one association not known : " + navigableRole.getFullPath()
			);
		}

		this.entityDescriptor = context.getInFlightRuntimeModel()
				.findEntityDescriptor( valueMapping.getReferencedEntityName() );
		if ( entityDescriptor == null ) {
			throw new MappingException(
					String.format(
							Locale.ROOT,
							"Cannot create SingularPersistentAttributeEntity [%s] : could not locate target entity descriptor [%s]",
							navigableRole.getFullPath(),
							valueMapping.getReferencedEntityName()
					)
			);
		}

		// todo (6.0) : we need to delay resolving this.
		//		this is essentially a "second pass".  for now we assume it
		// 		points to the target entity's PK
//		assert valueMapping.isReferenceToPrimaryKey();

		// taken from ToOneFkSecondPass
		if ( valueMapping.getForeignKey() != null ) {
			this.foreignKey = context.getDatabaseObjectResolver().resolveForeignKey( valueMapping.getForeignKey() );
		}

		if ( MANY_TO_ONE.equals( classification ) ) {
			final ManyToOne manyToOne = (ManyToOne) bootModelAttribute.getValueMapping();
			isLogicalOneToOne = manyToOne.isLogicalOneToOne();
			persistentAttributeType = isLogicalOneToOne
					? PersistentAttributeType.ONE_TO_ONE
					: PersistentAttributeType.MANY_TO_ONE;
			constrained = true;
			notFoundAction = manyToOne.isIgnoreNotFound()
					? NotFoundAction.IGNORE
					: NotFoundAction.EXCEPTION;
		}
		else {
			isLogicalOneToOne = false;
			persistentAttributeType = PersistentAttributeType.ONE_TO_ONE;
			constrained = ( (OneToOne) valueMapping ).isConstrained();
			notFoundAction = NotFoundAction.EXCEPTION;
		}

		this.sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromAttributeName( bootModelAttribute.getName() );

		context.registerNavigable( this, bootModelAttribute );

		instantiationComplete( bootModelAttribute, context );

		this.cascadeStyle = CascadeStyles.getCascadeStyle(bootModelAttribute.getCascade());
		this.fetchStrategy = DomainModelHelper.determineFetchStrategy(
				bootModelAttribute,
				runtimeModelContainer,
				entityDescriptor
		);
	}

	@Override
	public CascadeStyle getCascadeStyle() {
		return this.cascadeStyle;
	}

	@Override
	public boolean isCircular(FetchParent fetchParent) {
		final NavigableContainer parentNavigableContainer = fetchParent.getNavigableContainer();

		if ( parentNavigableContainer != null ) {
			if ( parentNavigableContainer instanceof EntityIdentifierComposite ) {
				/*
					if we have the following mapping:

					@Entity
					public class Card{
						...

						@OneToMany( mappedBy = "primaryKey.card")
						private Set<CardField> field
					}

					@Entity
					public class CardField implements Serializable {
						...

						@EmbeddedId
						private PrimaryKey primaryKey;
					}

					@Embeddable
					public class PrimaryKey implements Serializable {
						@ManyToOne(optional = false)
						private Card card;

						@ManyToOne(optional = false)
						private Key key;
					}

				 retrieving an instance of a Card will produce a fetchable : "Card.CardField.{element}.{id}.card",
				 in such a case fetchParent.getNavigablePath() = Card.CardField.{element}.{id}
				  */

				NavigablePath parent = fetchParent.getNavigablePath();
				parent = parent.getParent();
				// now parent = Card.CardField.{element}
				if ( parent.getFullPath().endsWith( NAVIGABLE_NAME ) ) {
					parent = parent.getParent();
					// now parent is Card.CardField
				}
				NavigablePath parentParent = parent.getParent();
				if ( parentParent != null && parentParent.getFullPath()
						.equals( getEntityDescriptor().getNavigableName() ) ) {
					return true;
				}
			}
			else {
				NavigableRole parentParentNavigableRole = parentNavigableContainer.getNavigableRole().getParent();
				if ( parentParentNavigableRole != null &&
						parentParentNavigableRole.getNavigableName()
								.equals( getEntityDescriptor().getNavigableName() ) ) {
				/*
				if we have the following mapping
				@Entity
				public class Parent {
					...
					@OneToOne(mappedBy = "parent")
					private Child child;
					@OneToOne(mappedBy = "parent")
					private Child2 child2;
					...
				}
				@Entity
				public static class Child {
					...
					@OneToOne
					private Parent parent;
					...
				}
				@Entity
				public static class Child2 {
					...
					@OneToOne
					private Parent parent;
					...
				}
				when we do a Session.get(Child.class,...);
				we have to distinguish between:
				- Child.parent.child,
					where mappedBy == null && getNavigableName().equals( parentMappedBy ) and isCircular = true
				- Child.parent.child2,
					where as in the previous case mappedBy == null && getNavigableName().equals( parentMappedBy ) but isCircular is false

				checking parentParentNavigableRole.getNavigableName().equals( getEntityDescriptor().getNavigableName() ) ) helps to distinguish the 2 situations because for the first case it is true while in the second case it is false.
				 */
					if ( mappedBy != null && mappedBy.equals( fetchParent.getNavigablePath().getLocalName() ) ) {
						return true;
					}
					String parentMappedBy = ( (EntityFetch) fetchParent ).getEntityValuedNavigable().getMappedBy();
					if ( mappedBy == null && getNavigableName().equals( parentMappedBy ) ) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public String getMappedBy() {
		return mappedBy;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean finishInitialization(
			Object bootReference,
			RuntimeModelCreationContext creationContext) {
		if ( referencedUkAttributeName == null ) {
			singleEntityLoader = getAssociatedEntityDescriptor().getSingleIdLoader();
		}
		else {
			this.referencedUkAttribute = ( (SingularPersistentAttributeEntity) getContainer().findDeclaredPersistentAttribute(
					getName() ) ).getEntityDescriptor().findPersistentAttribute( referencedUkAttributeName );
			singleEntityLoader = new StandardSingleUniqueKeyEntityLoader(
					this.referencedUkAttribute,
					this
			);
		}

		if ( this.foreignKey == null ) {
			SingularPersistentAttributeEntity foreignKeyOwningAttribute = (SingularPersistentAttributeEntity)
					entityDescriptor.findNavigable( referencedUkAttributeName );

			if ( foreignKeyOwningAttribute != null && foreignKeyOwningAttribute.getForeignKey() != null ) {
				final ForeignKey foreignKeyOwning = foreignKeyOwningAttribute.getForeignKey();

				List<ColumnMappings.ColumnMapping> columns = new ArrayList<>();
				for ( ColumnMappings.ColumnMapping columnMapping : foreignKeyOwning.getColumnMappings()
						.getColumnMappings() ) {
					columns.add( new ColumnMappingImpl(
							columnMapping.getTargetColumn(),
							columnMapping.getReferringColumn()
					) );
				}

				this.foreignKey = new ForeignKey(
						foreignKeyOwning.getName(),
						false,
						foreignKeyOwning.getKeyDefinition(),
						false,
						false,
						foreignKeyOwning.getTargetTable(),
						foreignKeyOwning.getReferringTable(),
						new ColumnMappingsImpl(
								foreignKeyOwning.getTargetTable(),
								foreignKeyOwning.getReferringTable(),
								columns
						)
				);

				return true;
			}

			return false;
		}

		return true;
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return persistentAttributeType;
	}

	@Override
	public EntityTypeDescriptor<J> getEntityDescriptor() {
		return entityDescriptor;
	}

	@Override
	public EntityTypeDescriptor<J> getType() {
		return getEntityDescriptor();
	}

	@Override
	public String getJpaEntityName() {
		return entityDescriptor.getJpaEntityName();
	}

	@Override
	public EntityJavaDescriptor<J> getJavaTypeDescriptor() {
		return entityDescriptor.getJavaTypeDescriptor();
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return entityDescriptor.findNavigable( navigableName );
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		entityDescriptor.visitNavigables( visitor );
	}

	@Override
	public boolean isAssociation() {
		return true;
	}

	public EntityTypeDescriptor getAssociatedEntityDescriptor() {
		return entityDescriptor;
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return classification;
	}

	@Override
	public String asLoggableText() {
		return "SingularAttributeEntity([" + getAttributeTypeClassification().name() + "] " +
				getContainer().asLoggableText() + '.' + getAttributeName() +
				")";
	}

	@Override
	public String toString() {
		return asLoggableText();
	}

	public String getEntityName() {
		return entityDescriptor.getEntityName();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitSingularAttributeEntity( this );
	}

	@Override
	public void visitFetchables(Consumer<Fetchable> fetchableConsumer) {
		// todo (6.0) : ultimately all attributes/StateArrayContributors will need to be Fetchable - including basic-typed ones
		getEntityDescriptor().visitFetchables( fetchableConsumer );
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		return fetchStrategy;
	}

	@Override
	public ForeignKeyDirection getForeignKeyDirection() {
		/// todo (6.0) : calculate this and cache as state

		switch ( getAttributeTypeClassification() ) {
			case MANY_TO_ONE: {
				return isLogicalOneToOne
						? ForeignKeyDirection.FROM_PARENT
						: ForeignKeyDirection.TO_PARENT;
			}
			case ONE_TO_ONE: {
				return mappedBy != null
						? ForeignKeyDirection.TO_PARENT
						: ForeignKeyDirection.FROM_PARENT;

			}
			case ANY: {
				return ForeignKeyDirection.FROM_PARENT;
			}
		}

		throw new HibernateException(
				"Unexpected classification [" + getAttributeTypeClassification() +
						"] found in entity-valued attribute descriptor"
		);
	}

	@Override
	public SqmAttributeJoin createSqmJoin(
			SqmFrom lhs,
			SqmJoinType joinType,
			String alias,
			boolean fetched,
			SqmCreationState creationState) {
		return new SqmSingularJoin(
				lhs,
				this,
				alias,
				joinType,
				fetched,
				creationState.getCreationContext().getNodeBuilder()
		);
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {

		// types of entity fetches:
		//		1) joined
		//		2) lazy
		//		3) selected

		if ( fetchTiming == FetchTiming.DELAYED ) {
			// todo (6.0) : need general laziness metadata - currently only done for entity
			final boolean isContainerEnhancedForLazy = getContainer() instanceof EntityTypeDescriptor<?>
					&& ( (EntityTypeDescriptor) getContainer() ).getBytecodeEnhancementMetadata()
					.isEnhancedForLazyLoading();

			final boolean cannotBeLazy = ( getAttributeTypeClassification() == ONE_TO_ONE && isOptional() ) || isContainerEnhancedForLazy;

			if ( cannotBeLazy ) {
				return generateImmediateFetch( fetchParent, creationState );
			}
			else {
				return generateDelayedFetch( fetchParent, creationState );
			}
		}
		else {
			if ( selected ) {
				return generateJoinFetch(
						fetchParent,
						lockMode,
						resultVariable,
						creationState
				);
			}
			else {
				return generateImmediateFetch( fetchParent, creationState );
			}
		}
	}

	private Fetch generateDelayedFetch(
			FetchParent fetchParent,
			DomainResultCreationState creationState) {
		return new DelayedEntityFetch(
				fetchParent,
				this,
				createKeyDomainResult( fetchParent, creationState )
		);
	}

	private ForeignKeyDomainResult createKeyDomainResult(FetchParent fetchParent, DomainResultCreationState creationState) {
		// make sure columns for the FK, if one, are added to the SQL AST as selections

		final TableGroup tableGroup = creationState.getFromClauseAccess().getTableGroup( fetchParent.getNavigablePath() );
		final SqlExpressionResolver sqlExpressionResolver = creationState.getSqlExpressionResolver();
		final List<SqlSelection> sqlSelections = new ArrayList<>();

		List<Column> columns = getColumns();
		if ( getColumns() == null || getColumns().isEmpty() ) {
			columns = getAssociatedEntityDescriptor().getIdentifierDescriptor().getColumns();
		}

		for ( Column column : columns ) {
			sqlSelections.add(
					sqlExpressionResolver.resolveSqlSelection(
							sqlExpressionResolver.resolveSqlExpression( tableGroup, column ),
							column.getJavaTypeDescriptor(),
							creationState.getSqlAstCreationState().getCreationContext().getDomainModel().getTypeConfiguration()
					)
			);
		}

		return new ForeignKeyDomainResult( null, sqlSelections );
	}

	private Fetch generateImmediateFetch(
			FetchParent fetchParent,
			DomainResultCreationState creationState) {
		if ( referencedUkAttributeName == null
				|| referencedUkAttributeName.equals( NAVIGABLE_ID )
				|| referencedUkAttributeName.equals( EntityIdentifier.LEGACY_NAVIGABLE_ID ) ) {
			return new ImmediatePkEntityFetch(
					fetchParent,
					this,
					singleEntityLoader,
					createKeyDomainResult( fetchParent, creationState ),
					notFoundAction
			);
		}
		else {
			final Navigable ukTargetNavigable;
			if ( referencedUkAttribute == null ) {
				ukTargetNavigable = getAssociatedEntityDescriptor().getIdentifierDescriptor();
			}
			else {
				ukTargetNavigable = referencedUkAttribute;
			}
			return new ImmediateUkEntityFetch(
					fetchParent,
					this,
					singleEntityLoader,
					createKeyDomainResult( fetchParent, creationState ),
					(key, sessionContractImplementor) -> new EntityUniqueKey(
							getAssociatedEntityDescriptor().getEntityName(),
							referencedUkAttributeName,
							key,
							getAssociatedEntityDescriptor().getIdentifierDescriptor().getJavaTypeDescriptor(),
							ukTargetNavigable.getJavaTypeDescriptor(),
							getAssociatedEntityDescriptor().getHierarchy().getRepresentation()
					),
					notFoundAction
			);
		}
	}

	private Fetch generateJoinFetch(
			FetchParent fetchParent,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {

		final String navigableName = getNavigableName();
		final NavigablePath navigablePath = fetchParent.getNavigablePath().append( navigableName );

		TableGroup tableGroup = creationState.getFromClauseAccess().getTableGroup( fetchParent.getNavigablePath() );

		final TableGroupJoin tableGroupJoin = createTableGroupJoin(
				navigablePath,
				tableGroup,
				resultVariable,
				isNullable() ? JoinType.LEFT : JoinType.INNER,
				lockMode,
				creationState.getSqlAstCreationState()
		);

		tableGroup.addTableGroupJoin( tableGroupJoin );

		creationState.getFromClauseAccess().registerTableGroup( navigablePath, tableGroupJoin.getJoinedGroup() );

		return new EntityFetchImpl(
				fetchParent,
				this,
				lockMode,
				navigablePath,
				creationState
		);
	}

	@Override
	public String getSqlAliasStem() {
		return sqlAliasStem;
	}

	@Override
	public void applyTableReferenceJoins(
			ColumnReferenceQualifier lhs,
			JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector joinCollector) {
		getEntityDescriptor().applyTableReferenceJoins( lhs, joinType, sqlAliasBase, joinCollector );
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			JoinType joinType,
			LockMode lockMode,
			SqlAstCreationState creationState) {
		final SqlAliasBase sqlAliasBase = creationState.getSqlAliasBaseGenerator().createSqlAliasBase( getSqlAliasStem() );

		final ToOneJoinCollectorImpl joinCollector = new ToOneJoinCollectorImpl(
				navigablePath,
				this,
				lhs,
				explicitSourceAlias,
				lockMode
		);

		// handle optional entity references to be outer joins.
		if ( JoinType.INNER.equals( joinType ) && ( isNullable() || !lhs.isInnerJoinPossible() ) ) {
			joinType = JoinType.LEFT;
		}

		getEntityDescriptor().applyTableReferenceJoins(
				lhs,
				joinType,
				sqlAliasBase,
				joinCollector
		);

		return joinCollector.generateTableGroup( joinType );
	}

	@Override
	public Object unresolve(Object value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}

		if ( !getEntityDescriptor().isInstance( value ) ) {
			throw new HibernateException( "Unexpected value for unresolve [" + value + "], expecting entity instance" );
		}

		if ( referencedUkAttributeName == null || classification.equals( SingularAttributeClassification.ONE_TO_ONE ) ) {
			return getAssociatedEntityDescriptor().getIdentifierDescriptor().unresolve(
					// todo (6.0) - Needed to account for to-one proxies obtained from #getReference
					//		Should this be baked into #getIdentifier somehow?
					ForeignKeys.getEntityIdentifierIfNotUnsaved(
							getAssociatedEntityDescriptor().getEntityName(),
							value,
							session
					),
//					getAssociatedEntityDescriptor().getIdentifier( value, session ),
					session
			);
		}
		else {
			final NonIdPersistentAttribute referencedAttribute = getAssociatedEntityDescriptor()
					.findPersistentAttribute( referencedUkAttributeName );
			return referencedAttribute.unresolve(
					referencedAttribute.getPropertyAccess().getGetter().get( value ),
					session
			);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void dehydrate(
			Object value,
			JdbcValueCollector jdbcValueCollector,
			Clause clause,
			SharedSessionContractImplementor session) {
		if ( !clause.getInclusionChecker().test( this ) ) {
			return;
		}

		if ( classification == ONE_TO_ONE && isOptional() && !constrained ) {
			return;
		}

		final ExpressableType writeable;

		if ( referencedUkAttributeName == null ) {
			writeable = getAssociatedEntityDescriptor().getIdentifierDescriptor();
		}
		else {
			writeable = getAssociatedEntityDescriptor().findPersistentAttribute( referencedUkAttributeName );
		}

		if ( writeable != null ) {
			final Iterator<Column> columnItr = foreignKey.getColumnMappings().getReferringColumns().iterator();
			writeable.dehydrate(
					value,
					(jdbcValue, sqlExpressableType, boundColumn) -> {
						assert columnItr.hasNext();
						jdbcValueCollector.collect(
								jdbcValue,
								sqlExpressableType,
								columnItr.next()
						);
					},
					clause,
					session
			);
			assert !columnItr.hasNext();
		}
	}

	@Override
	public List<Column> getColumns() {
		// todo (6.0) - is this really necessary to use export-enabled
//		if ( foreignKey.isExportationEnabled() ) {
		return foreignKey.getColumnMappings().getReferringColumns();
//		}
//		return new ArrayList<>();
	}

	@Override
	public SimpleTypeDescriptor<?> getValueGraphType() {
		return getEntityDescriptor();
	}

	@Override
	public SimpleTypeDescriptor<?> getKeyGraphType() {
		return entityDescriptor.getIdentifierDescriptor().getNavigableType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object resolveHydratedState(
			Object hydratedForm,
			ExecutionContext executionContext,
			SharedSessionContractImplementor session,
			Object containerInstance) {
		if ( hydratedForm == null ) {
			return null;
		}

		// todo (6.0) : WrongClassException?

		if ( foreignKey.isReferenceToPrimaryKey() ) {
			// step 1 - generate EntityKey based on hydrated id form
			final Object resolvedIdentifier = getEntityDescriptor().getHierarchy()
					.getIdentifierDescriptor()
					.resolveHydratedState(
							hydratedForm,
							executionContext,
							session,
							null
					);
			final EntityKey entityKey = new EntityKey( resolvedIdentifier, getEntityDescriptor() );


			// step 2 - look for a matching entity (by EntityKey) on the context
			//		NOTE - we pass `! isOptional()` as `eager` to `resolveEntityInstance` because
			//		if it were being fetched dynamically (join fetch) that would have lead to an
			//		EntityInitializer for this entity being created and it would be available in the
			//		`resolutionContext`
			final Object entityInstance = executionContext.resolveEntityInstance( entityKey, !isOptional() );
			if ( entityInstance != null ) {
				return entityInstance;
			}

			// try loading it...
			//
			// todo (6.0) : need to make sure that the "JdbcValues" we are processing here have been added to the Session's stack of "load contexts"
			//		that allows the loader(s) to resolve entity's that are being loaded here.
			//
			//		NOTE : this is how we get around having to register a "holder" EntityEntry with the PersistenceContext
			//		but still letting other (recursive) loads find references we are loading.

			J loaded = getEntityDescriptor().getSingleIdLoader().load(
					resolvedIdentifier,
					LockOptions.READ,
					session
			);
			if ( loaded != null ) {
				return loaded;
			}
			throw new EntityNotFoundException(
					String.format(
							Locale.ROOT,
							"Unable to resolve entity-valued association [%s] foreign key value [%s] to associated entity instance of type [%s]",
							getNavigableRole(),
							resolvedIdentifier,
							getEntityDescriptor().getJavaTypeDescriptor()
					)
			);
		}
		else if ( referencedUkAttributeName != null ) {
			SingleUniqueKeyEntityLoader<J> loader = new StandardSingleUniqueKeyEntityLoader(
					referencedUkAttribute,
					this
			);
			EntityUniqueKey euk = new EntityUniqueKey(
					getEntityDescriptor().getEntityName(),
					referencedUkAttributeName,
					hydratedForm,
					getAssociatedEntityDescriptor().getIdentifierDescriptor().getJavaTypeDescriptor(),
					referencedUkAttribute.getJavaTypeDescriptor(),
					getEntityDescriptor().getHierarchy().getRepresentation()
			);
			// todo (6.0) : look into resolutionContext
			J loaded = loader.load( hydratedForm, LockOptions.READ, session );
			// todo (6.0) : if loaded != null add it to the resolutionContext
			return loaded;
		}
		return null;
	}

	@Override
	public void collectNonNullableTransientEntities(
			Object value,
			ForeignKeys.Nullifier nullifier,
			NonNullableTransientDependencies nonNullableTransientEntities,
			SharedSessionContractImplementor session) {
		if ( !isNullable()
				&& getAttributeTypeClassification() != ONE_TO_ONE
				&& nullifier.isNullifiable( getEntityDescriptor().getEntityName(), value ) ) {
			nonNullableTransientEntities.add( getEntityDescriptor().getEntityName(), value );
		}
	}

	@Override
	public int getNumberOfJdbcParametersNeeded() {
		return foreignKey.getColumnMappings().getColumnMappings().size();
	}

	public AllowableParameterType resolveTemporalPrecision(
			TemporalType temporalType,
			TypeConfiguration typeConfiguration) {
		throw new UnsupportedOperationException( "ManyToOne cannot be treated as temporal type" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean isDirty(Object originalValue, Object currentValue, SharedSessionContractImplementor session) {
		if ( classification == ONE_TO_ONE ) {
			return false;
		}
		if ( Objects.equals( originalValue, currentValue ) ) {
			return false;
		}

		Object oldIdentifier = extractFkValue( originalValue );
		Object newIdentifier = extractFkValue( currentValue );

		return !getEntityDescriptor()
				.getIdentifierDescriptor()
				.areEqual( oldIdentifier, newIdentifier );
	}

	@Override
	public boolean isModified(Object old, Object current, SharedSessionContractImplementor session) {
		if ( classification == ONE_TO_ONE ) {
			return false;
		}
		if ( Objects.equals( old, current ) ) {
			return false;
		}

		Object oldIdentifier = extractFkValue( old );
		Object newIdentifier = extractFkValue( current );
		if ( referencedUkAttributeName == null || classification.equals( SingularAttributeClassification.ONE_TO_ONE ) ) {
			return getEntityDescriptor()
					.getIdentifierDescriptor().isModified( oldIdentifier, newIdentifier, session );
		}
		else {
			return getAssociatedEntityDescriptor()
					.findPersistentAttribute( referencedUkAttributeName ).isDirty(
							oldIdentifier,
							newIdentifier,
							session
					);
		}
	}

	@Override
	public boolean areEqual(Object x, Object y) throws HibernateException {
		return getEntityDescriptor().areEqual( (J) x, (J) y );
	}

	@Override
	public int extractHashCode(Object o) {
		return getEntityDescriptor().extractHashCode( (J) o );
	}

	public Object extractFkValue(Object value) {
		if ( value == null ) {
			return null;
		}

		if ( !getEntityDescriptor().isInstance( value ) ) {
			throw new HibernateException( "Unexpected value for unresolve [" + value + "], expecting entity instance" );
		}

		if ( referencedUkAttributeName == null || classification.equals( SingularAttributeClassification.ONE_TO_ONE ) ) {
			return getAssociatedEntityDescriptor().getIdentifier( value );
		}
		else {
			return getAssociatedEntityDescriptor()
					.findPersistentAttribute( referencedUkAttributeName )
					.getPropertyAccess()
					.getGetter()
					.get( value );
		}
	}

	@Override
	public List<ColumnReference> resolveColumnReferences(
			ColumnReferenceQualifier qualifier,
			SqlAstCreationState creationState) {
		List<ColumnReference> columnReferences = new ArrayList<>();
		for ( Column column : foreignKey.getColumnMappings().getReferringColumns() ) {
			columnReferences.add( new ColumnReference( qualifier, column ) );
		}
		return columnReferences;
	}

	@Override
	protected void instantiationComplete(
			PersistentAttributeMapping bootModelAttribute,
			RuntimeModelCreationContext context) {
		super.instantiationComplete( bootModelAttribute, context );

		// todo (6.0) : determine mutability plan based
		// for now its set to immutable

		this.mutabilityPlan = ImmutableMutabilityPlan.INSTANCE;
	}

	public ForeignKey getForeignKey() {
		return foreignKey;
	}

	@Override
	public void visitColumns(
			BiConsumer<SqlExpressableType, Column> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		for ( ColumnMappings.ColumnMapping columnMapping : getForeignKey().getColumnMappings().getColumnMappings() ) {
			final Column column = columnMapping.getReferringColumn();
			action.accept( column.getExpressableType(), column );
		}
	}
}
