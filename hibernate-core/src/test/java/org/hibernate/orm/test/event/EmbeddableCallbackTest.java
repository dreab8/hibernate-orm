/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.event;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;

import org.hibernate.boot.MetadataSources;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;

import org.hibernate.testing.TestForIssue;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-12326")
public class EmbeddableCallbackTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Employee.class,
		};
	}

	@Test
	public void test() {
		inTransaction( session -> {
			Employee employee = new Employee();
			employee.details = new EmployeeDetails();
			employee.id = 1;

			session.persist( employee );
		} );

		inTransaction( session -> {
			Employee employee = session.find( Employee.class, 1 );

			assertEquals( "Vlad", employee.name );
			assertEquals( "Developer Advocate", employee.details.jobTitle );

			session.delete( employee );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13110")
	public void testNullEmbeddable() {
		inTransaction( session -> {
			Employee employee = new Employee();
			employee.id = 1;

			session.persist( employee );
		} );

		inTransaction( session -> {
			Employee employee = session.find( Employee.class, 1 );

			assertEquals( "Vlad", employee.name );
			assertTrue( employee.details == null || employee.details.jobTitle == null );

			session.delete( employee );
		} );
	}

	@Entity(name = "Employee")
	public static class Employee {

		@Id
		private Integer id;

		private String name;

		private EmployeeDetails details;

		@PrePersist
		public void setUp() {
			name = "Vlad";
		}
	}

	@Embeddable
	public static class EmployeeDetails {

		private String jobTitle;

		@PrePersist
		public void setUp() {
			jobTitle = "Developer Advocate";
		}
	}
}
