/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.persistence.OneToMany;

import org.hibernate.collection.internal.PersistentSortedSet;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * @author Andrea Boriero
 */
public class PersistentSortedSetDescriptorImpl extends PersistentSetDescriptorImpl {

	private final Comparator comparator;

	public PersistentSortedSetDescriptorImpl(
			Property bootProperty,
			ManagedTypeDescriptor runtimeContainer,
			RuntimeModelCreationContext context) {
		super( bootProperty, runtimeContainer, context );
		this.comparator = ( (Collection) bootProperty.getValue() ).getComparator();
	}

	@Override
	public PersistentCollection wrap(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor descriptor,
			Object rawCollection) {
		PersistentSortedSet set = new PersistentSortedSet( session, descriptor, (SortedSet) rawCollection );
		set.setComparator( comparator );
		return set;
	}

	@OneToMany
	public Class getReturnedClass() {
		return java.util.SortedSet.class;
	}

	@Override
	public Object instantiateRaw(int anticipatedSize) {
		return new TreeSet( comparator );
	}

	@Override
	public PersistentCollection instantiateWrapper(
			SharedSessionContractImplementor session,
			PersistentCollectionDescriptor descriptor,
			Serializable key) {
		return new PersistentSortedSet( session, descriptor, key );
	}
}
