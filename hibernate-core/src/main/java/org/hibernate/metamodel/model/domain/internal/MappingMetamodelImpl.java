/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;

import org.hibernate.EntityNameResolver;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.UnknownEntityTypeException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.EntityManagerMessageLogger;
import org.hibernate.internal.HEMLogging;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.MetamodelUnsupportedOperationException;
import org.hibernate.metamodel.internal.JpaStaticMetaModelPopulationSetting;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.TupleType;
import org.hibernate.metamodel.spi.EntityRepresentationStrategy;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.persister.spi.PersisterFactory;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmFieldLiteral;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.metamodel.internal.JpaStaticMetaModelPopulationSetting.determineJpaMetaModelPopulationSetting;

/**
 * Hibernate implementation of the JPA {@link jakarta.persistence.metamodel.Metamodel} contract.
 *
 * Really more of the mapping model then the domain model, though it does have reference to the `JpaMetamodel`
 *
 * NOTE : we suppress deprecation warnings because at the moment we still implement a deprecated API so
 * have to reference deprecated things
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 * @author Andrea Boriero
 */
@SuppressWarnings("deprecation")
public class MappingMetamodelImpl implements MappingMetamodel, MetamodelImplementor, Serializable {
	// todo : Integrate EntityManagerLogger into CoreMessageLogger
	private static final EntityManagerMessageLogger log = HEMLogging.messageLogger( MappingMetamodelImpl.class );

	private static final String[] EMPTY_IMPLEMENTORS = ArrayHelper.EMPTY_STRING_ARRAY;

	private final SessionFactoryImplementor sessionFactory;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JpaMetamodel

	private final JpaMetamodel jpaMetamodel;

	private final Map<Class, String> entityProxyInterfaceMap = new ConcurrentHashMap<>();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// RuntimeModel

	private final Map<String, EntityPersister> entityPersisterMap = new ConcurrentHashMap<>();
	private final Map<String, CollectionPersister> collectionPersisterMap = new ConcurrentHashMap<>();
	private final Map<String, Set<String>> collectionRolesByEntityParticipant = new ConcurrentHashMap<>();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainMetamodel

	private final Set<EntityNameResolver> entityNameResolvers = new HashSet<>();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// NOTE : Relational/mapping information is not part of the JPA metamodel
	// (type system).  However, this relational/mapping info *is* part of the
	// Hibernate metamodel.  This is a mismatch.  Normally this is not a
	// problem - ignoring Hibernate's representation mode (entity mode),
	// an entity (or mapped superclass) *Class* always refers to the same
	// EntityType (JPA) and EntityPersister (Hibernate)..  The problem is
	// in regards to embeddables.  For an embeddable, as with the rest of its
	// metamodel, Hibernate combines the embeddable's relational/mapping
	// while JPA does not.  This is consistent with each's model paradigm.
	// However, it causes a mismatch in that while JPA expects a single
	// "type descriptor" for a given embeddable class, Hibernate incorporates
	// the relational/mapping info so we have a "type descriptor" for each
	// usage of that embeddable.  Think embeddable versus embedded.
	//
	// To account for this, we track both paradigms here...

	/*
	 * There can be multiple instances of an Embeddable type, each one being relative to its parent entity.
	 */

	/**
	 * That's not strictly correct in the JPA standard since for a given Java type we could have
	 * multiple instances of an embeddable type. Some embeddable might override attributes, but we
	 * can only return a single EmbeddableTypeImpl for a given Java object class.
	 * <p>
	 * A better approach would be if the parent class and attribute name would be included as well
	 * when trying to locate the embeddable type.
	 */
//	private final Map<Class<?>, EmbeddableDomainType<?>> jpaEmbeddableTypeMap = new ConcurrentHashMap<>();
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private final TypeConfiguration typeConfiguration;

	private final Map<String, String[]> implementorsCache = new ConcurrentHashMap<>();
	private final Map<TupleType<?>, MappingModelExpressable<?>> tupleTypeCache = new ConcurrentHashMap<>();

	public MappingMetamodelImpl(SessionFactoryImplementor sessionFactory, TypeConfiguration typeConfiguration) {
		this.sessionFactory = sessionFactory;
		this.typeConfiguration = typeConfiguration;
		this.jpaMetamodel = new JpaMetamodelImpl( typeConfiguration, sessionFactory.getSessionFactoryOptions().getJpaCompliance() );
	}

	public JpaMetamodel getJpaMetamodel() {
		return jpaMetamodel;
	}

