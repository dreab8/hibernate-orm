/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.EntityMode;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.CacheHelper;
import org.hibernate.internal.CoreLogging;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

import org.jboss.logging.Logger;

/**
 * Tracks entity and collection keys that are available for batch
 * fetching, and the queries which were used to load entities, which
 * can be re-used as a subquery for loading owned collections.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Guenther Demetz
 */
public class BatchFetchQueue {
	private static final Logger LOG = CoreLogging.logger( BatchFetchQueue.class );

	private final PersistenceContext context;

	/**
	 * A map of {@link SubselectFetch subselect-fetch descriptors} keyed by the
	 * {@link EntityKey) against which the descriptor is registered.
	 */
	private final Map<EntityKey, SubselectFetch> subselectsByEntityKey = new HashMap<>( 8 );

	/**
	 * Used to hold information about the entities that are currently eligible for batch-fetching.  Ultimately
	 * used by {@link #getEntityBatch} to build entity load batches.
	 * <p/>
	 * A Map structure is used to segment the keys by entity type since loading can only be done for a particular entity
	 * type at a time.
	 */
	private final Map <String,LinkedHashSet<EntityKey>> batchLoadableEntityKeys = new HashMap<>( 8 );
	
	/**
	 * Used to hold information about the collections that are currently eligible for batch-fetching.  Ultimately
	 * used by {@link #getCollectionBatch} to build collection load batches.
	 */
	private final Map<String, LinkedHashMap<CollectionEntry, PersistentCollection>> batchLoadableCollections = new HashMap<>( 8 );

	/**
	 * Constructs a queue for the given context.
	 *
	 * @param context The owning context.
	 */
	public BatchFetchQueue(PersistenceContext context) {
		this.context = context;
	}

	/**
	 * Clears all entries from this fetch queue.
	 * <p/>
	 * Called after flushing or clearing the session.
	 */
	public void clear() {
		batchLoadableEntityKeys.clear();
		batchLoadableCollections.clear();
		subselectsByEntityKey.clear();
	}


	// sub-select support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Retrieve the fetch descriptor associated with the given entity key.
	 *
	 * @param key The entity key for which to locate any defined subselect fetch.
	 * @return The fetch descriptor; may return null if no subselect fetch queued for
	 * this entity key.
	 */
	public SubselectFetch getSubselect(EntityKey key) {
		return subselectsByEntityKey.get( key );
	}

	/**
	 * Adds a subselect fetch decriptor for the given entity key.
	 *
	 * @param key The entity for which to register the subselect fetch.
	 * @param subquery The fetch descriptor.
	 */
	public void addSubselect(EntityKey key, SubselectFetch subquery) {
		subselectsByEntityKey.put( key, subquery );
	}

	/**
	 * After evicting or deleting an entity, we don't need to
	 * know the query that was used to load it anymore (don't
	 * call this after loading the entity, since we might still
	 * need to load its collections)
	 */
	public void removeSubselect(EntityKey key) {
		subselectsByEntityKey.remove( key );
	}

	// entity batch support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * If an EntityKey represents a batch loadable entity, add
	 * it to the queue.
	 * <p/>
	 * Note that the contract here is such that any key passed in should
	 * previously have been been checked for existence within the
	 * {@link PersistenceContext}; failure to do so may cause the
	 * referenced entity to be included in a batch even though it is
	 * already associated with the {@link PersistenceContext}.
	 */
	public void addBatchLoadableEntityKey(EntityKey key) {
		if ( key.isBatchLoadable() ) {
			LinkedHashSet<EntityKey> set =  batchLoadableEntityKeys.get( key.getEntityName());
			if (set == null) {
				set = new LinkedHashSet<>( 8 );
				batchLoadableEntityKeys.put( key.getEntityName(), set);
			}
			set.add(key);
		}
	}
	

	/**
	 * After evicting or deleting or loading an entity, we don't
	 * need to batch fetch it anymore, remove it from the queue
	 * if necessary
	 */
	public void removeBatchLoadableEntityKey(EntityKey key) {
		if ( key.isBatchLoadable() ) {
			LinkedHashSet<EntityKey> set =  batchLoadableEntityKeys.get( key.getEntityName());
			if (set != null) {
				set.remove(key);
			}
		}
	}

	/**
	 * Intended for test usage.  Really has no use-case in Hibernate proper.
	 */
	public boolean containsEntityKey(EntityKey key) {
		if ( key.isBatchLoadable() ) {
			LinkedHashSet<EntityKey> set =  batchLoadableEntityKeys.get( key.getEntityName());
			if ( set != null ) {
				return set.contains( key );
			}
		}
		return false;
	}

	/**
	 * Get a batch of unloaded identifiers for this class, using a slightly
	 * complex algorithm that tries to grab keys registered immediately after
	 * the given key.
	 *
	 * @param entityDescriptor The descriptor for the entities being loaded.
	 * @param id The identifier of the entity currently demanding load.
	 * @param batchSize The maximum number of keys to return
	 * @return an array of identifiers, of length batchSize (possibly padded with nulls)
	 */
	@SuppressWarnings("unchecked")
	public Object[] getEntityBatch(
			final EntityTypeDescriptor entityDescriptor,
			final Object id,
			final int batchSize,
			final EntityMode entityMode) {
		Object[] ids = new Serializable[batchSize];
		ids[0] = id; //first element of array is reserved for the actual instance we are loading!
		int i = 1;
		int end = -1;
		boolean checkForEnd = false;

		// TODO: this needn't exclude subclasses...

		LinkedHashSet<EntityKey> set =  batchLoadableEntityKeys.get( entityDescriptor.getEntityName() );
		if ( set != null ) {
			for ( EntityKey key : set ) {
				if ( checkForEnd && i == end ) {
					//the first id found after the given id
					return ids;
				}
				if ( entityDescriptor.getIdentifierDescriptor().getJavaTypeDescriptor().areEqual( id, key.getIdentifier() ) ) {
					end = i;
				}
				else {
					if ( !isCached( key, entityDescriptor ) ) {
						ids[i++] = key.getIdentifier();
					}
				}
				if ( i == batchSize ) {
					i = 1; // end of array, start filling again from start
					if ( end != -1 ) {
						checkForEnd = true;
					}
				}
			}
		}
		return ids; //we ran out of ids to try
	}

