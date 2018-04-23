/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.relational;


import org.hibernate.naming.Identifier;

/**
 * @author Steve Ebersole
 *
 * @deprecated since 6.0, use {@link org.hibernate.naming.QualifiedSequenceName} instead.
 */
@Deprecated
public class QualifiedSequenceName extends org.hibernate.naming.QualifiedSequenceName {

	public QualifiedSequenceName(org.hibernate.naming.Identifier catalogName, org.hibernate.naming.Identifier schemaName, org.hibernate.naming.Identifier sequenceName) {
		super( catalogName, schemaName, sequenceName );
	}
	public QualifiedSequenceName(Namespace.Name schemaName, Identifier sequenceName) {
		super( schemaName, sequenceName );
	}

	public org.hibernate.boot.model.naming.Identifier getSequenceName() {
		return (org.hibernate.boot.model.naming.Identifier) getObjectName();
	}
}