	public void finishInitialization(
			MetadataImplementor bootModel,
			BootstrapContext bootstrapContext,
			SessionFactoryImplementor sessionFactory) {
		final RuntimeModelCreationContext runtimeModelCreationContext = new RuntimeModelCreationContext() {
			@Override
			public BootstrapContext getBootstrapContext() {
				return bootstrapContext;
			}

			@Override
			public SessionFactoryImplementor getSessionFactory() {
				return sessionFactory;
			}

			@Override
			public MetadataImplementor getBootModel() {
				return bootModel;
			}

			@Override
			public MappingMetamodel getDomainModel() {
				return MappingMetamodelImpl.this;
			}
		};

		final PersisterFactory persisterFactory = sessionFactory.getServiceRegistry().getService( PersisterFactory.class );

		final JpaStaticMetaModelPopulationSetting jpaStaticMetaModelPopulationSetting = determineJpaMetaModelPopulationSetting( sessionFactory.getProperties() );

		bootModel.visitRegisteredComponents( Component::prepareForMappingModel );
		bootModel.getMappedSuperclassMappingsCopy().forEach( MappedSuperclass::prepareForMappingModel );
		bootModel.getEntityBindings().forEach( PersistentClass::prepareForMappingModel );

		processBootEntities(
				bootModel.getEntityBindings(),
				sessionFactory.getCache(),
				persisterFactory,
				runtimeModelCreationContext
		);

		processBootCollections(
				bootModel.getCollectionBindings(),
				sessionFactory.getCache(),
				persisterFactory,
				runtimeModelCreationContext
		);


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// after *all* persisters and named queries are registered

		MappingModelCreationProcess.process(
				entityPersisterMap,
				sessionFactory.getQueryEngine().getSqmFunctionRegistry(),
				runtimeModelCreationContext
		);

		for ( EntityPersister persister : entityPersisterMap.values() ) {
			persister.postInstantiate();
			registerEntityNameResolvers( persister, entityNameResolvers );
		}

		collectionPersisterMap.values().forEach( CollectionPersister::postInstantiate );


		( (JpaMetamodelImpl) this.jpaMetamodel ).processJpa(
				bootModel,
				this,
				entityProxyInterfaceMap,
				jpaStaticMetaModelPopulationSetting,
				bootModel.getNamedEntityGraphs().values(),
				runtimeModelCreationContext
		);
	}

	private void processBootEntities(
			java.util.Collection<PersistentClass> entityBindings,
			CacheImplementor cacheImplementor,
			PersisterFactory persisterFactory,
			RuntimeModelCreationContext modelCreationContext) {
		for ( final PersistentClass model : entityBindings ) {
			final NavigableRole rootEntityRole = new NavigableRole( model.getRootClass().getEntityName() );
			final EntityDataAccess accessStrategy = cacheImplementor.getEntityRegionAccess( rootEntityRole );
			final NaturalIdDataAccess naturalIdAccessStrategy = cacheImplementor
					.getNaturalIdCacheRegionAccessStrategy( rootEntityRole );

			final EntityPersister cp = persisterFactory.createEntityPersister(
					model,
					accessStrategy,
					naturalIdAccessStrategy,
					modelCreationContext
			);
			entityPersisterMap.put( model.getEntityName(), cp );

			if ( cp.getConcreteProxyClass() != null
					&& cp.getConcreteProxyClass().isInterface()
					&& !Map.class.isAssignableFrom( cp.getConcreteProxyClass() )
					&& cp.getMappedClass() != cp.getConcreteProxyClass() ) {
				// IMPL NOTE : we exclude Map based proxy interfaces here because that should
				//		indicate MAP entity mode.0

				if ( cp.getMappedClass().equals( cp.getConcreteProxyClass() ) ) {
					// this part handles an odd case in the Hibernate test suite where we map an interface
					// as the class and the proxy.  I cannot think of a real life use case for that
					// specific test, but..
					if ( log.isDebugEnabled() ) {
						log.debugf(
								"Entity [%s] mapped same interface [%s] as class and proxy",
								cp.getEntityName(),
								cp.getMappedClass()
						);
					}
				}
				else {
					final String old = entityProxyInterfaceMap.put( cp.getConcreteProxyClass(), cp.getEntityName() );
					if ( old != null ) {
						throw new HibernateException(
								String.format(
										Locale.ENGLISH,
										"Multiple entities [%s, %s] named the same interface [%s] as their proxy which is not supported",
										old,
										cp.getEntityName(),
										cp.getConcreteProxyClass().getName()
								)
						);
					}
				}
			}
		}
	}

