/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.gambit;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * @author Andrea Boriero
 */
@Entity
public class EntityWithOneToMany {
	private Integer id;

	// alphabetical
	private String name;
	private Set<SimpleEntity> others = new HashSet<>(  );
	private Integer someInteger;

	public EntityWithOneToMany() {
	}

	public EntityWithOneToMany(Integer id, String name, Integer someInteger) {
		this.id = id;
		this.name = name;
		this.someInteger = someInteger;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@OneToMany(fetch = FetchType.LAZY)
	public Set<SimpleEntity> getOthers() {
		return others;
	}

	public void setOthers(Set<SimpleEntity> others) {
		this.others = others;
	}

	public Integer getSomeInteger() {
		return someInteger;
	}

	public void setSomeInteger(Integer someInteger) {
		this.someInteger = someInteger;
	}

	public void addOther(SimpleEntity other) {
		others.add( other );
	}
}
