/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AbstractFetchParentAccess;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.LoadingEntityEntry;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class EntityFetchDelayedInitializer extends AbstractFetchParentAccess implements EntityInitializer {

	private final NavigablePath navigablePath;
	private final EntityPersister concreteDescriptor;
	private final DomainResultAssembler identifierAssembler;

	private Object entityInstance;
	private Object identifier;

	public EntityFetchDelayedInitializer(
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResultAssembler identifierAssembler) {
		this.navigablePath = fetchedNavigable;
		this.concreteDescriptor = concreteDescriptor;
		this.identifierAssembler = identifierAssembler;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
		// nothing to do
	}

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {
		if ( entityInstance != null ) {
			return;
		}
		identifier = identifierAssembler.assemble( rowProcessingState );

		if ( identifier == null ) {
			// todo (6.0) : check this is the correct behaviour
			entityInstance = null;
		}
		else {
			final EntityKey entityKey = new EntityKey( identifier, concreteDescriptor );
			PersistenceContext persistenceContext = rowProcessingState.getSession().getPersistenceContext();
			final Object entity = persistenceContext.getEntity( entityKey );
			if ( entity != null ) {
				entityInstance = entity;
			}
			else {
				LoadingEntityEntry loadingEntityLocally = rowProcessingState.getJdbcValuesSourceProcessingState()
						.findLoadingEntityLocally( entityKey );
				if ( loadingEntityLocally != null ) {
					entityInstance = loadingEntityLocally.getEntityInstance();
				}
				else if ( concreteDescriptor.hasProxy() ) {
					final Object proxy = persistenceContext.getProxy( entityKey );
					if ( proxy != null ) {
						entityInstance = proxy;
					}
					else {
						entityInstance = concreteDescriptor.createProxy(
								identifier,
								rowProcessingState.getSession()
						);
						persistenceContext
								.getBatchFetchQueue().addBatchLoadableEntityKey( entityKey );
						persistenceContext.addProxy( entityKey, entityInstance );
					}
				}
				else if ( concreteDescriptor
						.getBytecodeEnhancementMetadata()
						.isEnhancedForLazyLoading() ) {
					entityInstance = concreteDescriptor.instantiate(
							identifier,
							rowProcessingState.getSession()
					);
				}
			}

			notifyParentResolutionListeners( entityInstance );
		}
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
		// nothing to do
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		entityInstance = null;
		identifier = null;

		clearParentResolutionListeners();
	}

	@Override
	public EntityPersister getEntityDescriptor() {
		return concreteDescriptor;
	}

	@Override
	public Object getEntityInstance() {
		return entityInstance;
	}

	@Override
	public EntityKey getEntityKey() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Object getParentKey() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void registerResolutionListener(Consumer<Object> listener) {
		if ( entityInstance != null ) {
			listener.accept( entityInstance );
		}
		else {
			super.registerResolutionListener( listener );
		}
	}

}
