/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.UUID;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.internal.UUIDJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.VarcharSqlDescriptor;
import org.hibernate.type.internal.BasicTypeImpl;

/**
 * A type mapping {@link java.sql.Types#CHAR} (or {@link java.sql.Types#VARCHAR}) and {@link java.util.UUID}
 *
 * @author Steve Ebersole
 */
public class UUIDCharTypeImpl extends BasicTypeImpl<UUID> implements LiteralType<UUID> {
	public static final UUIDCharTypeImpl INSTANCE = new UUIDCharTypeImpl();

	public UUIDCharTypeImpl() {
		super( VarcharSqlDescriptor.INSTANCE, UUIDJavaDescriptor.INSTANCE );
	}

	public String getName() {
		return "uuid-char";
	}

	public String objectToSQLString(UUID value, Dialect dialect) throws Exception {
		return StringTypeImpl.INSTANCE.objectToSQLString( value.toString(), dialect );
	}
}
