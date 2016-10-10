/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.internal.java.managed;

import org.hibernate.type.descriptor.spi.java.managed.JavaTypeDescriptorEmbeddableImplementor;

/**
 * @author Chris Cranford
 */
public class EmbeddableDescriptor
		extends ManagedTypeDescriptor
		implements JavaTypeDescriptorEmbeddableImplementor {

	public EmbeddableDescriptor(String typeName, Class javaType, ManagedTypeDescriptor superTypeDescriptor) {
		super( typeName, javaType, superTypeDescriptor );
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

}
