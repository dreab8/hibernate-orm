/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cut;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CompositeType;

/**
 * @author Rob.Hasselbaum
 */
@Entity
public class MutualFund {

	@Id
	@GeneratedValue
	private Long id;

	@CompositeType(MonetoryAmountUserType.class)
	@AttributeOverride(name = "amount", column = @Column(name = "amount_millions", nullable = false))
	@ColumnTransformer(forColumn = "amount_millions", read = "amount_millions * 1000000.0", write = "? / 1000000.0")
	@Column(nullable = false)
	private MonetoryAmount holdings;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public MonetoryAmount getHoldings() {
		return holdings;
	}

	public void setHoldings(MonetoryAmount holdings) {
		this.holdings = holdings;
	}

}
