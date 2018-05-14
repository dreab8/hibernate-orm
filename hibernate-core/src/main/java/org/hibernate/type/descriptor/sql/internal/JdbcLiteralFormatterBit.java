package org.hibernate.type.descriptor.sql.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.spi.WrapperOptions;

/**
 * @author Andrea Boriero
 */
public class JdbcLiteralFormatterBit extends AbstractJdbcLiteralFormatter {
	public JdbcLiteralFormatterBit(JavaTypeDescriptor javaTypeDescriptor) {
		super( javaTypeDescriptor );
	}

	@Override
	public String toJdbcLiteral(
			Object value, Dialect dialect, WrapperOptions options) {
		return getJavaTypeDescriptor().toString( value );
	}
}

