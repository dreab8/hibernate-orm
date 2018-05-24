/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.spi;

import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.domain.EntityMappingHierarchy;
import org.hibernate.boot.model.domain.MappedSuperclassMapping;
import org.hibernate.boot.model.domain.spi.EmbeddedValueMappingImplementor;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.collection.spi.PersistentCollectionRepresentationResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.internal.JpaStaticMetaModelPopulationSetting;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.MappedSuperclassDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeRepresentationResolver;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * "Parameter object" providing access to additional information that may be needed
 * in the creation of the persisters.
 *
 * @author Steve Ebersole
 */
public interface RuntimeModelCreationContext extends JpaAttributeConverterCreationContext {
	SessionFactoryImplementor getSessionFactory();

	BootstrapContext getBootstrapContext();

	TypeConfiguration getTypeConfiguration();
	MetadataImplementor getMetadata();

	InFlightRuntimeModel getInFlightRuntimeModel();

	DatabaseModel getDatabaseModel();
	DatabaseObjectResolver getDatabaseObjectResolver();

	JpaStaticMetaModelPopulationSetting getJpaStaticMetaModelPopulationSetting();

	RuntimeModelDescriptorFactory getRuntimeModelDescriptorFactory();
	ManagedTypeRepresentationResolver getRepresentationStrategySelector();

	default IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
		return getSessionFactory().getServiceRegistry().getService( MutableIdentifierGeneratorFactory.class );
	}

	PersistentCollectionRepresentationResolver getPersistentCollectionRepresentationResolver();

	@Override
	default ManagedBeanRegistry getManagedBeanRegistry() {
		return getSessionFactory().getServiceRegistry().getService( ManagedBeanRegistry.class );
	}

	@Override
	default JavaTypeDescriptorRegistry getJavaTypeDescriptorRegistry() {
		return getTypeConfiguration().getJavaTypeDescriptorRegistry();
	}


	// todo (6.0) : replace these with InFlightRuntimeModel?

	void registerEntityHierarchy(
			EntityHierarchy runtimeHierarchy,
			EntityMappingHierarchy bootHierarchy);

	void registerEntityDescriptor(
			EntityDescriptor runtimeDescriptor,
			EntityMapping bootDescriptor);

	void registerMappedSuperclassDescriptor(
			MappedSuperclassDescriptor runtimeType,
			MappedSuperclassMapping bootMapping);

	void registerCollectionDescriptor(
			PersistentCollectionDescriptor runtimeDescriptor,
			Collection bootDescriptor);

	void registerEmbeddableDescriptor(
			EmbeddedTypeDescriptor runtimeDescriptor,
			EmbeddedValueMappingImplementor bootDescriptor);

}
