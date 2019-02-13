/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.collections.mapkey;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.MapKey;
import javax.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;


/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "IdMapKey")
public class IdMapKeyEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	@ManyToMany
	@MapKey
	private Map<Integer, StrTestEntity> idmap;

	public IdMapKeyEntity() {
		idmap = new HashMap<Integer, StrTestEntity>();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Map<Integer, StrTestEntity> getIdmap() {
		return idmap;
	}

	public void setIdmap(Map<Integer, StrTestEntity> idmap) {
		this.idmap = idmap;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		IdMapKeyEntity that = (IdMapKeyEntity) o;
		return Objects.equals( id, that.id );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id );
	}

	@Override
	public String toString() {
		return "IdMapKeyEntity{" +
				"id=" + id +
				", idmap=" + idmap +
				'}';
	}
}