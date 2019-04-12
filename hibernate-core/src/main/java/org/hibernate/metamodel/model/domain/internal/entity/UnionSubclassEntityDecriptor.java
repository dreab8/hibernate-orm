/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.boot.model.domain.ManagedTypeMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.domain.spi.EntityMappingImplementor;
import org.hibernate.boot.model.domain.spi.ManagedTypeMappingImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractEntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NonIdPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.metamodel.model.relational.spi.Table;

/**
 * @author Andrea Boriero
 */
public class UnionSubclassEntityDecriptor<J> extends AbstractEntityTypeDescriptor<J> {

	private EntityIdentifier identifierDescriptor;

	public UnionSubclassEntityDecriptor(
			EntityMappingImplementor bootMapping,
			IdentifiableTypeDescriptor superTypeDescriptor,
			RuntimeModelCreationContext creationContext)
			throws HibernateException {
		super( bootMapping, superTypeDescriptor, creationContext );
		identifierDescriptor = bootMapping.makeRuntimeIdentifierDescriptor(
				getHierarchy(),
				this,
				creationContext
		);
	}

	@Override
	public void afterInitialize(Object entity, SharedSessionContractImplementor session) {
		super.afterInitialize( entity, session );
	}

	@Override
	public EntityIdentifier getIdentifierDescriptor() {
		return identifierDescriptor;
	}

	@Override
	public void delete(
			Object id, Object version, Object object, SharedSessionContractImplementor session)
			throws HibernateException {
		throw new NotYetImplementedFor6Exception( getClass() );

	}

	@Override
	public void update(
			Object id,
			Object[] fields,
			int[] dirtyFields,
			boolean hasDirtyCollection,
			Object[] oldFields,
			Object oldVersion,
			Object object,
			Object rowId,
			SharedSessionContractImplementor session) throws HibernateException {
		throw new NotYetImplementedFor6Exception( getClass() );

	}

	@Override
	public Serializable getIdByUniqueKey(
			Serializable key, String uniquePropertyName, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public String asLoggableText() {
		return String.format( "UnionSubclassEntityDecriptor<%s>", getEntityName() );
	}

	@Override
	protected Table resolveRootTable(EntityMapping entityMapping, RuntimeModelCreationContext creationContext) {
		return resolveTable( entityMapping.getMappedTable(), creationContext );
	}

	@Override
	protected void initializeAttributes(
			ManagedTypeMappingImplementor bootDescriptor,
			RuntimeModelCreationContext creationContext) {
		List<PersistentAttributeMapping> persistentAttributes = bootDescriptor.getPersistentAttributes();
		final int attributeCount = persistentAttributes.size();

		declaredAttributes = CollectionHelper.arrayList( attributeCount );
		declaredAttributesByName = CollectionHelper.concurrentMap( attributeCount );
		// NOTE : we can't know the size of declared contributors up front
		stateArrayContributors = new ArrayList<>();

		attributes = CollectionHelper.arrayList( attributeCount );

		createAttributes( bootDescriptor, creationContext, persistentAttributes );

		if ( bootDescriptor instanceof IdentifiableTypeMapping ) {
			addJoinDeclaredAttributes( (IdentifiableTypeMapping) bootDescriptor, creationContext );
		}

		inFlightAccess.finishUp();
	}

	protected void createAttributes(
			ManagedTypeMapping bootContainer,
			RuntimeModelCreationContext creationContext,
			List<PersistentAttributeMapping> attributes) {
		attributes.forEach(
				attributeMapping -> {
					createAttribute(
							attributeMapping,
							bootContainer,
							creationContext
					);
				}
		);
	}

	private void createAttribute(
			PersistentAttributeMapping attributeMapping,
			ManagedTypeMapping bootContainer,
			RuntimeModelCreationContext creationContext) {
		PersistentAttributeDescriptor<J, Object> attribute = attributeMapping.makeRuntimeAttribute(
				this,
				bootContainer,
				SingularPersistentAttribute.Disposition.NORMAL,
				creationContext
		);
		if ( !NonIdPersistentAttribute.class.isInstance( attribute ) ) {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Boot-time attribute descriptor [%s] made non-NonIdPersistentAttribute, " +
									"while a NonIdPersistentAttribute was expected : %s",
							attributeMapping,
							attribute
					)
			);
		}

		getInFlightAccess().addAttribute( attribute );
	}
}