	private void processBootCollections(
			java.util.Collection<Collection> collectionBindings,
			CacheImplementor cacheImplementor,
			PersisterFactory persisterFactory,
			RuntimeModelCreationContext modelCreationContext) {
		for ( final Collection model : collectionBindings ) {
			final NavigableRole navigableRole = new NavigableRole( model.getRole() );

			final CollectionDataAccess accessStrategy = cacheImplementor.getCollectionRegionAccess(
					navigableRole );

			final CollectionPersister persister = persisterFactory.createCollectionPersister(
					model,
					accessStrategy,
					modelCreationContext
			);
			collectionPersisterMap.put( model.getRole(), persister );
			Type indexType = persister.getIndexType();
			if ( indexType != null && indexType.isEntityType() && !indexType.isAnyType() ) {
				String entityName = ( (org.hibernate.type.EntityType) indexType ).getAssociatedEntityName();
				Set<String> roles = collectionRolesByEntityParticipant.get( entityName );
				//noinspection Java8MapApi
				if ( roles == null ) {
					roles = new HashSet<>();
					collectionRolesByEntityParticipant.put( entityName, roles );
				}
				roles.add( persister.getRole() );
			}
			Type elementType = persister.getElementType();
			if ( elementType.isEntityType() && !elementType.isAnyType() ) {
				String entityName = ( (org.hibernate.type.EntityType) elementType ).getAssociatedEntityName();
				Set<String> roles = collectionRolesByEntityParticipant.get( entityName );
				//noinspection Java8MapApi
				if ( roles == null ) {
					roles = new HashSet<>();
					collectionRolesByEntityParticipant.put( entityName, roles );
				}
				roles.add( persister.getRole() );
			}
		}
	}

	private static void registerEntityNameResolvers(
			EntityPersister persister,
			Set<EntityNameResolver> entityNameResolvers) {
		if ( persister.getRepresentationStrategy() == null ) {
			return;
		}
		registerEntityNameResolvers( persister.getRepresentationStrategy(), entityNameResolvers );
	}

	private static void registerEntityNameResolvers(
			EntityRepresentationStrategy representationStrategy,
			Set<EntityNameResolver> entityNameResolvers) {
		representationStrategy.visitEntityNameResolvers( entityNameResolvers::add );
	}

	@Override
	public java.util.Collection<EntityNameResolver> getEntityNameResolvers() {
		return entityNameResolvers;
	}


	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	public void visitEntityDescriptors(Consumer<EntityPersister> action) {
		entityPersisterMap.values().forEach( action );
	}

	@Override
	public EntityPersister resolveEntityDescriptor(EntityDomainType<?> entityDomainType) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public EntityPersister getEntityDescriptor(String entityName) {
		final EntityPersister entityPersister = entityPersisterMap.get( entityName );
		if ( entityPersister == null ) {
			throw new IllegalArgumentException( "Unable to locate persister: " + entityName );
		}
		return entityPersister;
	}

	@Override
	public EntityPersister getEntityDescriptor(NavigableRole name) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public EntityPersister findEntityDescriptor(String entityName) {
		return entityPersisterMap.get( entityName );
	}

