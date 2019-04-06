/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.entity;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.domain.spi.EntityMappingImplementor;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.ValueInclusion;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractEntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;

/**
 * @author Andrea Boriero
 */
public class UnionSubclassEntityDecriptor extends AbstractEntityTypeDescriptor {

	public UnionSubclassEntityDecriptor(
			EntityMappingImplementor bootMapping,
			IdentifiableTypeDescriptor superTypeDescriptor,
			RuntimeModelCreationContext creationContext)
			throws HibernateException {
		super( bootMapping, superTypeDescriptor, creationContext );
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
	public String getIdentifierPropertyName() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean isCacheInvalidationRequired() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean isLazyPropertiesCacheable() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public CacheEntryStructure getCacheEntryStructure() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public CacheEntry buildCacheEntry(
			Object entity, Object[] state, Object version, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Serializable getIdByUniqueKey(
			Serializable key, String uniquePropertyName, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Object getCurrentVersion(Object id, SharedSessionContractImplementor session) throws HibernateException {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Object forceVersionIncrement(
			Object id, Object currentVersion, SharedSessionContractImplementor session) throws HibernateException {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean isInstrumented() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Boolean isTransient(Object object, SharedSessionContractImplementor session) throws HibernateException {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Object[] getPropertyValuesToInsert(
			Object object, Map mergeMap, SharedSessionContractImplementor session) throws HibernateException {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void processInsertGeneratedProperties(
			Object id, Object entity, Object[] state, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );

	}

	@Override
	public void processUpdateGeneratedProperties(
			Object id, Object entity, Object[] state, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );

	}

	@Override
	public Class getMappedClass() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean implementsLifecycle() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public int[] resolveAttributeIndexes(String[] attributeNames) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean canUseReferenceCacheEntries() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void registerAffectingFetchProfile(String fetchProfileName) {
		throw new NotYetImplementedFor6Exception( getClass() );

	}

	@Override
	public boolean hasCollections() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public ValueInclusion[] getPropertyInsertGenerationInclusions() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public ValueInclusion[] getPropertyUpdateGenerationInclusions() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean[] getPropertyUpdateability() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean[] getPropertyVersionability() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public boolean[] getPropertyLaziness() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public CascadeStyle[] getPropertyCascadeStyles() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public String asLoggableText() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
