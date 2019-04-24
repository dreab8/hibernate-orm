/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.internal.NonNullableTransientDependencies;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.MarkerObject;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.LoadingCollectionEntry;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.descriptor.java.internal.CollectionJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * todo (6.0) : potentially split this single impl into specific impls for each "collection nature":
 * 		`collection`:: {@link BagPersistentAttribute}
 * 		`list`:: {@link ListPersistentAttribute}
 * 		`set`:: {@link SetPersistentAttribute}
 * 		`map`:: {@link MapPersistentAttribute}
 * 		`bag`:: synonym for `collection`
 * 		`idbag`:: not sure yet
 *
 * @author Steve Ebersole
 */
public abstract class AbstractPluralPersistentAttribute<O,C,E> extends AbstractPersistentAttribute<O,C> implements PluralPersistentAttribute<O,C,E> {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( AbstractPluralPersistentAttribute.class );
	private static final Object NOT_NULL_COLLECTION = new MarkerObject( "NOT NULL COLLECTION" );

	private final PersistentCollectionDescriptor<O,C,E> collectionDescriptor;
	private final FetchStrategy fetchStrategy;
	private CascadeStyle cascadeStyle;
	private final boolean isNullable;
	private final String cascade;

	private int stateArrayPosition;

	@SuppressWarnings("unchecked")
	public AbstractPluralPersistentAttribute(
			PersistentCollectionDescriptor collectionDescriptor,
			Property bootProperty,
			PropertyAccess propertyAccess,
			RuntimeModelCreationContext creationContext) {
		super( collectionDescriptor.getContainer(), bootProperty, propertyAccess );

		final Collection bootCollectionDescriptor = (Collection) bootProperty.getValue();

		this.collectionDescriptor = collectionDescriptor;

		creationContext.registerCollectionDescriptor( collectionDescriptor, bootCollectionDescriptor );

		this.isNullable = bootCollectionDescriptor.isNullable();
		this.fetchStrategy = DomainModelHelper.determineFetchStrategy( bootCollectionDescriptor );
		this.cascade = bootProperty.getCascade();
	}

	@Override
	public PersistentCollectionDescriptor<O,C,E> getPersistentCollectionDescriptor() {
		return collectionDescriptor;
	}

