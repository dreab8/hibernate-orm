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

import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Andrea Boriero
 */
public class ResultSetInfo {
	Set<ResultSet> resultSets = new HashSet<>();

	void addResultSet(ResultSet resultSet) {
		if ( resultSets.contains( resultSet ) ) {
			throw new LeakException( "ResultSet already registered and not closed" );
		}
		resultSets.add( resultSet );
	}

	void close(ResultSet resultSet) {
		if ( !resultSets.contains( resultSet ) ) {
			throw new LeakException( "Trying to close non registered ResultSet" );
		}
		resultSets.remove( resultSet );
	}

	public boolean areAllResultSetsClosed() {
		return resultSets.isEmpty();
	}
}
