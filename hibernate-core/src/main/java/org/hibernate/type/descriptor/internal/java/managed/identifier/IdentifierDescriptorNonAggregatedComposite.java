/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.internal.java.managed.identifier;

import java.util.Collection;
import java.util.Map;
import javax.persistence.metamodel.Type;

import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.type.descriptor.spi.java.managed.IdentifierDescriptor;

/**
 * @author Andrea Boriero
 */
public class IdentifierDescriptorNonAggregatedComposite implements IdentifierDescriptor {

	private final Class idClassType;
	private final Map<String, Type> ids;

	public IdentifierDescriptorNonAggregatedComposite(
			Class idClassType,
			Map<String, Type> ids) {
		this.idClassType = idClassType;
		this.ids = ids;
	}

	@Override
	public EntityIdentifierNature getNature() {
		return EntityIdentifierNature.NON_AGGREGATED_COMPOSITE;
	}

	public Class getIdClassJavaType(){
		return idClassType;
	}

	public boolean hasIdClass() {
		return idClassType != null;
	}

	public Collection<String> getIdNames(){
		return ids.keySet();
	}

	public Type getType(String name){
		return ids.get( name );
	}
}
