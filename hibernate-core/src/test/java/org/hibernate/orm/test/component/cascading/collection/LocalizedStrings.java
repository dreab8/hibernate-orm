/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component.cascading.collection;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;

/**
 * @author Steve Ebersole
 */
@Embeddable
public class LocalizedStrings {
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(joinColumns = @JoinColumn(name = "VAL_ID"))
	@MapKeyColumn(name = "LOC")
	@Column(name = "STR_VAL")
	private Map<Locale,String> strings = new HashMap<>();

	public void addString(Locale locale, String value) {
		strings.put( locale, value );
	}

	public String getString(Locale locale) {
		return strings.get( locale );
	}

	public Map<Locale,String> getStringsCopy() {
		return java.util.Collections.unmodifiableMap( strings );
	}
}
