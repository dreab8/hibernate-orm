/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.source.spi.IdentifierSourceAggregatedComposite;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.RootClass;

/**
 * @author Andrea Boriero
 */
public class AggregatedCompositeEntityIdentifierBinder extends AbstractCompositeEntityIdentifierBinder {


	public AggregatedCompositeEntityIdentifierBinder(
			Database database,
			ObjectNameNormalizer objectNameNormalizer) {
		super( database, objectNameNormalizer );
	}

	public void bind(
			MappingDocument mappingDocument,
			EntityHierarchySourceImpl hierarchySource,
			RootClass rootEntityDescriptor) {

		// an aggregated composite-id is a composite-id that defines a singular
		// (composite) attribute as part of the entity to represent the id.

		final IdentifierSourceAggregatedComposite identifierSource
				= (IdentifierSourceAggregatedComposite) hierarchySource.getIdentifierSource();

		final Component cid = new Component( mappingDocument.getMetadataCollector(), rootEntityDescriptor );
		cid.setKey( true );
		rootEntityDescriptor.setIdentifier( cid );

		final String idClassName = extractIdClassName( identifierSource );

		final String idPropertyName = identifierSource.getIdentifierAttributeSource().getName();
		final String pathPart = idPropertyName == null ? "<id>" : idPropertyName;

		bindComponent(
				mappingDocument,
				hierarchySource.getRoot().getAttributeRoleBase().append( pathPart ).getFullPath(),
				identifierSource.getEmbeddableSource(),
				cid,
				idClassName,
				rootEntityDescriptor.getClassName(),
				idPropertyName,
				idClassName == null && idPropertyName == null,
				identifierSource.getEmbeddableSource().isDynamic(),
				identifierSource.getIdentifierAttributeSource().getXmlNodeName()
		);

		finishBindingCompositeIdentifier(
				mappingDocument,
				rootEntityDescriptor,
				identifierSource,
				cid,
				idPropertyName
		);
	}

	private String extractIdClassName(IdentifierSourceAggregatedComposite identifierSource) {
		if ( identifierSource.getEmbeddableSource().getTypeDescriptor() == null ) {
			return null;
		}

		return identifierSource.getEmbeddableSource().getTypeDescriptor().getName();
	}
}