	@Override
	public Class getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return getCollectionDescriptor().isCascadeDeleteEnabled();
	}

	@Override
	public CascadeStyle getCascadeStyle() {
		if ( cascadeStyle == null ) {
			if ( collectionDescriptor.getElementDescriptor() instanceof CollectionElementEmbedded ) {
				cascadeStyle = elementCascadeStyle( (CollectionElementEmbedded) collectionDescriptor.getElementDescriptor() );
			}
			else {
				cascadeStyle = DomainModelHelper.determineCascadeStyle( cascade );
			}
		}
		return cascadeStyle;
	}

	private CascadeStyle elementCascadeStyle(CollectionElementEmbedded element) {
		EmbeddedTypeDescriptor embeddedDescriptor = element.getEmbeddedDescriptor();

		for ( StateArrayContributor contributor : embeddedDescriptor.getStateArrayContributors() ) {
			if ( contributor.getCascadeStyle() != CascadeStyles.NONE ) {
				return CascadeStyles.ALL;
			}
		}
		return DomainModelHelper.determineCascadeStyle( cascade );
	}

	@Override
	public void collectNonNullableTransientEntities(
			Object value,
			ForeignKeys.Nullifier nullifier,
			NonNullableTransientDependencies nonNullableTransientEntities,
			SharedSessionContractImplementor session) {
		// todo (6.0) : prior versions essentially skipped collections when doing this process
		//		verify this is actually correct... the collection can hold non-null, transient entities as
		//		well and not cascade to them, seems like it should add them.  Did previous versions handle
		// 		the collection values differently?
	}

	@Override
	public BindableType getBindableType() {
		return BindableType.PLURAL_ATTRIBUTE;
	}

	@Override
	public ManagedTypeDescriptor getContainer() {
		return getPersistentCollectionDescriptor().getContainer();
	}

	@Override
	public SimpleTypeDescriptor getElementType() {
		return collectionDescriptor.getElementDescriptor().getDomainTypeDescriptor();
	}

	@Override
	public SimpleTypeDescriptor<?> getValueGraphType() {
		return getElementType();
	}

	@Override
	public SimpleTypeDescriptor<?> getKeyGraphType() {
		return collectionDescriptor.getIndexDescriptor() == null
				? null
				: collectionDescriptor.getIndexDescriptor().getDomainTypeDescriptor();
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return false;
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		final CollectionElement collectionElement = getPersistentCollectionDescriptor().getElementDescriptor();
		switch ( collectionElement.getClassification() ) {
			case EMBEDDABLE:
			case BASIC:
				return PersistentAttributeType.ELEMENT_COLLECTION;
			case MANY_TO_MANY:
				return PersistentAttributeType.MANY_TO_MANY;
			case ONE_TO_MANY:
				return PersistentAttributeType.ONE_TO_MANY;
			default:
				throw new NotYetImplementedFor6Exception(  );
		}
	}

	@Override
	public boolean isAssociation() {
		return getPersistentAttributeType() == PersistentAttributeType.ONE_TO_MANY
				|| getPersistentAttributeType() == PersistentAttributeType.MANY_TO_MANY;
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public Class getBindableJavaType() {
		return getPersistentCollectionDescriptor().getElementDescriptor().getJavaType();
	}

	@Override
	public boolean isNullable() {
		return isNullable;
	}

	@Override
	public void visitColumns(
			BiConsumer action, Clause clause, TypeConfiguration typeConfiguration) {

	}

	@Override
	public boolean isInsertable() {
		return false;
	}

	@Override
	public boolean isUpdatable() {
		return false;
	}

	@Override
	public int getStateArrayPosition() {
		return stateArrayPosition;
	}

	@Override
	public void setStateArrayPosition(int position) {
		this.stateArrayPosition = position;
	}

	@Override
	public CollectionMutabilityPlan getMutabilityPlan() {
		return getCollectionDescriptor().getMutabilityPlan();
	}

	@Override
	public Navigable findNavigable(String navigableName) {
		return getPersistentCollectionDescriptor().findNavigable( navigableName );
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		getPersistentCollectionDescriptor().visitNavigables( visitor );
	}

	@Override
	public NavigableRole getNavigableRole() {
		return collectionDescriptor.getNavigableRole();
	}

	@Override
	public String asLoggableText() {
		return toString();
	}

	@Override
	public CollectionJavaDescriptor getJavaTypeDescriptor() {
		return collectionDescriptor.getJavaTypeDescriptor();
	}

//	@Override
//	public SqmPluralAttributeReference createSqmExpression(
//			SqmFrom sourceSqmFrom,
//			SqmNavigableContainerReference containerReference,
//			SqmCreationState creationState) {
//		return new SqmPluralAttributeReference(
//				containerReference,
//				(SqmNavigableJoin) sourceSqmFrom,
//				this,
//				creationState
//		);
//	}

	@Override
	public boolean isIncludedInDirtyChecking() {
		// todo (6.0) : depends how we want to handle dirty collections marking container as dirty
		//		this is only important for versioned entities
		//
		// for now return true
		return true;
	}

	@Override
	public DomainResult createDomainResult(
			NavigablePath navigablePath,
			String resultVariable,
			DomainResultCreationState creationState) {
		return getPersistentCollectionDescriptor().createDomainResult(
				navigablePath,
				resultVariable,
				creationState
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
		return getPersistentCollectionDescriptor().generateFetch(
				fetchParent,
				fetchTiming,
				selected,
				lockMode,
				resultVariable,
				creationState
		);
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		return fetchStrategy;
	}

	@Override
	public List<Column> getColumns() {
		return Collections.emptyList();
	}

	@Override
	public Object hydrate(Object jdbcValues, SharedSessionContractImplementor session) {
		if ( jdbcValues == null ) {
			return null;
		}

		return NOT_NULL_COLLECTION;
	}

	@Override
	public Object resolveHydratedState(
			Object hydratedForm,
			ExecutionContext executionContext,
			SharedSessionContractImplementor session,
			Object containerInstance) {
		// todo (6.0) : use the collection-key descriptor to resolve the hydrated key value...
		//
		// for now we hack it in such a way that will work for all simple identifiers

		final IdentifiableTypeDescriptor ownerDescriptor = (IdentifiableTypeDescriptor) getContainer();
		final EntityIdentifierSimple identifierDescriptor = (EntityIdentifierSimple) ownerDescriptor.getIdentifierDescriptor();

		final Object key = identifierDescriptor.asAttribute( identifierDescriptor.getJavaType() ).getPropertyAccess().getGetter().get( containerInstance );

		PersistentCollectionDescriptor collectionDescriptor = getPersistentCollectionDescriptor();

		final CollectionKey collectionKey = new CollectionKey( collectionDescriptor, key );

		final PersistenceContext persistenceContext = session.getPersistenceContext();

		// check if collection is currently being loaded
		final LoadingCollectionEntry loadingCollectionEntry = persistenceContext.getLoadContexts()
				.findLoadingCollectionEntry( collectionKey );

		PersistentCollection collection = loadingCollectionEntry == null ? null : loadingCollectionEntry.getCollectionInstance();

		if ( collection == null ) {
			// check if it is already completely loaded, but unowned
			collection = persistenceContext.useUnownedCollection( collectionKey );

			if ( collection == null ) {

				collection = persistenceContext.getCollection( collectionKey );

				if ( collection == null ) {
					// create a new collection wrapper, to be initialized later
					collection = collectionDescriptor.instantiateWrapper( session, key );
					collection.setOwner( containerInstance );

					persistenceContext.addUninitializedCollection( collectionDescriptor, collection, key );

					// todo (6.0) (fetching) : handle fetching or not
					//
					// for now. lazy

//					boolean eager = overridingEager != null ? overridingEager : ! isLazy();
//					if ( initializeImmediately() ) {
//						session.initializeCollection( collection, false );
//					}
//					else if ( eager ) {
//						persistenceContext.addNonLazyCollection( collection );
//					}
//
//					if ( hasHolder() ) {
//						session.getPersistenceContext().addCollectionHolder( collection );
//					}
				}

			}

			if ( log.isTraceEnabled() ) {
				log.tracef(
						"Created collection wrapper: %s",
						MessageHelper.collectionInfoString(
								collectionDescriptor,
								collection,
								key,
								session
						)
				);
			}

		}

		collection.setOwner( containerInstance );
		collection.setCurrentSession( session );

		return collection.getValue();
	}

	@Override
	public Object unresolve(Object value, SharedSessionContractImplementor session) {
//		throw new NotYetImplementedFor6Exception();
		// can't just return null here, since that would
		// cause an owning component to become null
		return NOT_NULL_COLLECTION;
	}

	@Override
	public void dehydrate(
			Object value,
			JdbcValueCollector jdbcValueCollector,
			Clause clause,
			SharedSessionContractImplementor session) {
		if ( value != null && value != NOT_NULL_COLLECTION ) {
			getPersistentCollectionDescriptor().getElementDescriptor().dehydrate(
					value,
					jdbcValueCollector,
					clause,
					session
			);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean isDirty(Object originalValue, Object currentValue, SharedSessionContractImplementor session) {
		return !getJavaTypeDescriptor().areEqual( originalValue, currentValue );
	}

	@Override
	public void visitFetchables(Consumer consumer) {
		getPersistentCollectionDescriptor().visitFetchables( consumer );
	}

	@Override
	public String toString() {
		return "PluralPersistentAttribute(" + getNavigableRole() + ")";
	}

	@Override
	public DomainType<C> getAttributeType() {
		return this;
	}

	@Override
	public ForeignKeyDirection getForeignKeyDirection() {
		return getCollectionDescriptor().getForeignKeyDirection();
	}

	@Override
	public Object replace(C originalValue, C targetValue, Object owner, Map copyCache, SessionImplementor session) {
		// todo (6.0) - This implementation should be moved to the MutabilityPlan.
		if ( originalValue == null ) {
			return  null;
		}

		if ( !Hibernate.isInitialized( originalValue ) ) {
			if ( ( (PersistentCollection) originalValue ).hasQueuedOperations() ) {
				if ( originalValue == targetValue ) {
					// A managed entity with an uninitialized collection is being merged,
					// We need to replace any detached entities in the queued operations
					// with managed copies.
					final AbstractPersistentCollection pc = (AbstractPersistentCollection) originalValue;
					pc.replaceQueuedOperationValues( getCollectionDescriptor(), copyCache );
				}
				else {
					// original is a detached copy of the collection;
					// it contains queued operations, which will be ignored
					log.ignoreQueuedOperationsOnMerge(
							MessageHelper.collectionInfoString(
									collectionDescriptor,
									( (PersistentCollection) targetValue ),
									( (PersistentCollection) targetValue ).getKey(),
									session
							)
					);
				}
			}
			return targetValue;
		}

		// For a null target or a target which is the same as the original,
		// we need to put the merged elements into a new collection.
		C result = ( targetValue == null ||
				targetValue == originalValue ||
				targetValue == LazyPropertyInitializer.UNFETCHED_PROPERTY ) ?
				instantiateResult( originalValue ) : targetValue;

		// For arrays, replaceElements() may return a different reference,
		// since the array length might not match.
		result = replaceElements( originalValue, result, owner, copyCache, session );

		if ( originalValue == targetValue ) {
			// Get the elements back into the target, making sure to handle the dirty flag.
			boolean wasClean = PersistentCollection.class.isInstance( targetValue ) &&
					!( (PersistentCollection) targetValue ).isDirty();

			// todo (6.0) - this is a bit inefficient, no need to do a whole deep replaceElements() call
			replaceElements( result, targetValue, owner, copyCache, session );
			if ( wasClean ) {
				( (PersistentCollection) targetValue ).clearDirty();;
			}
			result = targetValue;
		}

		return result;
	}

	@Override
	public Object replace(
			C originalValue,
			C targetValue,
			Object owner,
			Map copyCache,
			ForeignKeyDirection foreignKeyDirection,
			SessionImplementor session) {
		// todo (6.0) - This implementation should be moved to the MutabilityPlan.
		boolean include;
		if ( isAssociation() ) {
			include = getForeignKeyDirection() == foreignKeyDirection;
		}
		else {
			include = ForeignKeyDirection.FROM_PARENT == foreignKeyDirection;
		}

		return include ? replace( originalValue, targetValue, owner, copyCache, session ) : targetValue;
	}

	@SuppressWarnings("unchecked")
	protected C instantiateResult(C originalValue) {
		// todo (6.0) - how to handle arrays?  5.x handled this slightly different.
		return (C) collectionDescriptor.instantiateRaw( -1 );
	}

	protected C replaceElements(
			C originalValue,
			C targetValue,
			Object owner,
			Map copyCache,
			SessionImplementor session) {
		java.util.Collection result = (java.util.Collection) targetValue;
		result.clear();

		// copy elements into newly empty target collection
		final CollectionElement elementDescriptor = collectionDescriptor.getElementDescriptor();
		Iterator iter = ( (java.util.Collection) originalValue ).iterator();
		while ( iter.hasNext() ) {
			result.add( elementDescriptor.replace( iter.next(), null, owner, copyCache, session ) );
		}

		// If the originalValue is a PersistentCollection and that originalValue
		// was not flagged as dirty, then reset the targetValue's dirty flag
		// here after the copy operation
		//
		// One thing to note is if the originalValue was a bare collection, we
		// should not reset the dirty flag because we simply do not know.
		if ( originalValue instanceof PersistentCollection ) {
			if ( targetValue instanceof PersistentCollection ) {
				final PersistentCollection originalCollection  = (PersistentCollection) originalValue;
				final PersistentCollection targetCollection = (PersistentCollection) targetValue;

				preserveSnapshot( originalCollection, targetCollection, owner, copyCache, session );

				if ( !originalCollection.isDirty() ) {
					targetCollection.clearDirty();
				}
			}
		}

		return (C) result;
	}

	protected void preserveSnapshot(
			PersistentCollection originalCollection,
			PersistentCollection targetCollection,
			Object owner,
			Map copyCache,
			SessionImplementor session) {

		// todo (6.0) - is it possible to refactor this code to subtypes?

		Object originalValue = originalCollection.getStoredSnapshot();
		Object targetValue = targetCollection.getStoredSnapshot();
		Object result;

		final CollectionElement elementDescriptor = getCollectionDescriptor().getElementDescriptor();

		if ( originalValue instanceof List ) {
			result = new ArrayList<>( ( (List) originalValue ).size() );
			for ( Object entry : (List) originalValue ) {
				( (List) result ).add( elementDescriptor.replace( entry, null, owner, copyCache, session ) );
			}
		}
		else if ( originalValue instanceof Map ) {
			if ( originalValue instanceof SortedMap ) {
				result = new TreeMap<>( ( (SortedMap) originalValue ).comparator() );
			}
			else {
				result = new HashMap<>(
						CollectionHelper.determineProperSizing( ( (Map) originalValue ).size() ),
						CollectionHelper.LOAD_FACTOR
				);
			}

			for ( Map.Entry<?, ?> entry : ( (Map<?, ?>) originalValue ).entrySet() ) {
				Object key = entry.getKey();
				Object value = entry.getValue();
				Object resultSnapshotValue = ( targetValue == null )
						? null
						: ( (Map<?, ?>) targetValue ).get( key );

				Object newValue = elementDescriptor.replace( value, resultSnapshotValue,owner, copyCache, session );

				if ( key == value ) {
					( (Map) result ).put( newValue, newValue );

				}
				else {
					( (Map) result ).put( key, newValue );
				}
			}

		}
		else if ( originalValue instanceof Object[] ) {
			Object[] arr = (Object[]) originalValue;
			for ( int i = 0; i < arr.length; i++ ) {
				arr[i] = elementDescriptor.replace( arr[i], null, owner, copyCache, session );
			}
			result = originalValue;

		}
		else {
			// retain the same snapshot
			result = targetValue;
		}

		CollectionEntry entry = session.getPersistenceContext().getCollectionEntry( targetCollection );
		if ( entry != null ) {
			entry.resetStoredSnapshot( targetCollection, (Serializable) result );
		}
	}

}
