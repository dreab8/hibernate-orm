/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.util.Comparator;

import org.hibernate.internal.util.compare.RowVersionComparator;

/**
 * @author Andrea Boriero
 */
public class RowVersionJavaDescriptor extends PrimitiveByteArrayJavaDescriptor {

	@Override
	public Comparator<byte[]> getComparator() {
		return RowVersionComparator.INSTANCE;
	}
}
