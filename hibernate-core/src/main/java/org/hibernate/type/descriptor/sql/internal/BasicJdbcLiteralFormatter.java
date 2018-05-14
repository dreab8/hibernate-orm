/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.sql.internal;


import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.spi.WrapperOptions;

/**
 * @author Steve Ebersole
 */
public abstract class BasicJdbcLiteralFormatter extends AbstractJdbcLiteralFormatter {
	public BasicJdbcLiteralFormatter(JavaTypeDescriptor<?> javaTypeDescriptor) {
		super( javaTypeDescriptor );
	}

	@SuppressWarnings("unchecked")
	protected <X> X unwrap(Object value, Class<X> unwrapType, WrapperOptions options) {
		assert value != null;

		// for performance reasons, avoid conversions if we can
		if ( unwrapType.isInstance( value ) ) {
			return (X) value;
		}

		return (X) getJavaTypeDescriptor().unwrap( value, unwrapType, options );
	}
}