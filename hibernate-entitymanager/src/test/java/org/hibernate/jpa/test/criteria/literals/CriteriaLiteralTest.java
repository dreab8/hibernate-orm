/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.literals;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.Query;
import org.hibernate.jpa.internal.QueryImpl;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertTrue;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-9576")
public class CriteriaLiteralTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Animal.class};
	}

	@Before
	public void setUp() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			Animal animal = new Animal();
			animal.name = "horse";
			animal.age = 42;

			em.persist( animal );
			em.getTransaction().commit();
		}
		catch (Exception e) {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testPredicateWithString() {
		EntityManager entityManager = getOrCreateEntityManager();

		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Animal> criteriaQuery = criteriaBuilder.createQuery( Animal.class );
		Root<Animal> from = criteriaQuery.from( Animal.class );

		Predicate namePredicate = criteriaBuilder.equal( from.get( "name" ), "horse" );

		criteriaQuery.select( from ).where( namePredicate );
		TypedQuery<Animal> typedQuery = entityManager.createQuery( criteriaQuery );
		Animal singleResult = typedQuery.getSingleResult();

		Query query = typedQuery.unwrap( QueryImpl.class ).getHibernateQuery();
		entityManager.close();
		System.out.println( query.getQueryString() );
		assertTrue( query.getQueryString().contains( "name=:param" ) );
	}

	@Test
	public void testDiff() {
		EntityManager entityManager = getOrCreateEntityManager();

		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

		CriteriaQuery<Integer> criteriaQuery = criteriaBuilder.createQuery( Integer.class );

		criteriaQuery.select( criteriaBuilder.diff( criteriaBuilder.literal( 5 ), criteriaBuilder.literal( 2 ) ) );

		Root<Animal> from = criteriaQuery.from( Animal.class );
		Predicate namePredicate = criteriaBuilder.equal( from.get( "name" ), "horse" );

		criteriaQuery.where( namePredicate );
		TypedQuery<Integer> typedQuery = entityManager.createQuery( criteriaQuery );
		Integer singleResult = typedQuery.getSingleResult();

		Query query = typedQuery.unwrap( QueryImpl.class ).getHibernateQuery();
		entityManager.close();
		System.out.println( query.getQueryString() );
		assertTrue( query.getQueryString().contains( "name=:param" ) );

	}

	@Test
	public void testPredicateWithInteger() {
		EntityManager entityManager = getOrCreateEntityManager();

		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Animal> criteriaQuery = criteriaBuilder.createQuery( Animal.class );
		Root<Animal> from = criteriaQuery.from( Animal.class );

//		Predicate agePredicate = criteriaBuilder.equal( from.get( "age" ), Integer.valueOf( 42 ) );
		Predicate agePredicate = criteriaBuilder.equal(
				from.get( "age" ),
				criteriaBuilder.parameter( Integer.class, "param" )
		);

		criteriaQuery.select( from ).where( agePredicate );
		TypedQuery<Animal> typedQuery = entityManager.createQuery( criteriaQuery );
		typedQuery.setParameter( "param", 42 );
		Animal singleResult = typedQuery.getSingleResult();

		Query query = typedQuery.unwrap( QueryImpl.class ).getHibernateQuery();
		System.out.println( query.getQueryString() );
		entityManager.close();

		assertTrue( query.getQueryString().contains( "age=:param" ) );
	}

	@Entity(name = "Animal")
	public static class Animal {
		@Id
		@GeneratedValue
		Long id;
		String name;
		Integer age;
	}
}