	@Override
	public EntityPersister findEntityDescriptor(Class entityJavaType) {
		return findEntityDescriptor( entityJavaType.getName() );
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public EntityPersister getEntityDescriptor(Class entityJavaType) {
		EntityPersister entityPersister = entityPersisterMap.get( entityJavaType.getName() );
		if ( entityPersister == null ) {
			String mappedEntityName = entityProxyInterfaceMap.get( entityJavaType );
			if ( mappedEntityName != null ) {
				entityPersister = entityPersisterMap.get( mappedEntityName );
			}
		}

		if ( entityPersister == null ) {
			throw new IllegalArgumentException( "Unable to locate persister: " + entityJavaType.getName() );
		}

		return entityPersister;
	}

	@Override
	public EntityPersister locateEntityDescriptor(Class byClass) {
		EntityPersister entityPersister = entityPersisterMap.get( byClass.getName() );
		if ( entityPersister == null ) {
			String mappedEntityName = entityProxyInterfaceMap.get( byClass );
			if ( mappedEntityName != null ) {
				entityPersister = entityPersisterMap.get( mappedEntityName );
			}
		}

		if ( entityPersister == null ) {
			throw new UnknownEntityTypeException( "Unable to locate persister: " + byClass.getName() );
		}

		return entityPersister;
	}

	@Override
	public <X> EntityDomainType<X> entity(Class<X> cls) {
		return jpaMetamodel.entity( cls );
	}

	@Override
	public <X> ManagedDomainType<X> managedType(Class<X> cls) {
		return jpaMetamodel.managedType( cls );
	}

	@Override
	public <X> EmbeddableDomainType<X> embeddable(Class<X> cls) {
		return jpaMetamodel.embeddable( cls );
	}

	@Override
	public Set<ManagedType<?>> getManagedTypes() {
		return jpaMetamodel.getManagedTypes();
	}

	@Override
	public Set<EntityType<?>> getEntities() {
		return jpaMetamodel.getEntities();
	}

	@Override
	public Set<EmbeddableType<?>> getEmbeddables() {
		return jpaMetamodel.getEmbeddables();
	}

	@Override
	public <X> EntityDomainType<X> entity(String entityName) {
		return jpaMetamodel.entity( entityName );
	}

	@Override
	public <X> EntityDomainType<X> getHqlEntityReference(String entityName) {
		return jpaMetamodel.getHqlEntityReference( entityName );
	}

	@Override
	public <X> EntityDomainType<X> resolveHqlEntityReference(String entityName) {
		return jpaMetamodel.resolveHqlEntityReference( entityName );
	}

	@Override
	public void visitManagedTypes(Consumer<ManagedDomainType<?>> action) {
		jpaMetamodel.visitManagedTypes( action );
	}

	@Override
	public <X> ManagedDomainType<X> findManagedType(Class<X> cls) {
		return jpaMetamodel.findManagedType( cls );
	}

	@Override
	public void visitEntityTypes(Consumer<EntityDomainType<?>> action) {
		jpaMetamodel.visitEntityTypes( action );
	}

	@Override
	public <X> EntityDomainType<X> findEntityType(Class<X> cls) {
		return jpaMetamodel.findEntityType( cls );
	}

	@Override
	public void visitRootEntityTypes(Consumer<EntityDomainType<?>> action) {
		jpaMetamodel.visitRootEntityTypes( action );
	}

	@Override
	public void visitEmbeddables(Consumer<EmbeddableDomainType<?>> action) {
		jpaMetamodel.visitEmbeddables( action );
	}

	@Override
	public String qualifyImportableName(String queryName) {
		return jpaMetamodel.qualifyImportableName( queryName );
	}

	@Override
	public Map<String, Map<Class<?>, Enum<?>>> getAllowedEnumLiteralTexts() {
		return jpaMetamodel.getAllowedEnumLiteralTexts();
	}

	@Override
	public String getImportedClassName(String className) {
		throw new UnsupportedOperationException(  );
	}


	@Override
	public String[] getImplementors(String className) throws MappingException {
		// computeIfAbsent() can be a contention point and we expect all the values to be in the map at some point so
		// let's do an optimistic check first
		String[] implementors = implementorsCache.get( className );
		if ( implementors != null ) {
			return Arrays.copyOf( implementors, implementors.length );
		}

		try {
			final Class<?> clazz = getSessionFactory().getServiceRegistry()
					.getService( ClassLoaderService.class )
					.classForName( className );
			implementors = doGetImplementors( clazz );
			if ( implementors.length > 0 ) {
				implementorsCache.putIfAbsent( className, implementors );
				return Arrays.copyOf( implementors, implementors.length );
			}
			else {
				return EMPTY_IMPLEMENTORS;
			}
		}
		catch (ClassLoadingException e) {
			return new String[] { className }; // we don't cache anything for dynamic classes
		}
	}

	@Override
	public Map<String, EntityPersister> entityPersisters() {
		return entityPersisterMap;
	}

	@Override
	public CollectionPersister collectionPersister(String role) {
		final CollectionPersister persister = collectionPersisterMap.get( role );
		if ( persister == null ) {
			throw new MappingException( "Could not locate CollectionPersister for role : " + role );
		}
		return persister;
	}

	@Override
	public Map<String, CollectionPersister> collectionPersisters() {
		return collectionPersisterMap;
	}

	@Override
	public EntityPersister entityPersister(Class entityClass) {
		return entityPersister( entityClass.getName() );
	}

	@Override
	public EntityPersister entityPersister(String entityName) throws MappingException {
		EntityPersister result = entityPersisterMap.get( entityName );
		if ( result == null ) {
			throw new MappingException( "Unknown entity: " + entityName );
		}
		return result;
	}

	@Override
	public EntityPersister locateEntityPersister(String byName) {
		final EntityPersister entityPersister = entityPersisterMap.get( byName );
		if ( entityPersister == null ) {
			throw new UnknownEntityTypeException( "Unable to locate persister: " + byName );
		}
		return entityPersister;
	}

	@Override
	public String getImportedName(String name) {
		final String qualifiedName = jpaMetamodel.qualifyImportableName( name );
		return qualifiedName == null ? name : qualifiedName;
	}

	@Override
	public void visitCollectionDescriptors(Consumer<CollectionPersister> action) {
		collectionPersisterMap.values().forEach( action );
	}

	@Override
	public CollectionPersister getCollectionDescriptor(String role) {
		CollectionPersister collectionPersister = collectionPersisterMap.get( role );
		if ( collectionPersister == null ) {
			throw new IllegalArgumentException( "Unable to locate persister: " + role );
		}
		return collectionPersister;
	}

	@Override
	public CollectionPersister getCollectionDescriptor(NavigableRole role) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public CollectionPersister findCollectionDescriptor(NavigableRole role) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public CollectionPersister findCollectionDescriptor(String role) {
		return collectionPersisterMap.get( role );
	}

	@Override
	public Set<String> getCollectionRolesByEntityParticipant(String entityName) {
		return collectionRolesByEntityParticipant.get( entityName );
	}

	@Override
	public String[] getAllEntityNames() {
		return ArrayHelper.toStringArray( entityPersisterMap.keySet() );
	}

	@Override
	public String[] getAllCollectionRoles() {
		return ArrayHelper.toStringArray( collectionPersisterMap.keySet() );
	}

	@Override
	public <T> void addNamedEntityGraph(String graphName, RootGraphImplementor<T> entityGraph) {
		jpaMetamodel.addNamedEntityGraph( graphName, entityGraph );
	}

	@Override
	public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
		addNamedEntityGraph( graphName, (RootGraphImplementor<T>) entityGraph );
	}

