/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.engine.jdbc.group;

import java.sql.PreparedStatement;

import org.hibernate.Incubating;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.group.TableMutation;

/**
 * @author Steve Ebersole
 */
@Incubating
public interface PreparedStatementDetails {
	TableMutation getTableMutation();

	PreparedStatement getStatement();

	Expectation getExpectation();

	int getBaseOffset();
}
