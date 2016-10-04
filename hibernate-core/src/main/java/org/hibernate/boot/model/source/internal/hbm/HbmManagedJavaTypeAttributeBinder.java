/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.model.source.spi.AttributeSource;
import org.hibernate.boot.model.source.spi.IdentifiableTypeSource;
import org.hibernate.boot.model.source.spi.InheritanceType;
import org.hibernate.boot.model.source.spi.PluralAttributeElementSource;
import org.hibernate.boot.model.source.spi.PluralAttributeElementSourceBasic;
import org.hibernate.boot.model.source.spi.PluralAttributeElementSourceManyToMany;
import org.hibernate.boot.model.source.spi.PluralAttributeElementSourceOneToMany;
import org.hibernate.boot.model.source.spi.PluralAttributeSource;
import org.hibernate.boot.model.source.spi.SingularAttributeSource;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.type.descriptor.spi.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.spi.java.managed.AttributeBuilderPlural;
import org.hibernate.type.descriptor.spi.java.managed.AttributeBuilderSingular;
import org.hibernate.type.descriptor.spi.java.managed.JavaTypeDescriptorEntityImplementor;

/**
 * @author Chris Cranford
 */
public class HbmManagedJavaTypeAttributeBinder {
	private final MetadataBuildingContext metadataBuildingContext;

	public HbmManagedJavaTypeAttributeBinder(MetadataBuildingContext metadataBuildingContext) {
		this.metadataBuildingContext = metadataBuildingContext;
	}

	public void bindAttributes(final EntityHierarchySourceImpl hierarchySource) {
		if ( !InheritanceType.NO_INHERITANCE.equals( hierarchySource.getHierarchyInheritanceType() ) ) {
			bindHierarchyAttributes( hierarchySource.getRoot() );
		}
		else {
			bindAttributes( hierarchySource.getRoot() );
		}
	}

	private void bindHierarchyAttributes(IdentifiableTypeSource source) {
		bindAttributes( source );
		source.getSubTypes().forEach( this::bindHierarchyAttributes );
	}

	private void bindAttributes(IdentifiableTypeSource source) {
		final JavaTypeDescriptorEntityImplementor javaTypeDescriptor =
				(JavaTypeDescriptorEntityImplementor) metadataBuildingContext.getBootstrapContext()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( source.getTypeName() );

		for ( AttributeSource attributeSource : source.attributeSources() ) {
			if ( attributeSource.isSingular() ) {
				bindSingularAttribute( javaTypeDescriptor, (SingularAttributeSource) attributeSource );
			}
			else {
				bindPluralAttribute( javaTypeDescriptor, (PluralAttributeSource) attributeSource );
			}
		}
	}

	private void bindSingularAttribute(JavaTypeDescriptorEntityImplementor entityDescriptor, SingularAttributeSource attributeSource) {
		final JavaTypeDescriptor attributeDescriptor = metadataBuildingContext.getBootstrapContext()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( attributeSource.getTypeInformation().getName() );

		final AttributeBuilderSingular attributeBuilder = entityDescriptor.getInitializationAccess()
				.getSingularAttributeBuilder( attributeSource.getName() );

		attributeBuilder.setType( attributeDescriptor );
	}

	private void bindPluralAttribute(JavaTypeDescriptorEntityImplementor entityDescriptor, PluralAttributeSource attributeSource) {
		final JavaTypeDescriptor elementTypeDescriptor = metadataBuildingContext.getBootstrapContext()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( getPluralAttributeElementType( attributeSource ) );

		final JavaTypeDescriptor attributeDescriptor = metadataBuildingContext.getBootstrapContext()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( attributeSource.getNature().reportedJavaType() );

		final AttributeBuilderPlural attributeBuilder = entityDescriptor.getInitializationAccess()
				.getPluralAttributeBuilder( attributeSource.getName() );

		attributeBuilder.setCollectionType( elementTypeDescriptor );
		attributeBuilder.setType( attributeDescriptor );
	}

	private String getPluralAttributeElementType(PluralAttributeSource attributeSource) {
		final PluralAttributeElementSource elementSource = attributeSource.getElementSource();

		// todo: add support for MANY_TO_ANY, AGGREGATE
		switch ( elementSource.getNature() ) {
			case ONE_TO_MANY: {
				return ( (PluralAttributeElementSourceOneToMany) elementSource ).getReferencedEntityName();
			}
			case MANY_TO_MANY: {
				return ( (PluralAttributeElementSourceManyToMany) elementSource ).getReferencedEntityName();
			}
			case BASIC: {
				return ( (PluralAttributeElementSourceBasic) elementSource ).getExplicitHibernateTypeSource().getName();
			}
			default: {
				throw new NotYetImplementedException( "PluralAttributeElementSourceNature not yet supported" );
			}
		}
	}
}
