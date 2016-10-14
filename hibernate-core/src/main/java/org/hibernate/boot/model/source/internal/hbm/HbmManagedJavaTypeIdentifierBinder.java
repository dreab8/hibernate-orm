/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.List;
import javax.persistence.metamodel.EmbeddableType;

import org.hibernate.boot.model.source.spi.AttributeSource;
import org.hibernate.boot.model.source.spi.EmbeddableSource;
import org.hibernate.boot.model.source.spi.IdentifierSource;
import org.hibernate.boot.model.source.spi.IdentifierSourceAggregatedComposite;
import org.hibernate.boot.model.source.spi.IdentifierSourceNonAggregatedComposite;
import org.hibernate.boot.model.source.spi.IdentifierSourceSimple;
import org.hibernate.boot.model.source.spi.SingularAttributeSource;
import org.hibernate.boot.model.source.spi.SingularAttributeSourceEmbedded;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.type.descriptor.internal.java.managed.identifier.IdentifierDescriptorBuilderAggregatedCompositeImpl;
import org.hibernate.type.descriptor.internal.java.managed.identifier.IdentifierDescriptorBuilderNonAggregatedCompositeImpl;
import org.hibernate.type.descriptor.internal.java.managed.identifier.IdentifierDescriptorBuilderSimpleImpl;
import org.hibernate.type.descriptor.internal.java.managed.identifier.IdentifierDescriptorSimple;
import org.hibernate.type.descriptor.spi.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.spi.java.managed.IdentifierDescriptor;
import org.hibernate.type.descriptor.spi.java.managed.IdentifierDescriptorBuilder;
import org.hibernate.type.descriptor.spi.java.managed.InitializationAccess;
import org.hibernate.type.descriptor.spi.java.managed.JavaTypeDescriptorEntityImplementor;

/**
 * @author Chris Cranford
 */
public class HbmManagedJavaTypeIdentifierBinder extends AbstractJavaTypeDescriptorBinder {

	protected HbmManagedJavaTypeIdentifierBinder(MetadataBuildingContext metadataBuildingContext) {
		super( metadataBuildingContext );
	}

	public void bindIdentifier(final EntityHierarchySourceImpl hierarchySource) {
		final JavaTypeDescriptorEntityImplementor rootTypeDescriptor =
				(JavaTypeDescriptorEntityImplementor) getJavaTypeDescriptorRegistry()
						.getDescriptor( hierarchySource.getRoot().getTypeName() );

		bindIdentifier( hierarchySource.getIdentifierSource(), rootTypeDescriptor.getInitializationAccess() );
	}

	public void bindIdentifier(final IdentifierSource identifierSource, final InitializationAccess initializationAccess) {
		switch ( identifierSource.getNature() ) {
			case SIMPLE: {
				final IdentifierDescriptorBuilderSimpleImpl identifierDescriptorBuilder =
						(IdentifierDescriptorBuilderSimpleImpl) initializationAccess.getIdentifierDescriptorBuilder(
								EntityIdentifierNature.SIMPLE );

				bindSimpleIdentifierType(
						( (IdentifierSourceSimple) identifierSource ).getIdentifierAttributeSource(),
						identifierDescriptorBuilder
				);
				break;
			}
			case AGGREGATED_COMPOSITE: {
				final IdentifierDescriptorBuilderAggregatedCompositeImpl identifierDescriptorBuilder =
						(IdentifierDescriptorBuilderAggregatedCompositeImpl) initializationAccess.getIdentifierDescriptorBuilder(
								EntityIdentifierNature.AGGREGATED_COMPOSITE );

				bindAggregatedCompositeIdentifierType(
						( (IdentifierSourceAggregatedComposite) identifierSource ).getIdentifierAttributeSource(),
						identifierDescriptorBuilder
				);
				break;
			}
			case NON_AGGREGATED_COMPOSITE: {
				final IdentifierDescriptorBuilderNonAggregatedCompositeImpl identifierDescriptorBuilder =
						(IdentifierDescriptorBuilderNonAggregatedCompositeImpl) initializationAccess.getIdentifierDescriptorBuilder(
								EntityIdentifierNature.NON_AGGREGATED_COMPOSITE );
				final IdentifierSourceNonAggregatedComposite identifierSourceNonAggregatedComposite = (IdentifierSourceNonAggregatedComposite) identifierSource;
				bindNonAggregatedCompositeIdentiferType(
						identifierSourceNonAggregatedComposite.getIdClassSource(),
						identifierSourceNonAggregatedComposite.getAttributeSourcesMakingUpIdentifier(),
						identifierDescriptorBuilder
				);
				break;
			}
		}
	}

	private void bindSimpleIdentifierType(
			final SingularAttributeSource identifierAttributeSource,
			final IdentifierDescriptorBuilderSimpleImpl identifierDescriptorBuilder) {
		final JavaTypeDescriptor javaTypeDescriptor = getJavaTypeDescriptor( identifierAttributeSource );
		identifierDescriptorBuilder.setName( identifierAttributeSource.getName() ).setType( javaTypeDescriptor );
	}

	private void bindAggregatedCompositeIdentifierType(final SingularAttributeSourceEmbedded identifierAttributeSource,
													   final IdentifierDescriptorBuilderAggregatedCompositeImpl identifierDescriptorBuilder) {
		final JavaTypeDescriptor javaTypeDescriptor = getJavaTypeDescriptor( identifierAttributeSource );
		identifierDescriptorBuilder.setName( identifierAttributeSource.getName() ).setType( (EmbeddableType) javaTypeDescriptor );
	}

	private void bindNonAggregatedCompositeIdentiferType(
			final EmbeddableSource idClassSource,
			final List<SingularAttributeSource> attributeSources,
			final IdentifierDescriptorBuilderNonAggregatedCompositeImpl identifierDescriptorBuilder) {
		if ( idClassSource != null ) {
			identifierDescriptorBuilder.setIdClassType( idClassSource.getClass() );
		}
		attributeSources.stream().forEach( attributeSource -> {
			JavaTypeDescriptor javaTypeDescriptor = getJavaTypeDescriptor( attributeSource );
			identifierDescriptorBuilder.addId( attributeSource.getName(), javaTypeDescriptor );

		} );
	}

	private JavaTypeDescriptor getJavaTypeDescriptor(AttributeSource identifierAttributeSource) {
		return getJavaTypeDescriptorRegistry()
				.getDescriptor( identifierAttributeSource.getTypeInformation().getName() );
	}
}
