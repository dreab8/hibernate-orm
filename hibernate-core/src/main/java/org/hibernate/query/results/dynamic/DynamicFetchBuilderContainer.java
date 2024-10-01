/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.dynamic;

import org.hibernate.query.results.FetchBuilder;
import org.hibernate.sql.results.graph.Fetchable;

/**
 * @author Steve Ebersole
 */
public interface DynamicFetchBuilderContainer {
	/**
	 * Locate an explicit fetch definition for the fetchable
	 */
	default FetchBuilder findFetchBuilder(Fetchable fetchable) {
		return findFetchBuilder( fetchable.getFetchableName() );
	}

	/**
	 * Locate an explicit fetch definition for the named fetchable
	 */
	FetchBuilder findFetchBuilder(String fetchableName);

	/**
	 * Add a property mapped to a single column.
	 */
	default DynamicFetchBuilderContainer addProperty(Fetchable fetchable, String columnAlias) {
		return addProperty( fetchable.getFetchableName(), columnAlias );
	}

	/**
	 * Add a property mapped to a single column.
	 */
	DynamicFetchBuilderContainer addProperty(String fetchableName, String columnAlias);

	/**
	 * Add a property mapped to multiple columns
	 */
	default DynamicFetchBuilderContainer addProperty(Fetchable fetchable, String... columnAliases) {
		return addProperty( fetchable.getFetchableName(), columnAliases );
	}

	/**
	 * Add a property mapped to multiple columns
	 */
	DynamicFetchBuilderContainer addProperty(String fetchableName, String... columnAliases);

	/**
	 * Add a property whose columns can later be defined using {@link DynamicFetchBuilder#addColumnAlias}
	 */
	default DynamicFetchBuilder addProperty(Fetchable fetchable) {
		return addProperty( fetchable.getFetchableName() );
	}

	/**
	 * Add a property whose columns can later be defined using {@link DynamicFetchBuilder#addColumnAlias}
	 */
	DynamicFetchBuilder addProperty(String fetchableName);

	default void addFetchBuilder(Fetchable fetchable, FetchBuilder fetchBuilder) {
		addFetchBuilder( fetchable.getFetchableName(), fetchBuilder );
	}

	void addFetchBuilder(String fetchableName, FetchBuilder fetchBuilder);
}
