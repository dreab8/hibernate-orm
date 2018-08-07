/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;

/**
 * The "context" in which creation of SQL AST occurs.
 *
 * @author Steve Ebersole
 */
public interface SqlAstCreationContext {
	SessionFactoryImplementor getSessionFactory();

	SqlExpressionResolver getSqlSelectionResolver();

	// todo (6.0) : ultimately the plan is to expose the

	// todo (6.0) : we may instead want to just handle this as a `org.hibernate.sql.ast.consume.spi.SqlAstWalker` impl
	//		rather than during the actual SQL AST creation.  Keep in mind though that we really want
	//		to implement the new feature of supporting "un-fetch joins", probably just via HQL
}
