/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;


import java.util.List;

import org.hibernate.engine.spi.IdentifierValue;

/**
 * Support for composite identifier mappings
 *
 * @author Andrea Boriero
 */
public interface CompositeIdentifierMapping extends EntityIdentifierMapping {

	@Override
	default IdentifierValue getUnsavedStrategy() {
		return IdentifierValue.UNDEFINED;
	}

	/**
	 * Does the identifier have a corresponding EmbeddableId or IdClass?
	 *
	 * @return false if there is not an IdCass or an EmbeddableId
	 */
	boolean hasContainingClass();

	EmbeddableMappingType getPartMappingType();

	/**
	 * Returns the embeddable type descriptor of the id-class, if there is one,
	 * otherwise the one of the virtual embeddable mapping type.
	 */
	EmbeddableMappingType getMappedIdEmbeddableTypeDescriptor();
}
