/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.wip60.gambit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.GenericGenerator;

/**
 * @author Andrea Boriero
 */
@Entity
@GenericGenerator(name="increment", strategy = "increment")
public class EntityWithOneToMany {
	private Integer id;

	// alphabetical
	private String name;

	private Set<SimpleEntity> others = new HashSet<>(  );

	private List<SimpleEntity> othersIdentifierBag = new ArrayList<>(  );
	private Integer someInteger;

	EntityWithOneToMany(){}

	public EntityWithOneToMany(Integer id, String name, Integer someInteger) {
		this.id = id;
		this.name = name;
		this.someInteger = someInteger;
	}

	@Id
	public Integer getId() {
		return id;
	}

	@OneToMany
	@org.hibernate.annotations.CollectionId(
			columns = @Column(name = "BAG_ID"),
			type = @org.hibernate.annotations.Type(type = "long"),
			generator = "increment")
	public List<SimpleEntity> getOthersIdentifierBag() {
		return othersIdentifierBag;
	}

	public void setOthersIdentifierBag(List<SimpleEntity> othersIdentifierBag) {
		this.othersIdentifierBag = othersIdentifierBag;
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

	@OneToMany
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

	@Entity(name = "SimpleEntity2")
	public static class SimpleEntity2{
		@Id
		private Long id;
	}
}

