/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemavalidation.matchingtablenames;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.tool.hbm2ddl.SchemaValidator;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;

@TestForIssue(jiraKey = "HHH-10718")
public class TableNamesWithUnderscoreTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				AnEntity.class,
				AnotherEntity.class
		};
	}

	@Test
	public void testSchemaValidationDoesNotFailDueToAMoreThanOneTableFound() {
		new SchemaValidator().validate( metadata() );
	}

	@Entity(name = "AnEntity")
	@Table(name = "AN_ENTITY")
	public static class AnEntity {
		@Id
		@GeneratedValue
		private int id;
	}

	@Entity(name = "AnotherEntity")
	@Table(name = "AN1ENTITY")
	public static class AnotherEntity {
		@Id
		@GeneratedValue
		private int id;
	}
}
