/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.jdbc.leak;

import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Andrea Boriero
 */
public class StatementInfo {
	Set<Statement> statements = new HashSet<>();

	void addOpenStatement(Statement statement) {
		if ( statements.contains( statement ) ) {
			throw new LeakException( "Statement already registered and not closed" );
		}
		statements.add( statement );
	}

	void close(Statement statement) {
		if ( !statements.contains( statement ) ) {
			throw new LeakException( "Trying to close non registered Statement" );
		}
		statements.remove( statement );
	}

	public boolean areAllStatementsClosed(){
		return statements.isEmpty();
	}
}
