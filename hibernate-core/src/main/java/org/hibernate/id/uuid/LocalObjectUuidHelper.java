/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.id.uuid;

import org.hibernate.type.descriptor.java.internal.UUIDJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class LocalObjectUuidHelper {
	private LocalObjectUuidHelper() {
	}

	public static String generateLocalObjectUuid() {
		return UUIDJavaDescriptor.ToStringTransformer.INSTANCE.transform(
				StandardRandomStrategy.INSTANCE.generateUUID( null )
		);
	}
}
