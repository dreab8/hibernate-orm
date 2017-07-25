/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.query.NativeQuery;
import org.hibernate.query.sql.AttributeResultRegistration;

/**
 * @author Steve Ebersole
 */
public class AttributeResultBuilder
		implements ResultNodeImplementor, NativeQuery.ReturnProperty, AttributeResultRegistration {

	private final String attributeName;
	private List<String> columnAliases;

	public AttributeResultBuilder(String attributeName) {
		this.attributeName = attributeName;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ResultBuilder


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// NativeQuery.ReturnProperty

	@Override
	public NativeQuery.ReturnProperty addColumnAlias(String columnAlias) {
		if ( columnAliases == null ) {
			columnAliases = new ArrayList<>();
		}
		columnAliases.add( columnAlias );
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// AttributeResultRegistration

	@Override
	public List<String> getColumnAliases() {
		return columnAliases == null ? Collections.emptyList() : Collections.unmodifiableList( columnAliases );
	}
}
