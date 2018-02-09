/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.LinkedHashSet;

import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;

/**
 * @author Andrea Boriero
 */
public class PersistentOrderedSetDescriptorImpl extends PersistentSetDescriptorImpl {
	public PersistentOrderedSetDescriptorImpl(
			Property bootProperty,
			ManagedTypeDescriptor runtimeContainer,
			RuntimeModelCreationContext context) {
		super( bootProperty, runtimeContainer, context );
	}

	@Override
	public Object instantiateRaw(int anticipatedSize) {
		return anticipatedSize > 0
				? new LinkedHashSet( anticipatedSize )
				: new LinkedHashSet();
	}
}
