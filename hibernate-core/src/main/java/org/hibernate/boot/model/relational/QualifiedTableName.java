/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.relational;

import org.hibernate.boot.model.naming.Identifier;

/**
 * @author Steve Ebersole
 *
 * @deprecated since 6.0, use {@link org.hibernate.naming.QualifiedTableName} instead.
 */
@Deprecated
public class QualifiedTableName extends org.hibernate.naming.QualifiedTableName {

	public QualifiedTableName(org.hibernate.naming.Identifier catalogName, org.hibernate.naming.Identifier schemaName, org.hibernate.naming.Identifier tableName) {
		super( catalogName, schemaName, tableName );
	}

	public QualifiedTableName(Namespace.Name namespaceName, org.hibernate.naming.Identifier tableName) {
		super( namespaceName, tableName );
	}
	public Identifier getTableName() {
		return (Identifier) getObjectName();
	}
}
