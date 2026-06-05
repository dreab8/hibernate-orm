/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component.cascading.collection;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.id.IncrementGenerator;

/**
 * @author Steve Ebersole
 */
@Entity
public class Definition {
	@Id
	@GenericGenerator(type = IncrementGenerator.class)
	@Column(name = "ID")
	private Long id;

	@OneToMany(mappedBy = "definition", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	private Set<Value> values = new HashSet<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Set<Value> getValues() {
		return values;
	}

	public void setValues(Set<Value> values) {
		this.values = values;
	}
}
