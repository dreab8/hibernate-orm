/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.common.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.SqmDomainMetamodelImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.internal.ImprovedCollectionPersisterImpl;
import org.hibernate.persister.collection.spi.ImprovedCollectionPersister;
import org.hibernate.persister.entity.internal.ImprovedEntityPersisterImpl;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.sqm.domain.AttributeReference;
import org.hibernate.sqm.domain.BasicType;
import org.hibernate.sqm.domain.DomainReference;
import org.hibernate.sqm.domain.EntityReference;
import org.hibernate.sqm.domain.NoSuchAttributeException;
import org.hibernate.sqm.query.expression.BinaryArithmeticSqmExpression;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.spi.basic.BasicTypeParameters;
import org.hibernate.type.spi.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.MutabilityPlan;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class DomainMetamodelImpl implements SqmDomainMetamodelImplementor {
	private final DatabaseModel databaseModel  = new DatabaseModel();
	private final SessionFactoryImplementor sessionFactory;

	private final Map<EntityPersister, ImprovedEntityPersisterImpl> entityTypeDescriptorMap;
	private Map<String,PolymorphicEntityTypeImpl> polymorphicEntityTypeDescriptorMap;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// This section needs a bit of explanation...
	//
	// First, note too that it works on the assumption of PersisterFactoryImpl.INSTANCE having
	// been specified as the PersisterFactory used to build the SessionFactory.
	//
	// From there the first thing we do is to ask the PersisterFactoryImpl to "finish up" its
	// processing.  Internally PersisterFactoryImpl builds the legacy persisters and returns
	// them to the caller as per its contracts.  However, additionally for entity persisters
	// it will build the ImprovedEntityPersister variant*.  finishUp performs a second init
	// phase on each of the ImprovedEntityPersister instances, part of which is to build
	// its attribute descriptors.  When a plural attribute is processed, an ImprovedCollectionPersister
	// instance is built, but much like with ImprovedEntityPersister that is just a "shell"; we delay
	// most of its init until the ImprovedCollectionPersister.finishInitialization call done in
	// the DomainMetamodelImpl ctor.
	//
	// So functionally we:
	//		1) build all ImprovedEntityPersister instances
	//		2) finalize all ImprovedEntityPersister instances (side effect being creation of ImprovedCollectionPersister instances)
	//		3) finalize all ImprovedCollectionPersister instances.
	//
	// * - obviously a lot of this changes as we integrate this into ORM properly,  For example
	// the improved persister contracts will just simply be part of the ORM persister contracts so
	// no {persister}->{improved persister} mapping is needed.  Will need some thought on how to locate
	// the "declaring ManagedType" for the improved CollectionPersister from the PersisterFactory
	public DomainMetamodelImpl(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
		this.entityTypeDescriptorMap = PersisterFactoryImpl.INSTANCE.getEntityPersisterMap();
		PersisterFactoryImpl.INSTANCE.finishUp( databaseModel, this );
		for ( ImprovedCollectionPersister improvedCollectionPersister : collectionPersisterMap.values() ) {
			improvedCollectionPersister.finishInitialization( databaseModel, this );
		}
	}

	private Map<CollectionPersister, ImprovedCollectionPersister> collectionPersisterMap = new HashMap<>();

	public void registerCollectionPersister(ImprovedCollectionPersisterImpl persister) {
		collectionPersisterMap.put( persister.getPersister(), persister );
	}
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	@Override
	public <T> org.hibernate.type.spi.BasicType<T> getBasicType(Class<T> javaType) {
		return getBasicType( javaType, null );
	}

	@Override
	public <T> org.hibernate.type.spi.BasicType<T> getBasicType(Class<T> javaType, TemporalType temporalType) {
		return getTypeConfiguration().getBasicTypeRegistry().resolveBasicType(
				new BasicTypeParameters<T>() {
					@Override
					public JavaTypeDescriptor<T> getJavaTypeDescriptor() {
						return getTypeConfiguration().getTypeDescriptorRegistryAccess().getJavaTypeDescriptorRegistry().getDescriptor( javaType );
					}

					@Override
					public SqlTypeDescriptor getSqlTypeDescriptor() {
						return null;
					}

					@Override
					public AttributeConverterDefinition getAttributeConverterDefinition() {
						return null;
					}

					@Override
					public MutabilityPlan<T> getMutabilityPlan() {
						return null;
					}

					@Override
					public Comparator<T> getComparator() {
						return null;
					}

					@Override
					public javax.persistence.TemporalType getTemporalPrecision() {
						return temporalType;
					}
				},
				getTypeConfiguration().getBasicTypeRegistry().getBaseJdbcRecommendedSqlTypeMappingContext()
		);
	}

	private TypeConfiguration getTypeConfiguration() {
		return sessionFactory.getMetamodel().getTypeConfiguration();
	}

	@Override
	public EntityReference resolveEntityReference(Class javaType) {
		return resolveEntityReference( javaType.getName() );
	}

	@Override
	public EntityReference resolveEntityReference(String name) {
		final String importedName = sessionFactory.getMetamodel().getImportedClassName( name );
		if ( importedName != null ) {
			name = importedName;
		}

		// look at existing non-polymorphic descriptors
		final EntityPersister persister = sessionFactory.getMetamodel().entityPersister( name );
		if ( persister != null ) {
			return entityTypeDescriptorMap.get( persister );
		}

		// look at existing polymorphic descriptors
		if ( polymorphicEntityTypeDescriptorMap != null ) {
			PolymorphicEntityTypeImpl existingEntry = polymorphicEntityTypeDescriptorMap.get( name );
			if ( existingEntry != null ) {
				return existingEntry;
			}
		}


		final String[] implementors = sessionFactory.getMetamodel().getImplementors( name );
		if ( implementors != null ) {
			if ( implementors.length == 1 ) {
				return entityTypeDescriptorMap.get( sessionFactory.getMetamodel().entityPersister( implementors[0] ) );
			}
			else if ( implementors.length > 1 ) {
				final List<org.hibernate.sqm.domain.EntityType> implementDescriptors = new ArrayList<>();
				for ( String implementor : implementors ) {
					implementDescriptors.add(
							entityTypeDescriptorMap.get( sessionFactory.getMetamodel().entityPersister( implementor ) )
					);
				}
				if ( polymorphicEntityTypeDescriptorMap == null ) {
					polymorphicEntityTypeDescriptorMap = new HashMap<>();
				}
				PolymorphicEntityTypeImpl descriptor = new PolymorphicEntityTypeImpl(
						this,
						name,
						implementDescriptors
				);
				polymorphicEntityTypeDescriptorMap.put( name, descriptor );
				return descriptor;
			}
		}

		throw new HibernateException( "Could not resolve entity reference [" + name + "] from query" );
	}


	@Override
	public AttributeReference locateAttributeReference(
			DomainReference sourceBinding, String attributeName) {
		return null;
	}

	@Override
	public AttributeReference resolveAttributeReference(DomainReference sourceBinding, String attributeName)
			throws NoSuchAttributeException {
		return null;
	}

	@Override
	public BasicType resolveCastTargetType(String name) {
		return sessionFactory.getMetamodel().getTypeConfiguration().resolveCastTargetType( name );
	}

	@Override
	public BasicType resolveBasicType(Class javaType) {
		return null;
	}

	@Override
	public BasicType resolveArithmeticType(
			DomainReference firstType, DomainReference secondType, BinaryArithmeticSqmExpression.Operation operation) {
		return null;
	}

	@Override
	public BasicType resolveSumFunctionType(DomainReference argumentType) {
		return null;
	}
}
