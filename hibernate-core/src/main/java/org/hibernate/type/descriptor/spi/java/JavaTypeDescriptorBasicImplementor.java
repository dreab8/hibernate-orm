/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.spi.java;

import java.io.Serializable;
import javax.persistence.metamodel.BasicType;

/**
 * @author Steve Ebersole
 */
public interface JavaTypeDescriptorBasicImplementor<T> extends JavaTypeDescriptor<T>, BasicType<T>, Serializable {
	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}
}
