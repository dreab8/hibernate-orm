/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.ops;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@Jpa(
		annotatedClasses = {
				RemoveOrderingTest.Person.class, RemoveOrderingTest.Company.class
		},
		integrationSettings = { @Setting(name = AvailableSettings.JPA_VALIDATION_MODE, value = "NONE") }
)
public class RemoveOrderingTest {

	@Test
	@TestForIssue(jiraKey = "HHH-8550")
	@FailureExpected(jiraKey = "HHH-8550")
	public void testManyToOne(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					try {
						Company company = new Company( 1, "acme" );
						Person person = new Person( 1, "joe", company );
						entityManager.persist( person );
						entityManager.flush();

						entityManager.remove( company );
						entityManager.remove( person );
						entityManager.flush();

						entityManager.persist( person );
						entityManager.flush();

						entityManager.getTransaction().commit();
					}
					catch (Exception e) {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

	@Entity(name = "Company")
	@Table(name = "COMPANY")
	public static class Company {
		@Id
		public Integer id;
		public String name;

		public Company() {
		}

		public Company(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Person")
	@Table(name = "PERSON")
	public static class Person {
		@Id
		public Integer id;
		public String name;
		@ManyToOne(cascade = CascadeType.ALL, optional = false)
		@JoinColumn(name = "EMPLOYER_FK")
		public Company employer;

		public Person() {
		}

		public Person(Integer id, String name, Company employer) {
			this.id = id;
			this.name = name;
			this.employer = employer;
		}
	}
}
