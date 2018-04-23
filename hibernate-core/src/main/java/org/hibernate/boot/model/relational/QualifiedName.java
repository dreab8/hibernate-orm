/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.relational;

import org.hibernate.boot.model.naming.Identifier;

/**
 * Models the qualified name of a database object.  Some things to keep in
 * mind wrt catalog/schema:<ol>
 *     <li>{@link java.sql.DatabaseMetaData#isCatalogAtStart}</li>
 *     <li>{@link java.sql.DatabaseMetaData#getCatalogSeparator()}</li>
 * </ol>
 * <p/>
 * Also, be careful about the usage of {@link #render}.  If the intention is get get the name
 * as used in the database, the {@link org.hibernate.engine.jdbc.env.spi.JdbcEnvironment} ->
 * {@link org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameFormatter#format} should be
 * used instead.
 *
 * @author Steve Ebersole
 *
 * @deprecated since 6.0, use {@link org.hibernate.naming.spi.QualifiedName} instead.
 */
@Deprecated
public interface QualifiedName extends org.hibernate.naming.spi.QualifiedName {
	Identifier getCatalogName();
	Identifier getSchemaName();
	Identifier getObjectName();

	/**
	 * Returns a String-form of the qualified name.
	 * <p/>
	 * Depending on intention, may not be appropriate.  May want
	 * {@link org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameFormatter#format}
	 * instead.  See {@link org.hibernate.engine.jdbc.env.spi.JdbcEnvironment#getQualifiedObjectNameFormatter}
	 *
	 * @return The string form
	 */
	String render();
}
