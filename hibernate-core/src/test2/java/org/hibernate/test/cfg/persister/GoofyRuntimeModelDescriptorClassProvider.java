/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cfg.persister;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.boot.model.domain.spi.IdentifiableTypeMappingImplementor;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.cache.spi.entry.CacheEntry;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.MutableEntityEntryFactory;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.EntityEntryFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.ValueInclusion;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.loader.spi.CollectionLoader;
import org.hibernate.loader.spi.MultiLoadOptions;
import org.hibernate.loader.spi.SingleIdEntityLoader;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelDescriptorClassResolver;
import org.hibernate.metamodel.model.domain.spi.AbstractEntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.AbstractPersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.CollectionIdentifier;
import org.hibernate.metamodel.model.domain.spi.CollectionIndex;
import org.hibernate.metamodel.model.domain.spi.CollectionKey;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.JoinedTableBinding;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.sql.ast.produce.metamodel.spi.NavigableReferenceInfo;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupResolver;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.Type;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class GoofyRuntimeModelDescriptorClassProvider implements RuntimeModelDescriptorClassResolver {
	@Override
	public Class<? extends EntityDescriptor> getEntityPersisterClass(PersistentClass metadata) {
		return NoopEntityPersister.class;
	}

	@Override
	public Class<? extends PersistentCollectionDescriptor> getCollectionDescriptorClass(Collection bootMapping) {
		return NoopCollectionPersister.class;
	}

	public static class NoopEntityPersister extends AbstractEntityDescriptor implements EntityDescriptor {
		public NoopEntityPersister(
				EntityMapping entityMapping,
				EntityDataAccess cacheAccessStrategy,
				NaturalIdDataAccess naturalIdRegionAccessStrategy,
				RuntimeModelCreationContext creationContext) throws HibernateException {
			super( entityMapping, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext );
			throw new GoofyException(NoopEntityPersister.class);
		}

		@Override
		public EntityMode getEntityMode() {
			return null;
		}

		@Override
		public void finishInstantiation(
				EntityHierarchy entityHierarchy,
				IdentifiableTypeDescriptor superType,
				IdentifiableTypeMapping bootMapping,
				RuntimeModelCreationContext creationContext) {

		}

		@Override
		public void completeInitialization(
				EntityHierarchy entityHierarchy,
				IdentifiableTypeDescriptor superType,
				IdentifiableTypeMappingImplementor bootMapping,
				RuntimeModelCreationContext creationContext) {

		}

		@Override
		public void postInstantiate() throws MappingException {

		}

		@Override
		public SessionFactoryImplementor getFactory() {
			return null;
		}

		@Override
		public EntityIdentifier getIdentifierDescriptor() {
			return null;
		}

		@Override
		public EntityEntryFactory getEntityEntryFactory() {
			return MutableEntityEntryFactory.INSTANCE;
		}

		@Override
		public Table getPrimaryTable() {
			return null;
		}

		@Override
		public List<JoinedTableBinding> getSecondaryTableBindings() {
			return null;
		}

		@Override
		public String getRootEntityName() {
			return null;
		}

		@Override
		public String getEntityName() {
			return null;
		}

		@Override
		public EntityMetamodel getEntityMetamodel() {
			return null;
		}

		@Override
		public boolean isSubclassEntityName(String entityName) {
			return false;
		}

		@Override
		public Serializable[] getPropertySpaces() {
			return new Serializable[0];
		}

		@Override
		public Serializable[] getQuerySpaces() {
			return new Serializable[0];
		}

		@Override
		public boolean hasProxy() {
			return false;
		}

		@Override
		public boolean hasCollections() {
			return false;
		}

		@Override
		public boolean hasMutableProperties() {
			return false;
		}

		@Override
		public boolean hasSubselectLoadableCollections() {
			return false;
		}

		@Override
		public boolean hasCascades() {
			return false;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public boolean isInherited() {
			return false;
		}

		@Override
		public boolean isIdentifierAssignedByInsert() {
			return false;
		}

		@Override
		public Type getPropertyType(String propertyName) throws MappingException {
			return null;
		}

		@Override
		public int[] findDirty(Object[] currentState, Object[] previousState, Object owner, SharedSessionContractImplementor session) {
			return new int[0];
		}

		@Override
		public int[] findModified(Object[] old, Object[] current, Object object, SharedSessionContractImplementor session) {
			return new int[0];
		}

		@Override
		public boolean hasIdentifierProperty() {
			return false;
		}

		@Override
		public boolean canExtractIdOutOfEntity() {
			return false;
		}

		@Override
		public boolean isVersioned() {
			return false;
		}

		@Override
		public Comparator getVersionComparator() {
			return null;
		}

		@Override
		public Type getVersionType() {
			return null;
		}

		@Override
		public int getVersionProperty() {
			return 0;
		}

		@Override
		public boolean hasNaturalIdentifier() {
			return false;
		}

		@Override
		public int[] getNaturalIdentifierProperties() {
			return new int[0];
		}

		@Override
		public Object[] getNaturalIdentifierSnapshot(Serializable id, SharedSessionContractImplementor session) {
			return new Object[0];
		}

		@Override
		public Serializable loadEntityIdByNaturalId(
				Object[] naturalIdValues, LockOptions lockOptions, SharedSessionContractImplementor session) {
			return null;
		}

		@Override
		public IdentifierGenerator getIdentifierGenerator() {
			return null;
		}

		@Override
		public boolean hasLazyProperties() {
			return false;
		}

		@Override
		public Object load(Serializable id, Object optionalObject, LockMode lockMode, SharedSessionContractImplementor session) {
			return null;
		}

		@Override
		public Object load(Serializable id, Object optionalObject, LockOptions lockOptions, SharedSessionContractImplementor session) {
			return null;
		}

		@Override
		public List multiLoad(
				Object[] ids,
				MultiLoadOptions loadOptions,
				SharedSessionContractImplementor session) {
			return Collections.emptyList();
		}

		@Override
		public void lock(Serializable id, Object version, Object object, LockMode lockMode, SharedSessionContractImplementor session) {
		}

		@Override
		public void lock(Serializable id, Object version, Object object, LockOptions lockOptions, SharedSessionContractImplementor session) {
		}

		@Override
		public void insert(Serializable id, Object[] fields, Object object, SharedSessionContractImplementor session) {
		}

		@Override
		public Serializable insert(Object[] fields, Object object, SharedSessionContractImplementor session) {
			return null;
		}

		@Override
		public void delete(Serializable id, Object version, Object object, SharedSessionContractImplementor session) {
		}

		@Override
		public void update(
				Serializable id,
				Object[] fields,
				int[] dirtyFields,
				boolean hasDirtyCollection,
				Object[] oldFields,
				Object oldVersion,
				Object object,
				Object rowId,
				SharedSessionContractImplementor session) {
		}

		@Override
		public Type[] getPropertyTypes() {
			return new Type[0];
		}

		@Override
		public String[] getPropertyNames() {
			return new String[0];
		}

		@Override
		public boolean[] getPropertyInsertability() {
			return new boolean[0];
		}

		@Override
		public ValueInclusion[] getPropertyInsertGenerationInclusions() {
			return new ValueInclusion[0];
		}

		@Override
		public ValueInclusion[] getPropertyUpdateGenerationInclusions() {
			return new ValueInclusion[0];
		}

		@Override
		public boolean[] getPropertyUpdateability() {
			return new boolean[0];
		}

		@Override
		public boolean[] getPropertyCheckability() {
			return new boolean[0];
		}

		@Override
		public boolean[] getPropertyNullability() {
			return new boolean[0];
		}

		@Override
		public boolean[] getPropertyVersionability() {
			return new boolean[0];
		}

		@Override
		public boolean[] getPropertyLaziness() {
			return new boolean[0];
		}

		@Override
		public CascadeStyle[] getPropertyCascadeStyles() {
			return new CascadeStyle[0];
		}

		@Override
		public Type getIdentifierType() {
			return null;
		}

		@Override
		public String getIdentifierPropertyName() {
			return null;
		}

		@Override
		public boolean isCacheInvalidationRequired() {
			return false;
		}

		@Override
		public boolean isLazyPropertiesCacheable() {
			return false;
		}

		@Override
		public boolean hasCache() {
			return false;
		}

		@Override
		public EntityDataAccess getCacheAccessStrategy() {
			return null;
		}
		
		@Override
		public boolean hasNaturalIdCache() {
			return false;
		}

		@Override
		public NaturalIdDataAccess getNaturalIdCacheAccessStrategy() {
			return null;
		}

		@Override
		public CacheEntryStructure getCacheEntryStructure() {
			return null;
		}

		@Override
		public CacheEntry buildCacheEntry(
				Object entity, Object[] state, Object version, SharedSessionContractImplementor session) {
			return null;
		}

		@Override
		public ClassMetadata getClassMetadata() {
			return null;
		}

		@Override
		public boolean isBatchLoadable() {
			return false;
		}

		@Override
		public boolean isSelectBeforeUpdateRequired() {
			return false;
		}

		@Override
		public Object[] getDatabaseSnapshot(Serializable id, SharedSessionContractImplementor session) throws HibernateException {
			return new Object[0];
		}

		@Override
		public Serializable getIdByUniqueKey(Serializable key, String uniquePropertyName, SharedSessionContractImplementor session) {
			throw new UnsupportedOperationException( "not supported" );
		}

		@Override
		public Object getCurrentVersion(Serializable id, SharedSessionContractImplementor session) throws HibernateException {
			return null;
		}

		@Override
		public Object forceVersionIncrement(Serializable id, Object currentVersion, SharedSessionContractImplementor session) {
			return null;
		}

		@Override
		public boolean isInstrumented() {
			return false;
		}

		@Override
		public boolean hasInsertGeneratedProperties() {
			return false;
		}

		@Override
		public boolean hasUpdateGeneratedProperties() {
			return false;
		}

		@Override
		public boolean isVersionPropertyGenerated() {
			return false;
		}

		@Override
		public void afterInitialize(Object entity, SharedSessionContractImplementor session) {

		}

		@Override
		public void afterReassociate(Object entity, SharedSessionContractImplementor session) {
		}

		@Override
		public Object createProxy(Serializable id, SharedSessionContractImplementor session) throws HibernateException {
			return null;
		}

		@Override
		public Boolean isTransient(Object object, SharedSessionContractImplementor session) throws HibernateException {
			return null;
		}

		@Override
		public Object[] getPropertyValuesToInsert(Object object, Map mergeMap, SharedSessionContractImplementor session) {
			return new Object[0];
		}

		@Override
		public void processInsertGeneratedProperties(Serializable id, Object entity, Object[] state, SharedSessionContractImplementor session) {
		}

		@Override
		public void processUpdateGeneratedProperties(Serializable id, Object entity, Object[] state, SharedSessionContractImplementor session) {
		}

		@Override
		public Class getMappedClass() {
			return null;
		}

		@Override
		public boolean implementsLifecycle() {
			return false;
		}

		@Override
		public Class getConcreteProxyClass() {
			return null;
		}

		@Override
		public void setPropertyValues(Object object, Object[] values) {
		}

		@Override
		public void setPropertyValue(Object object, int i, Object value) {
		}

		@Override
		public Object[] getPropertyValues(Object object) {
			return new Object[0];
		}

		@Override
		public Object getPropertyValue(Object object, int i) {
			return null;
		}

		@Override
		public Object getPropertyValue(Object object, String propertyName) {
			return null;
		}

		@Override
		public Serializable getIdentifier(Object object) {
			return null;
		}

		@Override
		public Serializable getIdentifier(Object entity, SharedSessionContractImplementor session) {
			return null;
		}

		@Override
		public void setIdentifier(Object entity, Serializable id, SharedSessionContractImplementor session) {
		}

		@Override
		public Object getVersion(Object object) {
			return null;
		}

		@Override
		public Object instantiate(Serializable id, SharedSessionContractImplementor session) {
			return null;
		}

		@Override
		public boolean isInstance(Object object) {
			return false;
		}

		@Override
		public boolean hasUninitializedLazyProperties(Object object) {
			return false;
		}

		@Override
		public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion, SharedSessionContractImplementor session) {
		}

		@Override
		public EntityDescriptor getSubclassEntityPersister(Object instance, SessionFactoryImplementor factory) {
			return null;
		}

		@Override
		public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public EntityDescriptor getEntityDescriptor() {
			return this;
		}

		@Override
		protected SingleIdEntityLoader createLoader(
				LockOptions lockOptions, SharedSessionContractImplementor session) {
			return null;
		}

		@Override
        public int[] resolveAttributeIndexes(String[] attributeNames) {
            return null;
        }

		@Override
		public boolean canUseReferenceCacheEntries() {
			return false;
		}

		@Override
		public TableGroup resolveTableGroup(
				NavigableReferenceInfo embeddedReferenceInfo, TableGroupResolver tableGroupResolver) {
			return null;
		}

		@Override
		public String asLoggableText() {
			return null;
		}
	}

	public static class NoopCollectionPersister extends AbstractPersistentCollectionDescriptor
			implements PersistentCollectionDescriptor {
		public NoopCollectionPersister(
				Collection collectionBinding,
				ManagedTypeDescriptor source,
				String navigableName,
				CollectionDataAccess cacheAccessStrategy,
				RuntimeModelCreationContext creationContext) throws MappingException, CacheException {
			super( collectionBinding, source, navigableName, cacheAccessStrategy, creationContext );
			throw new GoofyException(NoopCollectionPersister.class);
		}

		@Override
		protected Table resolveCollectionTable(
				Collection collectionBinding, RuntimeModelCreationContext creationContext) {
			return null;
		}

		public void initialize(Serializable key, SharedSessionContractImplementor session) throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public CollectionClassification getCollectionClassification() {
			return null;
		}

		@Override
		public NavigableRole getNavigableRole() {
			return null;
		}

		@Override
		public CollectionKey getForeignKeyDescriptor() {
			return null;
		}

		@Override
		public CollectionIdentifier getIdDescriptor() {
			return null;
		}

		@Override
		public CollectionElement getElementDescriptor() {
			return null;
		}

		@Override
		public CollectionIndex getIndexDescriptor() {
			return null;
		}

		@Override
		public CollectionLoader getLoader() {
			return null;
		}

		@Override
		public Table getSeparateCollectionTable() {
			return null;
		}

		public boolean hasCache() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public CollectionDataAccess getCacheAccessStrategy() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public CacheEntryStructure getCacheEntryStructure() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Type getKeyType() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Type getIndexType() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Type getElementType() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Class getElementClass() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Object readKey(ResultSet rs, String[] keyAliases, SharedSessionContractImplementor session)
				throws HibernateException, SQLException {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Object readElement(ResultSet rs, Object owner, String[] columnAliases, SharedSessionContractImplementor session)
				throws HibernateException, SQLException {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Object readIndex(ResultSet rs, String[] columnAliases, SharedSessionContractImplementor session)
				throws HibernateException, SQLException {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Object readIdentifier(ResultSet rs, String columnAlias, SharedSessionContractImplementor session)
				throws HibernateException, SQLException {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isPrimitiveArray() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isArray() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isOneToMany() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isManyToMany() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public String getManyToManyFilterFragment(String alias, Map enabledFilters) {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean hasIndex() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isLazy() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isInverse() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public void remove(Serializable id, SharedSessionContractImplementor session) throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public void recreate(PersistentCollection collection, Serializable key, SharedSessionContractImplementor session)
				throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public void deleteRows(PersistentCollection collection, Serializable key, SharedSessionContractImplementor session)
				throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public void updateRows(PersistentCollection collection, Serializable key, SharedSessionContractImplementor session)
				throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public void insertRows(PersistentCollection collection, Serializable key, SharedSessionContractImplementor session)
				throws HibernateException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public String getRole() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public EntityDescriptor getOwnerEntityPersister() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public IdentifierGenerator getIdentifierGenerator() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Type getIdentifierType() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean hasOrphanDelete() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean hasOrdering() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean hasManyToManyOrdering() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Serializable[] getCollectionSpaces() {
			return new Serializable[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isCascadeDeleteEnabled() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isVersioned() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isMutable() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public void postInstantiate() throws MappingException {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		public SessionFactoryImplementor getFactory() {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isAffectedByEnabledFilters(SharedSessionContractImplementor session) {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public String[] getKeyColumnAliases(String suffix) {
			return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public String[] getIndexColumnAliases(String suffix) {
			return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public String[] getElementColumnAliases(String suffix) {
			return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
		}

		public String getIdentifierColumnAlias(String suffix) {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public boolean isExtraLazy() {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public int getSize(Serializable key, SharedSessionContractImplementor session) {
			return 0;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Boolean indexExists(Serializable key, Object index, SharedSessionContractImplementor session) {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Boolean elementExists(Serializable key, Object element, SharedSessionContractImplementor session) {
			return false;  //To change body of implemented methods use File | Settings | File Templates.
		}

		public Object getElementByIndex(Serializable key, Object index, SharedSessionContractImplementor session, Object owner) {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public void visitNavigables(NavigableVisitationStrategy visitor) {

		}

		@Override
		public ManagedTypeDescriptor getContainer() {
			return null;
		}

		@Override
		public JavaTypeDescriptor getJavaTypeDescriptor() {
			return null;
		}

		@Override
		public String asLoggableText() {
			return null;
		}

		@Override
		public void visitNavigable(NavigableVisitationStrategy visitor) {

		}

		@Override
		public QueryResult generateQueryResult(
				NavigableReference selectedExpression,
				String resultVariable,
				SqlExpressionResolver sqlSelectionResolver,
				QueryResultCreationContext creationContext) {
			return null;
		}

		@Override
		public PersistenceType getPersistenceType() {
			return null;
		}

		@Override
		public Class getJavaType() {
			return null;
		}
	}
}