	private boolean isCached(EntityKey entityKey, EntityTypeDescriptor entityDescriptor) {
		final SharedSessionContractImplementor session = context.getSession();

		if ( ! session.getCacheMode().isGetEnabled() ) {
			return false;
		}

		if ( !entityDescriptor.canReadFromCache() ) {
			return false;
		}

		EntityDataAccess cacheAccess = entityDescriptor.getHierarchy().getEntityCacheAccess();
		final Object key = cacheAccess.generateCacheKey(
				entityKey.getIdentifier(),
				entityDescriptor.getHierarchy(),
				session.getFactory(),
				session.getTenantIdentifier()
		);
		return CacheHelper.fromSharedCache( session, key, cacheAccess ) != null;
	}
	

	// collection batch support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * If a CollectionEntry represents a batch loadable collection, add
	 * it to the queue.
	 */
	public void addBatchLoadableCollection(PersistentCollection collection, CollectionEntry ce) {
		final PersistentCollectionDescriptor collectionDescriptor = ce.getLoadedCollectionDescriptor();

		LinkedHashMap<CollectionEntry, PersistentCollection> map =  batchLoadableCollections.get( collectionDescriptor.getNavigableRole().getFullPath() );
		if ( map == null ) {
			map = new LinkedHashMap<>( 16 );
			batchLoadableCollections.put( collectionDescriptor.getNavigableRole().getFullPath(), map );
		}
		map.put( ce, collection );
	}
	
	/**
	 * After a collection was initialized or evicted, we don't
	 * need to batch fetch it anymore, remove it from the queue
	 * if necessary
	 */
	public void removeBatchLoadableCollection(CollectionEntry ce) {
		LinkedHashMap<CollectionEntry, PersistentCollection> map =  batchLoadableCollections.get( ce.getLoadedCollectionDescriptor().getNavigableRole().getFullPath() );
		if ( map != null ) {
			map.remove( ce );
		}
	}

	/**
	 * Get a batch of uninitialized collection keys for a given role
	 *
	 * @param persistentCollectionDescriptor The descriptor for the collection role.
	 * @param id A key that must be included in the batch fetch
	 * @param batchSize the maximum number of keys to return
	 * @return an array of collection keys, of length batchSize (padded with nulls)
	 */
	public Object[] getCollectionBatch(
			final PersistentCollectionDescriptor persistentCollectionDescriptor,
			final Serializable id,
			final int batchSize) {

		Object[] keys = new Object[batchSize];
		keys[0] = id;

		int i = 1;
		int end = -1;
		boolean checkForEnd = false;

		final LinkedHashMap<CollectionEntry, PersistentCollection> map =  batchLoadableCollections.get( persistentCollectionDescriptor.getNavigableRole().getFullPath() );
		if ( map != null ) {
			for ( Entry<CollectionEntry, PersistentCollection> me : map.entrySet() ) {
				final CollectionEntry ce = me.getKey();
				final PersistentCollection collection = me.getValue();
				
				if ( ce.getLoadedKey() == null ) {
					// the loadedKey of the collectionEntry might be null as it might have been reset to null
					// (see for example Collections.processDereferencedCollection()
					// and CollectionEntry.afterAction())
					// though we clear the queue on flush, it seems like a good idea to guard
					// against potentially null loadedKeys (which leads to various NPEs as demonstrated in HHH-7821).
					continue;
				}

				if ( collection.wasInitialized() ) {
					// should never happen
					LOG.warn( "Encountered initialized collection in BatchFetchQueue, this should not happen." );
					continue;
				}

				if ( checkForEnd && i == end ) {
					return keys; //the first key found after the given key
				}

				final boolean isEqual = persistentCollectionDescriptor.getCollectionKeyDescriptor()
						.getJavaTypeDescriptor()
						.areEqual(
								id,
								ce.getLoadedKey()
						);

				if ( isEqual ) {
					end = i;
					//checkForEnd = false;
				}
				else if ( !isCached( ce.getLoadedKey(), persistentCollectionDescriptor ) ) {
					keys[i++] = ce.getLoadedKey();
					//count++;
				}

				if ( i == batchSize ) {
					i = 1; //end of array, start filling again from start
					if ( end != -1 ) {
						checkForEnd = true;
					}
				}
			}
		}
		return keys; //we ran out of keys to try
	}

	private boolean isCached(Object collectionKey, PersistentCollectionDescriptor descriptor) {
		SharedSessionContractImplementor session = context.getSession();
		if ( session.getCacheMode().isGetEnabled() && descriptor.hasCache() ) {
			CollectionDataAccess cache = descriptor.getCacheAccess();
			Object cacheKey = cache.generateCacheKey(
					collectionKey,
					descriptor,
					session.getFactory(),
					session.getTenantIdentifier()
			);
			return CacheHelper.fromSharedCache( session, cacheKey, cache ) != null;
		}

		return false;
	}

}
