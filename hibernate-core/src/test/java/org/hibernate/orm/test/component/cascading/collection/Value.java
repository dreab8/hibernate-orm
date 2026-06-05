/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component.cascading.collection;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.id.IncrementGenerator;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "`Value`")
public class Value {
	@Id
	@GenericGenerator(type = IncrementGenerator.class)
	@Column(name = "ID")
	private Long id;

	@ManyToOne
	@JoinColumn(name = "DEF_ID")
	private Definition definition;

	@Embedded
	private LocalizedStrings localizedStrings = new LocalizedStrings();

	protected Value() {
	}

	public Value(Definition definition) {
		this();
		this.definition = definition;
		definition.getValues().add( this );
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Definition getDefinition() {
		return definition;
	}

	public void setDefinition(Definition definition) {
		this.definition = definition;
	}

	public LocalizedStrings getLocalizedStrings() {
		return localizedStrings;
	}

	public void setLocalizedStrings(LocalizedStrings localizedStrings) {
		this.localizedStrings = localizedStrings;
	}
}
