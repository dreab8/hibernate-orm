/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.ops;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

/**
 * @author Emmanuel Bernard
 */
@Jpa(annotatedClasses = {
		Mammal.class,
		Reptile.class,
		Animal.class
})
public class FindTest {
	@Test
	public void testSubclassWrongId(EntityManagerFactoryScope scope) {
		Mammal mammal = new Mammal();
		mammal.setMamalNbr( 2 );
		mammal.setName( "Human" );
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();
						entityManager.persist( mammal );
						entityManager.flush();
						Assertions.assertNull( entityManager.find( Reptile.class, 1l ) );
						entityManager.getTransaction().rollback();
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

	@Test
	@TestForIssue( jiraKey = "HHH-9856" )
	public void testNonEntity(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					entityManager.getTransaction().begin();
					try {
						entityManager.find( String.class, 1 );
						Assertions.fail( "Expecting a failure" );
					}
					catch (IllegalArgumentException ignore) {
						// expected
					}
				}
		);
	}
}
