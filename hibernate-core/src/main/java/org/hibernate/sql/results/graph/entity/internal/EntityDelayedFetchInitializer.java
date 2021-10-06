/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.loader.entity.CacheEntityLoaderHelper;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
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
public class EntityDelayedFetchInitializer extends AbstractFetchParentAccess implements EntityInitializer {

	private final NavigablePath navigablePath;
	private final ToOneAttributeMapping referencedModelPart;
	private final DomainResultAssembler identifierAssembler;

	private Object entityInstance;
	private Object identifier;

	public EntityDelayedFetchInitializer(
			NavigablePath fetchedNavigable,
			ToOneAttributeMapping referencedModelPart,
			DomainResultAssembler identifierAssembler) {
		this.navigablePath = fetchedNavigable;
		this.referencedModelPart = referencedModelPart;
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
	public ModelPart getInitializedPart(){
		return referencedModelPart;
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
			final EntityPersister concreteDescriptor = referencedModelPart.getEntityMappingType().getEntityPersister();
			final EntityKey entityKey = new EntityKey( identifier, concreteDescriptor );
			final PersistenceContext persistenceContext = rowProcessingState.getSession().getPersistenceContext();

			LoadingEntityEntry loadingEntityLocally = persistenceContext
					.getLoadContexts().findLoadingEntityEntry( entityKey );
			if ( loadingEntityLocally != null ) {
				entityInstance = loadingEntityLocally.getEntityInstance();
			}
			else {
				final Object entity = persistenceContext.getEntity( entityKey );
				final Object proxy = persistenceContext.getProxy( entityKey );
				if ( entity != null && proxy != null && ( (HibernateProxy) proxy ).getHibernateLazyInitializer()
						.isUninitialized() ) {
					entityInstance = entity;
				}
				else {
					if ( proxy != null ) {
						entityInstance = proxy;
					}
					else {
						if ( entity != null ) {
							entityInstance = entity;
						}
						else {
							// Look into the second level cache if the descriptor is polymorphic
							final Object cachedEntity;
							if ( concreteDescriptor.getEntityMetamodel().hasSubclasses() ) {
								cachedEntity = CacheEntityLoaderHelper.INSTANCE.loadFromSecondLevelCache(
										rowProcessingState.getSession(),
										null,
										LockMode.NONE,
										concreteDescriptor,
										entityKey
								);
							}
							else {
								cachedEntity = null;
							}

							if ( cachedEntity != null ) {
								entityInstance = cachedEntity;
							}
							else {
								entityInstance = persistenceContext.getEntity( entityKey );
								if ( entityInstance == null ) {
									if ( concreteDescriptor.hasProxy()  ) {
										entityInstance = concreteDescriptor.createProxy(
												identifier,
												rowProcessingState.getSession()
										);
										persistenceContext.getBatchFetchQueue().addBatchLoadableEntityKey( entityKey );
										persistenceContext.addProxy( entityKey, entityInstance );
									}
									else if ( concreteDescriptor.getBytecodeEnhancementMetadata()
											.isEnhancedForLazyLoading() && !concreteDescriptor.isAbstract() ) {
										entityInstance = concreteDescriptor.getBytecodeEnhancementMetadata()
												.createEnhancedProxy(
														entityKey,
														true,
														rowProcessingState.getSession()
												);
									}else {
										entityInstance = rowProcessingState.getSession()
												.internalLoad(
														concreteDescriptor.getEntityName(),
														identifier,
														false,
														referencedModelPart.isInternalLoadNullable()
//													!referencedModelPart.isConstrained() || referencedModelPart.isIgnoreNotFound()
												);
									}
								}
							}
						}
					}
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
		return referencedModelPart.getEntityMappingType().getEntityPersister();
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

	@Override
	public EntityPersister getConcreteDescriptor() {
		return getEntityDescriptor();
	}

	@Override
	public String toString() {
		return "EntityDelayedFetchInitializer(" + LoggingHelper.toLoggableString( navigablePath ) + ")";
	}

}
