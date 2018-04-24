package org.hibernate.type.descriptor.sql.spi;

import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Andrea Boriero
 */
public interface SqlTypeDescriptor extends org.hibernate.type.descriptor.sql.SqlTypeDescriptor {

	<T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor);

}

