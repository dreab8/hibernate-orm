/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.collectionelement;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitCollectionTableNameSource;
import org.hibernate.boot.model.naming.ImplicitJoinColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;

public class MyImprovedNamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl {
	@Override
	public Identifier determineCollectionTableName(ImplicitCollectionTableNameSource source) {
		// This impl uses the owner entity table name instead of the JPA entity name when
		// generating the implicit name.
		final String name = source.getOwningPhysicalTableName().getText()
				+ '_'
				+ transformAttributePath( source.getOwningAttributePath() );

		return toIdentifier( name, source.getBuildingContext() );
	}

	@Override
	public Identifier determineJoinColumnName(ImplicitJoinColumnNameSource source) {
		final String name = source.getReferencedTableName() + "_" + source.getReferencedColumnName();
		return toIdentifier( name, source.getBuildingContext() );
	}
}
