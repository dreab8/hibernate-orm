/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.tool.schemacreation.idgenerator;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.tool.schemacreation.BaseSchemaCreationTestCase;

import org.hibernate.testing.junit5.RequiresDialect;

/**
 * @author Andrea Boriero
 */
@RequiresDialect(dialectClass = H2Dialect.class)
public abstract class AbstractGenerationTest extends BaseSchemaCreationTestCase {

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	protected boolean isCommandGenerated(List<String> commands, String expectedCommnad) {
		final Pattern pattern = Pattern.compile( expectedCommnad.toLowerCase() );
		for ( String command : commands ) {
			Matcher matcher = pattern.matcher( command.toLowerCase() );
			if ( matcher.matches() ) {
				return true;
			}
		}
		return false;
	}
}