	@Override
	public <T> RootGraphImplementor<T> findEntityGraphByName(String name) {
		return jpaMetamodel.findEntityGraphByName( name );
	}

	@Override
	public <T> List<RootGraphImplementor<? super T>> findEntityGraphsByJavaType(Class<T> entityClass) {
		return jpaMetamodel.findEntityGraphsByJavaType( entityClass );
	}

	@Override
	public JpaCompliance getJpaCompliance() {
		return jpaMetamodel.getJpaCompliance();
	}

	@Override
	public RootGraph<?> findNamedGraph(String name) {
		return findEntityGraphByName( name );
	}

	@Override
	public void visitNamedGraphs(Consumer<RootGraph<?>> action) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public RootGraph<?> defaultGraph(String entityName) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public RootGraph<?> defaultGraph(Class entityJavaType) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public RootGraph<?> defaultGraph(EntityPersister entityDescriptor) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public RootGraph<?> defaultGraph(EntityDomainType<?> entityDomainType) {
		return null;
	}

	@Override
	public List<RootGraph<?>> findRootGraphsForType(Class baseEntityJavaType) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public List<RootGraph<?>> findRootGraphsForType(String baseEntityName) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public List<RootGraph<?>> findRootGraphsForType(EntityPersister baseEntityDescriptor) {
		return null;
	}

	@Override
	public void close() {
		// anything to do ?
	}

	private String[] doGetImplementors(Class<?> clazz) throws MappingException {
		ArrayList<String> results = new ArrayList<>();
		for ( EntityPersister checkPersister : entityPersisters().values() ) {
			if ( !Queryable.class.isInstance( checkPersister ) ) {
				continue;
			}
			final Queryable checkQueryable = Queryable.class.cast( checkPersister );
			final String checkQueryableEntityName = checkQueryable.getEntityName();
			final boolean isMappedClass = clazz.getName().equals( checkQueryableEntityName );
			if ( checkQueryable.isExplicitPolymorphism() ) {
				if ( isMappedClass ) {
					return new String[] { clazz.getName() }; // NOTE EARLY EXIT
				}
			}
			else {
				if ( isMappedClass ) {
					results.add( checkQueryableEntityName );
				}
				else {
					final Class<?> mappedClass = checkQueryable.getMappedClass();
					if ( mappedClass != null && clazz.isAssignableFrom( mappedClass ) ) {
						final boolean assignableSuperclass;
						if ( checkQueryable.isInherited() ) {
							Class<?> mappedSuperclass = entityPersister( checkQueryable.getMappedSuperclass() ).getMappedClass();
							assignableSuperclass = clazz.isAssignableFrom( mappedSuperclass );
						}
						else {
							assignableSuperclass = false;
						}
						if ( !assignableSuperclass ) {
							results.add( checkQueryableEntityName );
						}
					}
				}
			}
		}

		return results.toArray( new String[results.size()] );
	}

