/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.internal.sql;

import org.hibernate.type.descriptor.spi.java.JavaTypeDescriptor;
import org.hibernate.type.mapper.spi.JdbcLiteralFormatter;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractJdbcLiteralFormatter implements JdbcLiteralFormatter {
	private final JavaTypeDescriptor javaTypeDescriptor;

	public AbstractJdbcLiteralFormatter(JavaTypeDescriptor javaTypeDescriptor) {
		this.javaTypeDescriptor = javaTypeDescriptor;
	}

	protected JavaTypeDescriptor getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}
}
