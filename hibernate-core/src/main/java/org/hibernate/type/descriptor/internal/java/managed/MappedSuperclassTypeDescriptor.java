/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.internal.java.managed;

import org.hibernate.type.descriptor.spi.java.managed.EntityHierarchy;
import org.hibernate.type.descriptor.spi.java.managed.JavaTypeDescriptorMappedSuperclassImplementor;

/**
 * @author Steve Ebersole
 */
public class MappedSuperclassTypeDescriptor
		extends IdentifiableTypeDescriptor
		implements JavaTypeDescriptorMappedSuperclassImplementor {

	public MappedSuperclassTypeDescriptor(
			Class javaType,
			EntityHierarchy entityHierarchy,
			ManagedTypeDescriptor superTypeDescriptor) {
		super( javaType.getName(), entityHierarchy, javaType, superTypeDescriptor );
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.MAPPED_SUPERCLASS;
	}
}
