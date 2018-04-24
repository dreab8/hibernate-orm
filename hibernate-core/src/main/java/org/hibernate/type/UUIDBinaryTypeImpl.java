/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.UUID;

import org.hibernate.type.descriptor.java.internal.UUIDJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.BinarySqlDescriptor;
import org.hibernate.type.internal.BasicTypeImpl;

/**
 * A type mapping {@link java.sql.Types#BINARY} and {@link UUID}
 *
 * @author Steve Ebersole
 */
public class UUIDBinaryTypeImpl extends BasicTypeImpl<UUID> {
	public static final UUIDBinaryTypeImpl INSTANCE = new UUIDBinaryTypeImpl();

	public UUIDBinaryTypeImpl() {
		super( BinarySqlDescriptor.INSTANCE, UUIDJavaDescriptor.INSTANCE );
	}

	public String getName() {
		return "uuid-binary";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}
}
