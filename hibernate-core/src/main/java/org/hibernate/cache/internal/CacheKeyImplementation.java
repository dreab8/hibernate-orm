/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.internal;

import java.io.Serializable;

import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.type.spi.Type;

/**
 * Allows multiple entity classes / collection roles to be stored in the same cache region. Also allows for composite
 * keys which do not properly implement equals()/hashCode().
 *
 * This was named org.hibernate.cache.spi.CacheKey in Hibernate until version 5.
 * Temporarily maintained as a reference while all components catch up with the refactoring to the caching interfaces.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
final class CacheKeyImplementation implements Serializable {
	private final Object id;
	private final Type type;
	private final String entityOrRoleName;
	private final String tenantId;
	private final int hashCode;

	/**
	 * Construct a new key for a collection or entity instance.
	 * Note that an entity name should always be the root entity
	 * name, not a subclass entity name.
	 *
	 * @param id The identifier associated with the cached data
	 * @param type The Hibernate type mapping
	 * @param entityOrRoleName The entity or collection-role name.
	 * @param tenantId The tenant identifier associated this data.
	 */
	CacheKeyImplementation(
			final Object id,
			final Type type,
			final String entityOrRoleName,
			final String tenantId) {
		this.id = id;
		this.type = type;
		this.entityOrRoleName = entityOrRoleName;
		this.tenantId = tenantId;
		this.hashCode = calculateHashCode( type );
	}

	private int calculateHashCode(Type type) {
		int result = type.getHashCode( id );
		result = 31 * result + ( tenantId != null ? tenantId.hashCode() : 0 );
		return result;
	}

	public Object getId() {
		return id;
	}

	@Override
	public boolean equals(Object other) {
		if ( other == null ) {
			return false;
		}
		if ( this == other ) {
			return true;
		}
		if ( hashCode != other.hashCode() || !( other instanceof CacheKeyImplementation) ) {
			//hashCode is part of this check since it is pre-calculated and hash must match for equals to be true
			return false;
		}
		final CacheKeyImplementation that = (CacheKeyImplementation) other;
		return EqualsHelper.equals( entityOrRoleName, that.entityOrRoleName )
				&& type.isEqual( id, that.id)
				&& EqualsHelper.equals( tenantId, that.tenantId );
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		// Used to be required for OSCache
		return entityOrRoleName + '#' + id.toString();
	}
}
