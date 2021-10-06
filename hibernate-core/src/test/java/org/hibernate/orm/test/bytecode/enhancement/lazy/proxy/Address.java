/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity(name = "Address")
@Table(name = "address")
public class Address {
	private Integer id;

	private String text;

	public Address() {
	}

	public Address(Integer id, String text) {
		this.id = id;
		this.text = text;
	}

	@Id
	@Column(name = "oid")
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
