/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.model.source.internal.hbm.EntityHierarchySourceImpl;
import org.hibernate.boot.model.source.internal.hbm.HbmManagedJavaTypeAttributeBinder;
import org.hibernate.boot.model.source.internal.hbm.HbmManagedJavaTypeDescriptorBinder;
import org.hibernate.boot.model.source.internal.hbm.HbmManagedJavaTypeIdentifierBinder;
import org.hibernate.boot.model.source.spi.IdentifierSourceAggregatedComposite;
import org.hibernate.boot.model.source.spi.IdentifierSourceNonAggregatedComposite;
import org.hibernate.boot.model.source.spi.IdentifierSourceSimple;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.NotYetImplementedException;

/**
 * @author Chris Cranford
 */
public class HbmMetadataSourceManagedTypeBuilder {

	private final HbmManagedJavaTypeDescriptorBinder descriptorBinder;
	private final HbmManagedJavaTypeAttributeBinder attributeBinder;
	private final HbmManagedJavaTypeIdentifierBinder identifierBinder;

	public HbmMetadataSourceManagedTypeBuilder(MetadataBuildingContext metadataBuildingContext) {
		this.descriptorBinder = new HbmManagedJavaTypeDescriptorBinder( metadataBuildingContext );
		this.attributeBinder = new HbmManagedJavaTypeAttributeBinder( metadataBuildingContext );
		this.identifierBinder = new HbmManagedJavaTypeIdentifierBinder( metadataBuildingContext );
	}

	public void buildEntityHierarchy(EntityHierarchySourceImpl hierarchySource) {
		descriptorBinder.bindDescriptors( hierarchySource );
		identifierBinder.bindIdentifier( hierarchySource );
		attributeBinder.bindAttributes( hierarchySource );
	}

	// todo: move these methods to the HbmManagedJavaTypeIdentifierBinder

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


}