	@Override
	public MappingModelExpressable lenientlyResolveMappingExpressable(
			SqmExpressable<?> sqmExpressable,
			Function<NavigablePath, TableGroup> tableGroupLocator) {
		return resolveMappingExpressable( sqmExpressable, tableGroupLocator );
	}


	@Override
	public MappingModelExpressable resolveMappingExpressable(SqmExpressable<?> sqmExpressable, Function<NavigablePath, TableGroup> tableGroupLocator) {
		if ( sqmExpressable instanceof SqmPath ) {
			final SqmPath sqmPath = (SqmPath) sqmExpressable;
			final NavigablePath navigablePath = sqmPath.getNavigablePath();
			if ( navigablePath.getParent() != null ) {
				final TableGroup parentTableGroup = tableGroupLocator.apply( navigablePath.getParent() );
				return parentTableGroup.getModelPart().findSubPart( navigablePath.getLocalName(), null );
			}
			return tableGroupLocator.apply( navigablePath.getParent() ).getModelPart();
		}

		if ( sqmExpressable instanceof BasicType<?> ) {
			return (BasicType) sqmExpressable;
		}

		if ( sqmExpressable instanceof BasicSqmPathSource<?> ) {
			return getTypeConfiguration().getBasicTypeForJavaType(((BasicSqmPathSource<?>) sqmExpressable).getJavaType());
		}

		if ( sqmExpressable instanceof SqmFieldLiteral ) {
			return getTypeConfiguration().getBasicTypeForJavaType( ( (SqmFieldLiteral<?>) sqmExpressable ).getJavaType() );
		}

		if ( sqmExpressable instanceof CompositeSqmPathSource ) {
			throw new NotYetImplementedFor6Exception( "Resolution of embedded-valued SqmExpressable nodes not yet implemented" );
		}

		if ( sqmExpressable instanceof EmbeddableTypeImpl ) {
			return (MappingModelExpressable) sqmExpressable;
		}

		if ( sqmExpressable instanceof EntityDomainType<?> ) {
			return getEntityDescriptor( ( (EntityDomainType<?>) sqmExpressable ).getHibernateEntityName() );
		}

		if ( sqmExpressable instanceof TupleType<?> ) {
			final MappingModelExpressable<?> mappingModelExpressable = tupleTypeCache.get( sqmExpressable );
			if ( mappingModelExpressable != null ) {
				return mappingModelExpressable;
			}
			final TupleType<?> tupleType = (TupleType<?>) sqmExpressable;
			final MappingModelExpressable<?>[] components = new MappingModelExpressable<?>[tupleType.componentCount()];
			for ( int i = 0; i < components.length; i++ ) {
				components[i] = resolveMappingExpressable( tupleType.get( i ), tableGroupLocator );
			}
			final MappingModelExpressable createdMappingModelExpressable = new TupleMappingModelExpressable( components );
			final MappingModelExpressable<?> existingMappingModelExpressable = tupleTypeCache.putIfAbsent(
					tupleType,
					createdMappingModelExpressable
			);
			return existingMappingModelExpressable == null
					? createdMappingModelExpressable
					: existingMappingModelExpressable;
		}
		return null;
	}

	@Override
	public  <T> AllowableParameterType<T> resolveQueryParameterType(Class<T> javaClass) {
		final BasicType<T> basicType = getTypeConfiguration().getBasicTypeForJavaType( javaClass );
		// For enums, we simply don't know the exact mapping if there is no basic type registered
		if ( basicType != null || javaClass.isEnum() ) {
			return basicType;
		}

		final ManagedDomainType<T> managedType = jpaMetamodel.findManagedType( javaClass );
		if ( managedType != null ) {
			return managedType;
		}

		final JavaType<T> javaType = getTypeConfiguration().getJavaTypeDescriptorRegistry()
				.findDescriptor( javaClass );
		if ( javaType != null ) {
			final JdbcType recommendedJdbcType = javaType.getRecommendedJdbcType( getTypeConfiguration().getCurrentBaseSqlTypeIndicators() );
			if ( recommendedJdbcType != null ) {
				return getTypeConfiguration().getBasicTypeRegistry().resolve(
						javaType,
						recommendedJdbcType
				);
			}
		}

		return null;
	}
}
