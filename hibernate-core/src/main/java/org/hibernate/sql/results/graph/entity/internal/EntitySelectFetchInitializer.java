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
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.AbstractFetchParentAccess;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.graph.entity.EntityLoadingLogger;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;
import org.hibernate.sql.results.graph.entity.LoadingEntityEntry;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import static org.hibernate.internal.log.LoggingHelper.toLoggableString;

/**
 * @author Andrea Boriero
 */
public class EntitySelectFetchInitializer extends AbstractFetchParentAccess implements EntityInitializer {
	private static final String CONCRETE_NAME = EntitySelectFetchInitializer.class.getSimpleName();

	private final ToOneAttributeMapping fetchedAttribute;
	private final NavigablePath navigablePath;
	private final EntityPersister concreteDescriptor;
	private final DomainResultAssembler identifierAssembler;
	private final boolean isEnhancedForLazyLoading;
	private boolean loadByUniqueKey;
	private final boolean nullable;

	private Object entityInstance;

	protected EntitySelectFetchInitializer(
			EntityValuedFetchable fetchedAttribute,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResultAssembler identifierAssembler,
			boolean loadByUniqueKey,
			boolean nullable) {
		this.fetchedAttribute = (ToOneAttributeMapping) fetchedAttribute;
		this.navigablePath = fetchedNavigable;
		this.concreteDescriptor = concreteDescriptor;
		this.identifierAssembler = identifierAssembler;
		this.loadByUniqueKey = loadByUniqueKey;
		this.nullable = nullable;
		this.isEnhancedForLazyLoading = concreteDescriptor.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading();
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

	}

	@Override
	public void initializeInstance(RowProcessingState rowProcessingState) {
		if ( entityInstance != null ) {
			return;
		}

		final Object id = identifierAssembler.assemble( rowProcessingState );
		if ( id == null ) {
			return;
		}
		final SharedSessionContractImplementor session = rowProcessingState.getSession();
		final String entityName = concreteDescriptor.getEntityName();

		if ( loadByUniqueKey ) {
			String uniqueKeyPropertyName = fetchedAttribute.getMappedBy();
			EntityUniqueKey euk = new EntityUniqueKey(
					entityName,
					uniqueKeyPropertyName,
					id,
					concreteDescriptor.getIdentifierType(),
					concreteDescriptor.getEntityMode(),
					session.getFactory()
			);
			final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
			entityInstance = persistenceContext.getEntity( euk );
			if ( entityInstance == null ) {
				entityInstance = ( (UniqueKeyLoadable) concreteDescriptor ).loadByUniqueKey(
						uniqueKeyPropertyName,
						id,
						session
				);

				// If the entity was not in the Persistence Context, but was found now,
				// add it to the Persistence Context
				if ( entityInstance != null ) {
					persistenceContext.addEntity( euk, entityInstance );
				}
			}
		}
		else {
			final EntityKey entityKey = new EntityKey( id, concreteDescriptor );
			final LoadingEntityEntry existingLoadingEntry = session
					.getPersistenceContext()
					.getLoadContexts()
					.findLoadingEntityEntry( entityKey );

			if ( existingLoadingEntry != null ) {
				if ( EntityLoadingLogger.DEBUG_ENABLED ) {
					EntityLoadingLogger.LOGGER.debugf(
							"(%s) Found existing loading entry [%s] - using loading instance",
							CONCRETE_NAME,
							toLoggableString( getNavigablePath(), id )
					);
				}
				this.entityInstance = existingLoadingEntry.getEntityInstance();

				if ( existingLoadingEntry.getEntityInitializer() != this ) {
					// the entity is already being loaded elsewhere
					if ( EntityLoadingLogger.DEBUG_ENABLED ) {
						EntityLoadingLogger.LOGGER.debugf(
								"(%s) Entity [%s] being loaded by another initializer [%s] - skipping processing",
								CONCRETE_NAME,
								toLoggableString( getNavigablePath(), id ),
								existingLoadingEntry.getEntityInitializer()
						);
					}

					// EARLY EXIT!!!
					return;
				}
			}

			entityInstance = session.internalLoad(
					entityName,
					id,
					true,
					nullable
			);
		}

		if ( entityInstance instanceof HibernateProxy && isEnhancedForLazyLoading ) {
			( (HibernateProxy) entityInstance ).getHibernateLazyInitializer().setUnwrap( true );
		}
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
