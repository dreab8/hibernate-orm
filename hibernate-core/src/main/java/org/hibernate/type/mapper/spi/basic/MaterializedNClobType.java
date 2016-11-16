/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.mapper.spi.basic;

import org.hibernate.type.descriptor.spi.java.StringTypeDescriptor;
import org.hibernate.type.descriptor.spi.sql.NClobTypeDescriptor;
import org.hibernate.type.spi.JdbcLiteralFormatter;

/**
 * A type that maps between {@link java.sql.Types#NCLOB NCLOB} and {@link String}
 *
 * @author Gavin King
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class MaterializedNClobType extends BasicTypeImpl<String> {
	public static final MaterializedNClobType INSTANCE = new MaterializedNClobType();

	public MaterializedNClobType() {
		super( StringTypeDescriptor.INSTANCE, NClobTypeDescriptor.DEFAULT );
	}

	public String getName() {
		return "materialized_nclob";
	}

	@Override
	public JdbcLiteralFormatter<String> getJdbcLiteralFormatter() {
		// no literal support for NCLOB
		return null;
	}
}
