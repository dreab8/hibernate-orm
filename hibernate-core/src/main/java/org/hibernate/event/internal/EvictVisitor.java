/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.model.domain.spi.PluralAttributeCollection;
import org.hibernate.pretty.MessageHelper;

/**
 * Evict any collections referenced by the object from the session cache.
 * This will NOT pick up any collections that were dereferenced, so they
 * will be deleted (suboptimal but not exactly incorrect).
 *
 * @author Gavin King
 */
public class EvictVisitor extends AbstractVisitor {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( EvictVisitor.class );

	private Object owner;

	EvictVisitor(EventSource session, Object owner) {
		super(session);
		this.owner = owner;
	}

	@Override
	Object processCollection(Object collection, PluralAttributeCollection attributeCollection) throws HibernateException {
		if (collection != null) {
			evictCollection(collection, attributeCollection);
		}

		return null;
	}
	public void evictCollection(Object value, PluralAttributeCollection attributeCollection) {
		final PersistentCollection collection;
		if ( attributeCollection.getPersistentCollectionDescriptor()
				.getCollectionClassification() == CollectionClassification.ARRAY ) {
			collection = getSession().getPersistenceContext().removeCollectionHolder(value);
		}
		else if ( value instanceof PersistentCollection ) {
			collection = (PersistentCollection) value;
		}
		else if ( value == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
//			collection = (PersistentCollection) type.resolve( value, getSession(), this.owner );
			throw new NotYetImplementedFor6Exception( "evictCollection + LazyPropertyInitializer.UNFETCHED_PROPERTY" );
		}
		else {
			return; //EARLY EXIT!
		}

		if ( collection != null && collection.unsetSession( getSession() ) ) {
			evictCollection( collection );
		}
	}

	private void evictCollection(PersistentCollection collection) {
		CollectionEntry ce = (CollectionEntry) getSession().getPersistenceContext().getCollectionEntries().remove(collection);
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Evicting collection: %s",
					MessageHelper.collectionInfoString( ce.getLoadedPersistentCollectionDescriptor(),
							collection,
							ce.getLoadedKey(),
							getSession() ) );
		}
		if (ce.getLoadedPersistentCollectionDescriptor() != null && ce.getLoadedPersistentCollectionDescriptor().getBatchSize() > 1) {
			getSession().getPersistenceContext().getBatchFetchQueue().removeBatchLoadableCollection(ce);
		}
		if ( ce.getLoadedPersistentCollectionDescriptor() != null && ce.getLoadedKey() != null ) {
			//TODO: is this 100% correct?
			getSession().getPersistenceContext().getCollectionsByKey().remove(
					new CollectionKey( ce.getLoadedPersistentCollectionDescriptor(), ce.getLoadedKey() )
			);
		}
	}

	@Override
	boolean includeEntityProperty(Object[] values, int i) {
		return true;
	}
}
