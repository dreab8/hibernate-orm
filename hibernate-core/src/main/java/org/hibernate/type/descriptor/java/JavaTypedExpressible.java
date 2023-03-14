/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;

/**
 * Contract for something that has an associated JavaType
 */
public interface JavaTypedExpressible<T> {
	JavaType<T> getExpressibleJavaType();

	default int extractHashCodeFromDisassembled(Serializable disassembledValue) {
		return getExpressibleJavaType().extractHashCodeFromDisassembled( disassembledValue );
	}

}
