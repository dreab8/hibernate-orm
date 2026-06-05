/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cut;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CompositeType;

/**
 * @author Gavin King
 */
@Entity
@Table(name = "Trnsctn")
public class Transaction {

	@Id
	@GeneratedValue
	private Long id;

	@Column(length = 100, nullable = false)
	private String description;

	@CompositeType(MonetoryAmountUserType.class)
	@Column(nullable = false)
	private MonetoryAmount value;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public MonetoryAmount getValue() {
		return value;
	}

	public void setValue(MonetoryAmount value) {
		this.value = value;
	}

}
