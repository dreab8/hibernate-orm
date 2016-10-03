/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.type.internal;

import org.hibernate.boot.model.source.internal.hbm.EntityHierarchySourceImpl;
import org.hibernate.boot.model.source.spi.AttributeSource;
import org.hibernate.boot.model.source.spi.IdentifiableTypeSource;
import org.hibernate.boot.model.source.spi.IdentifierSourceAggregatedComposite;
import org.hibernate.boot.model.source.spi.IdentifierSourceNonAggregatedComposite;
import org.hibernate.boot.model.source.spi.IdentifierSourceSimple;
import org.hibernate.boot.model.source.spi.InheritanceType;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.type.descriptor.internal.java.managed.RootEntityDescriptor;
import org.hibernate.type.descriptor.spi.java.managed.EntityHierarchy;
import org.hibernate.type.descriptor.spi.java.managed.JavaTypeDescriptorEntityImplementor;

/**
 * @author Chris Cranford
 */
public class HbmManagedJavaTypeDescriptorBuilder {

	private final MetadataBuildingContext metadataBuildingContext;

	public HbmManagedJavaTypeDescriptorBuilder(MetadataBuildingContext metadataBuildingContext) {
		this.metadataBuildingContext = metadataBuildingContext;
	}

	public void buildEntityHierarchy(EntityHierarchySourceImpl hierarchySource) {
		buildDescriptors( hierarchySource );

		buildIdentifierType( hierarchySource );

		buildAttributes( hierarchySource );
	}

	private void buildDescriptors(final EntityHierarchySourceImpl hierarchySource) {
		final RootEntityDescriptor rootDescriptor = (RootEntityDescriptor) metadataBuildingContext.getMetadataCollector()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.makeRootEntityDescriptor(
						hierarchySource.getRoot().getEntityNamingSource().getEntityName(),
						interpretInheritanceStyle( hierarchySource.getHierarchyInheritanceType() ),
						hierarchySource.getEntityMode()
				);

		if ( !InheritanceType.NO_INHERITANCE.equals( hierarchySource.getHierarchyInheritanceType() ) ) {
			buildSubclassDescriptors( hierarchySource.getRoot(), rootDescriptor );
		}
	}

	private void buildSubclassDescriptors(IdentifiableTypeSource entitySource, JavaTypeDescriptorEntityImplementor parentDescriptor) {
		for ( IdentifiableTypeSource subType : entitySource.getSubTypes() ) {
			buildSubclassDescriptor( subType, parentDescriptor );
		}
	}

	private void buildSubclassDescriptor(IdentifiableTypeSource entitySource, JavaTypeDescriptorEntityImplementor superTypeDescriptor) {
		final JavaTypeDescriptorEntityImplementor entityDescriptor = metadataBuildingContext.getBootstrapContext()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.makeEntityDescriptor(
						entitySource.getTypeName(),
						superTypeDescriptor
				);
		entityDescriptor.getInitializationAccess().setSuperType( superTypeDescriptor );

		buildSubclassDescriptors( entitySource, entityDescriptor );
	}

	private void buildAttributes(final EntityHierarchySourceImpl hierarchySource) {
		if ( !InheritanceType.NO_INHERITANCE.equals( hierarchySource.getHierarchyInheritanceType() ) ) {
			buildHierarchyAttributes( hierarchySource.getRoot() );
		}
		else {
			buildAttributes( hierarchySource.getRoot() );
		}
	}

	private void buildHierarchyAttributes(IdentifiableTypeSource hierarchySource) {
		buildAttributes( hierarchySource );
		for ( IdentifiableTypeSource subType : hierarchySource.getSubTypes() ) {
			buildHierarchyAttributes( subType );
		}
	}

	private void buildAttributes(IdentifiableTypeSource entitySource) {
		final JavaTypeDescriptorEntityImplementor javaTypeDescriptor = metadataBuildingContext.getBootstrapContext()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getEntityDescriptor( entitySource.getTypeName() );

		for ( AttributeSource attributeSource : entitySource.attributeSources() ) {
			if ( attributeSource.isSingular() ) {
				// todo: build singular attribute
			}
			else {
				// todo: build plural attribute
			}
		}
	}

	private void buildIdentifierType(final EntityHierarchySourceImpl hierarchySource) {
		switch ( hierarchySource.getIdentifierSource().getNature() ) {
			case SIMPLE: {
				buildSimpleIdentifierType( hierarchySource );
				break;
			}
			case AGGREGATED_COMPOSITE: {
				buildAggregatedCompositeIdentifierType( hierarchySource );
				break;
			}
			case NON_AGGREGATED_COMPOSITE: {
				buildNonAggregatedCompositeIdentiferType( hierarchySource );
				break;
			}
		}
	}

	private void buildSimpleIdentifierType(final EntityHierarchySourceImpl hierarchySource) {
		final IdentifierSourceSimple idSource = (IdentifierSourceSimple) hierarchySource.getIdentifierSource();
		// todo: need to create an IdentifierTypeDescriptor here
	}

	private void buildAggregatedCompositeIdentifierType(final EntityHierarchySourceImpl hierarchySource) {
		final IdentifierSourceAggregatedComposite identifierSource
				= (IdentifierSourceAggregatedComposite) hierarchySource.getIdentifierSource();
		// todo: need to create an IdentifierTypeDescriptor here
		throw new NotYetImplementedException();
	}

	private void buildNonAggregatedCompositeIdentiferType(final EntityHierarchySourceImpl hierarchySource) {
		final IdentifierSourceNonAggregatedComposite identifierSource
				= (IdentifierSourceNonAggregatedComposite) hierarchySource.getIdentifierSource();
		// todo: need to create an IdentifierTypeDescriptor here
		throw new NotYetImplementedException();
	}

	private EntityHierarchy.InheritanceStyle interpretInheritanceStyle(InheritanceType hierarchyInheritanceType) {
		switch ( hierarchyInheritanceType ) {
			case JOINED: {
				return EntityHierarchy.InheritanceStyle.JOINED;
			}
			case UNION: {
				return EntityHierarchy.InheritanceStyle.TABLE_PER_CLASS;
			}
			default: {
				return EntityHierarchy.InheritanceStyle.SINGLE_TABLE;
			}
		}
	}
}
