/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ordered;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

import jakarta.persistence.OrderBy;

@Entity
public class Search {
	@Id
	private String searchString;
	@ElementCollection
	@CollectionTable(joinColumns = @JoinColumn(name = "searchString"))
	@Column(name = "text")
	@OrderBy
	private Set<String> searchResults = new HashSet<>();

	Search() {}

	public Search(String string) {
		searchString = string;
	}

	public Set getSearchResults() {
		return searchResults;
	}
	public void setSearchResults(Set searchResults) {
		this.searchResults = searchResults;
	}
	public String getSearchString() {
		return searchString;
	}
	public void setSearchString(String searchString) {
		this.searchString = searchString;
	}
}
