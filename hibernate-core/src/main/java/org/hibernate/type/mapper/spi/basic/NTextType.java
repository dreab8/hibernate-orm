/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.mapper.spi.basic;

import org.hibernate.type.descriptor.spi.java.StringTypeDescriptor;
import org.hibernate.type.descriptor.spi.sql.LongNVarcharTypeDescriptor;
import org.hibernate.type.mapper.spi.JdbcLiteralFormatter;

/**
 * A type that maps between {@link java.sql.Types#LONGNVARCHAR LONGNVARCHAR} and {@link String}
 *
 * @author Gavin King,
 * @author Bertrand Renuart
 * @author Steve Ebersole
 */
public class NTextType extends BasicTypeImpl<String> {
	public static final NTextType INSTANCE = new NTextType();

	public NTextType() {
		super( StringTypeDescriptor.INSTANCE, LongNVarcharTypeDescriptor.INSTANCE );
	}

	public String getName() { 
		return "ntext";
	}

	@Override
	public JdbcLiteralFormatter<String> getJdbcLiteralFormatter() {
		// no literal support for LONGNVARCHAR
		return StringTypeDescriptor.INSTANCE.getJdbcLiteralFormatter();
	}
}
