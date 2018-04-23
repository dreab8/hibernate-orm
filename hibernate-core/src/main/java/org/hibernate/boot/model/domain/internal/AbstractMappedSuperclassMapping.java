/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain.internal;

import javax.persistence.metamodel.Type.PersistenceType;

import org.hibernate.boot.model.domain.EntityMappingHierarchy;
import org.hibernate.boot.model.domain.MappedSuperclassJavaTypeMapping;
import org.hibernate.boot.model.domain.MappedSuperclassMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.domain.spi.MappedSuperclassImplementor;

/**
 * @author Chris Cranford
 */
public abstract class AbstractMappedSuperclassMapping
		extends AbstractIdentifiableTypeMapping
		implements MappedSuperclassImplementor {

	public AbstractMappedSuperclassMapping(
			EntityMappingHierarchy entityMappingHierarchy,
			MappedSuperclassJavaTypeMapping javaTypeMapping) {
		super( entityMappingHierarchy, javaTypeMapping );
	}

	@Override
	public void addDeclaredPersistentAttribute(PersistentAttributeMapping attribute) {
		for ( PersistentAttributeMapping existingAttribute : getDeclaredPersistentAttributes() ) {
			if ( attribute.getName().equals( existingAttribute.getName() ) ) {
				return;
			}
		}
		super.addDeclaredPersistentAttribute( attribute );
	}

	@Override
	public MappedSuperclassMapping getSuperManagedTypeMapping() {
		return (MappedSuperclassMapping) super.getSuperManagedTypeMapping();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.MAPPED_SUPERCLASS;
	}
}
