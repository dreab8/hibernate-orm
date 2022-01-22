/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.compliance;

import java.util.Set;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(
		annotatedClasses = CriteriaGetParametersTest.Person.class,
		properties = @Setting( name = AvailableSettings.JPA_QUERY_COMPLIANCE, value = "true")
)
public class CriteriaGetParametersTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.persist( new Person( 1, "Andrea", 5 ) );
					entityManager.persist( new Person( 2, "Andrea", 35 ) );
				}
		);
	}

	@Test
	public void testGetParameters(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

					final CriteriaQuery<Person> query = criteriaBuilder.createQuery( Person.class );
					final Root<Person> person = query.from( Person.class );

					query.select( person );
					query.where( criteriaBuilder.equal(  person.get( "age" ), 30 )  );

					final Set<ParameterExpression<?>> parameters = query.getParameters();

					entityManager.createQuery( query ).getResultList();
					assertThat( parameters, notNullValue() );
					assertTrue( parameters.isEmpty() );
				}
		);
	}

	@Entity(name = "Person")
	@Table(name = "PERSON_TABLE")
	public static class Person {

		@Id
		private Integer id;

		private String name;

		private Integer age;

		Person() {
		}

		public Person(Integer id, String name, Integer age) {
			this.id = id;
			this.name = name;
			this.age = age;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
