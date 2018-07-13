/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

@Entity
@Table(name = "person")
@IdClass(PersonId.class)
public abstract class Person {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	private int id;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "clientId")
	private int clientId;

	@Column(name = "name")
	private String name;

	public Person() {
		someInitMethod();
	}

	public void someInitMethod() {
		System.out.print( "a" );
	}

	public int getId() {
		return id;
	}

	public int getClientId() {
		return clientId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}