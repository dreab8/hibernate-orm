/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.internal.java.managed.identifier;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.metamodel.Type;

import org.hibernate.type.descriptor.spi.java.managed.IdentifierDescriptor;
import org.hibernate.type.descriptor.spi.java.managed.IdentifierDescriptorBuilder;

/**
 * @author Andrea Boriero
 */
public class IdentifierDescriptorBuilderNonAggregatedCompositeImpl implements IdentifierDescriptorBuilder {
	private Class idClassType;
	private Map<String, Type> ids = new HashMap<>();

	public IdentifierDescriptorBuilderNonAggregatedCompositeImpl setIdClassType(Class idClassType) {
		this.idClassType = idClassType;
		return this;
	}

	public IdentifierDescriptorBuilderNonAggregatedCompositeImpl addId(String attributeName, Type attrubuteType) {
		ids.putIfAbsent( attributeName, attrubuteType );
		return this;
	}

	@Override
	public IdentifierDescriptor build() {
		return new IdentifierDescriptorNonAggregatedComposite( idClassType, ids );
	}
}
