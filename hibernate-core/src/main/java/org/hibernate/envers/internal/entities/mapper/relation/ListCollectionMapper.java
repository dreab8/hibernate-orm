/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.internal.entities.mapper.PersistentCollectionChangeData;
import org.hibernate.envers.internal.entities.mapper.PropertyMapper;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.Initializor;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.initializor.ListCollectionInitializor;
import org.hibernate.envers.internal.entities.mapper.relation.lazy.proxy.ListProxy;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.Pair;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public final class ListCollectionMapper extends AbstractCollectionMapper<List> implements PropertyMapper {
	private final MiddleComponentData elementComponentData;
	private final MiddleComponentData indexComponentData;

	public ListCollectionMapper(
			CommonCollectionMapperData commonCollectionMapperData,
			MiddleComponentData elementComponentData,
			MiddleComponentData indexComponentData,
			boolean revisionTypeInId) {
		super( commonCollectionMapperData, List.class, ListProxy.class, false, revisionTypeInId );
		this.elementComponentData = elementComponentData;
		this.indexComponentData = indexComponentData;
	}

	@Override
	protected Initializor<List> getInitializor(
			AuditReaderImplementor versionsReader,
			Object primaryKey,
			Number revision,
			boolean removed) {
		return new ListCollectionInitializor(
				versionsReader,
				commonCollectionMapperData.getQueryGenerator(),
				primaryKey,
				revision,
				removed,
				elementComponentData,
				indexComponentData
		);
	}

	@Override
	@SuppressWarnings({"unchecked"})
	protected Collection getNewCollectionContent(PersistentCollection newCollection) {
		if ( newCollection == null ) {
			return null;
		}
		else {
			return listToIndexElementPairList( (List<Object>) newCollection );
		}
	}

	@Override
	@SuppressWarnings({"unchecked"})
	protected Collection getOldCollectionContent(Serializable oldCollection) {
		if ( oldCollection == null ) {
			return null;
		}
		else {
			return listToIndexElementPairList( (List<Object>) oldCollection );
		}
	}

	@Override
	@SuppressWarnings({"unchecked"})
	protected void mapToMapFromObject(
			SessionImplementor session,
			Map<String, Object> idData,
			Map<String, Object> data,
			Object changed) {
		final Pair<Integer, Object> indexValuePair = (Pair<Integer, Object>) changed;
		elementComponentData.getComponentMapper().mapToMapFromObject(
				session,
				idData,
				data,
				indexValuePair.getSecond()
		);
		indexComponentData.getComponentMapper().mapToMapFromObject( session, idData, data, indexValuePair.getFirst() );
	}

	@Override
	protected Set<Object> buildCollectionChangeSet(Object eventCollection, Collection collection) {
		final Set<Object> changeSet = new HashSet<>();
		if ( eventCollection != null ) {
			for ( Object entry : collection ) {
				if ( entry != null ) {
					final Pair pair = Pair.class.cast( entry );
					if ( pair.getSecond() == null ) {
						continue;
					}
					changeSet.add( entry );
				}
			}
		}
		return changeSet;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<PersistentCollectionChangeData> mapCollectionChanges(
			SessionImplementor session,
			PersistentCollection newColl,
			Serializable oldColl,
			Object id) {
		final List<PersistentCollectionChangeData> collectionChanges = new ArrayList<>();

		final PersistentCollectionDescriptor collectionDescriptor = resolveCollectionDescriptor( session, newColl );

		// Comparing new and old collection content.
		final Collection newCollection = getNewCollectionContent( newColl );
		final Collection oldCollection = getOldCollectionContent( oldColl );

		final Set<Object> addedElements = buildCollectionChangeSet( newColl, newCollection );
		if ( oldColl != null ) {
			for ( int i = 0; i < oldCollection.size(); ++i ) {
				Pair<Integer, ?> oldEntry = ((List<Pair<Integer, ?>>) oldCollection).get( i );
				for( Iterator itor = addedElements.iterator(); itor.hasNext(); ) {
					Pair<Integer, ?> addedEntry = (Pair<Integer, ?>) itor.next();
					if ( oldEntry.getFirst().equals( addedEntry.getFirst() ) ) {
						final Object oldValue = oldEntry.getSecond();
						final Object addedValue = addedEntry.getSecond();
						if ( isSame( collectionDescriptor, oldValue, addedValue, session ) ) {
							itor.remove();
							break;
						}
					}
				}
			}
		}

		final Set<Object> deleteElements = buildCollectionChangeSet( oldColl, oldCollection );
		if ( newColl != null ) {
			for ( int i = 0; i < newCollection.size(); ++i ) {
				Pair<Integer, ?> newEntry = ((List<Pair<Integer, ?>>) newCollection).get( i );
				for ( Iterator itor = deleteElements.iterator(); itor.hasNext(); ) {
					Pair<Integer, ?> deletedEntry = (Pair<Integer, ?>) itor.next();
					if ( newEntry.getFirst().equals( deletedEntry.getFirst() ) ) {
						final Object newValue = newEntry.getSecond();
						final Object deletedValue = deletedEntry.getSecond();
						if ( isSame( collectionDescriptor, deletedValue, newValue, session ) ) {
							itor.remove();
							break;
						}
					}
				}
			}
		}

		addCollectionChanges( session, collectionChanges, addedElements, RevisionType.ADD, id );
		addCollectionChanges( session, collectionChanges, deleteElements, RevisionType.DEL, id );

		return collectionChanges;
	}

	private static <T> List<Pair<Integer, T>> listToIndexElementPairList(List<T> list) {
		final List<Pair<Integer, T>> ret = new ArrayList<>();
		final Iterator<T> listIter = list.iterator();
		for ( int i = 0; i < list.size(); i++ ) {
			ret.add( Pair.make( i, listIter.next() ) );
		}

		return ret;
	}
}
