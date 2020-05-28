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
import org.hibernate.engine.spi.EntityUniqueKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AbstractFetchParentAccess;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityLoadingLogger;
import org.hibernate.sql.results.graph.entity.LoadingEntityEntry;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import static org.hibernate.internal.log.LoggingHelper.toLoggableString;

/**
 * @author Andrea Boriero
 */
public class BiDirectionalEntityInitializer extends AbstractFetchParentAccess implements EntityInitializer {

	private final NavigablePath navigablePath;
	private final EntityPersister concreteDescriptor;
	private final DomainResultAssembler identifierAssembler;
	private final boolean isEnhancedForLazyLoading;
	private final boolean nullable;
	private String uniqueKeyPropertyName;

	private Object entityInstance;

	public BiDirectionalEntityInitializer(
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResultAssembler identifierAssembler,
			boolean nullable,
			String uniqueKeyPropertyName) {
		this.navigablePath = fetchedNavigable;
		this.concreteDescriptor = concreteDescriptor;
		this.identifierAssembler = identifierAssembler;
		this.isEnhancedForLazyLoading = concreteDescriptor.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();
		this.nullable = nullable;
		this.uniqueKeyPropertyName = uniqueKeyPropertyName;
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

		final Object entityIdentifier = identifierAssembler.assemble( rowProcessingState );
		if ( entityIdentifier == null ) {
			return;
		}

		final SharedSessionContractImplementor session = rowProcessingState
				.getJdbcValuesSourceProcessingState()
				.getSession();
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final String entityName = concreteDescriptor.getEntityName();

		if ( uniqueKeyPropertyName !=null ) {
			EntityUniqueKey euk = new EntityUniqueKey(
					entityName,
					uniqueKeyPropertyName,
					entityIdentifier,
					null,
					concreteDescriptor.getRepresentationStrategy().getMode().getLegacyEntityMode(),
					session.getFactory()
			);
			Object result = persistenceContext.getEntity( euk );
			if ( result == null ) {
				result = ((UniqueKeyLoadable)concreteDescriptor).loadByUniqueKey( uniqueKeyPropertyName, entityIdentifier, session );

				// If the entity was not in the Persistence Context, but was found now,
				// add it to the Persistence Context
				if (result != null) {
					persistenceContext.addEntity(euk, result);
				}
			}
			return;
		}

		EntityKey entityKey = new EntityKey( entityIdentifier, concreteDescriptor );
		final Object existingEntity = persistenceContext.getEntity( entityKey );

		if ( existingEntity != null ) {
			entityInstance = existingEntity;
			return;
		}

		// look to see if another initializer from a parent load context or an earlier
		// initializer is already loading the entity

		final LoadingEntityEntry existingLoadingEntry = persistenceContext
				.getLoadContexts()
				.findLoadingEntityEntry( entityKey );

		if ( existingLoadingEntry != null ) {
			if ( EntityLoadingLogger.DEBUG_ENABLED ) {
				EntityLoadingLogger.INSTANCE.debugf(
						"(%s) Found existing loading entry [%s] - using loading instance",
						StringHelper.collapse( this.getClass().getName() ),
						toLoggableString( getNavigablePath(), entityIdentifier )
				);
			}

			this.entityInstance = existingLoadingEntry.getEntityInstance();

			if ( existingLoadingEntry.getEntityInitializer() != this ) {
				// the entity is already being loaded elsewhere
				if ( EntityLoadingLogger.DEBUG_ENABLED ) {
					EntityLoadingLogger.INSTANCE.debugf(
							"(%s) Entity [%s] being loaded by another initializer [%s] - skipping processing",
							StringHelper.collapse( this.getClass().getName() ),
							toLoggableString( getNavigablePath(), entityIdentifier ),
							existingLoadingEntry.getEntityInitializer()
					);
				}

				// EARLY EXIT!!!
				return;
			}
		}

		if ( entityInstance == null ) {
			// see if it is managed in the Session already
			final Object entity = persistenceContext.getEntity( entityKey );
			if ( entity != null ) {
				this.entityInstance = entity;
			}
		}

		entityInstance = session.internalLoad(
				entityName,
				entityIdentifier,
				true,
				nullable
		);

		if ( entityInstance instanceof HibernateProxy && isEnhancedForLazyLoading ) {
			( (HibernateProxy) entityInstance ).getHibernateLazyInitializer().setUnwrap( true );
		}
	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
		// nothing to do
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		entityInstance = null;
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
