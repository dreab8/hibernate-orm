/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.source.spi.CompositeIdentifierSource;
import org.hibernate.boot.model.source.spi.IdentifierSourceAggregatedComposite;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;

/**
 * @author Andrea Boriero
 */
public class AbstractCompositeEntityIdentifierBinder extends AbstractEntityIdentifierBinder {
	public AbstractCompositeEntityIdentifierBinder(
			Database database,
			ObjectNameNormalizer objectNameNormalizer) {
		super( database, objectNameNormalizer );
	}

	void finishBindingCompositeIdentifier(
			MappingDocument sourceDocument,
			RootClass rootEntityDescriptor,
			CompositeIdentifierSource identifierSource,
			Component cid,
			String propertyName) {
		if ( propertyName == null ) {
			rootEntityDescriptor.setEmbeddedIdentifier( cid.isEmbedded() );
			if ( cid.isEmbedded() ) {
				// todo : what is the implication of this?
				cid.setDynamic( !rootEntityDescriptor.hasPojoRepresentation() );
				/*
				 * Property prop = new Property(); prop.setName("id");
				 * prop.setPropertyAccessorName("embedded"); prop.setValue(id);
				 * entity.setIdentifierProperty(prop);
				 */
			}
		}
		else {
			Property prop = new Property();
			prop.setValue( cid );
			PropertyBinder.bind(
					sourceDocument,
					( (IdentifierSourceAggregatedComposite) identifierSource ).getIdentifierAttributeSource(),
					prop
			);
			rootEntityDescriptor.setIdentifierProperty( prop );
			rootEntityDescriptor.setDeclaredIdentifierProperty( prop );
		}

		makeIdentifier(
				sourceDocument,
				identifierSource.getIdentifierGeneratorDescriptor(),
				null,
				cid
		);
	}
}
