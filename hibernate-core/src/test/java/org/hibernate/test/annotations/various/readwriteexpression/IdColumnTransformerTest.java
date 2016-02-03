/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.various.readwriteexpression;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class IdColumnTransformerTest extends BaseCoreFunctionalTestCase {

	private Entity1 e1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Entity1.class, Entity2.class};
	}

	@Override
	public void prepareTest() {
		Session s = openSession();
		s.getTransaction().begin();
		try {
			e1 = new Entity1();
			e1.id = 10;
			e1.field2 = 10;
			e1.field3 = 10;

			Entity2 e2 = new Entity2();
			e1.entity2.add( e2 );
			e2.e1 = e1;

			Entity2 e22 = new Entity2();
			e1.entity2.add( e22 );
			e22.e1 = e1;

			s.save( e1 );
			s.getTransaction().commit();
		}
		catch (Exception e) {
			if ( s.getTransaction().getStatus() == TransactionStatus.ACTIVE ) {
				s.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			s.close();
		}
	}

	@Override
	public void cleanupTest() {
		Session s = openSession();
		s.getTransaction().begin();
		try {
			Object merge = s.merge( e1 );
			s.delete( merge );
			s.getTransaction().commit();
		}
		catch (Exception e) {
			if ( s.getTransaction().getStatus() == TransactionStatus.ACTIVE ) {
				s.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			s.close();
		}
	}

	@Test
	public void testObjectRetrieval() {
		Session s = openSession();
		s.getTransaction().begin();
		try {
			Entity1 e1 = s.get( Entity1.class, 5 );

			assertNotNull( "Object not retrieved ", e1 );
			assertThat( e1.id, is( 5 ) );
			assertThat( e1.field2, is( 2 ) );
			assertThat( e1.field3, is( 10 ) );
			s.getTransaction().commit();
//			assertThat( e1.entity2.size(), is(2) );
		}
		catch (Exception e) {
			if ( s.getTransaction().getStatus() == TransactionStatus.ACTIVE ) {
				s.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			s.close();
		}
	}

	@Test
	public void testObjectRetrieval2() {
		Session s = openSession();
		try {
			Query query = s.createQuery( "select e from Entity1 e where e.id = ?" );
			query.setParameter( 0, 5 );
			Entity1 e1 = (Entity1) query.uniqueResult();

			assertNotNull( "Object not retrieved ", e1 );
			assertThat( e1.id, is( 5 ) );
			assertThat( e1.field2, is( 2 ) );
			assertThat( e1.field3, is( 10 ) );
		}
		finally {
			s.close();
		}
	}

	@Entity(name = "Entity1")
	@Table(name = "Entity_1")
	public static class Entity1 {
		@Id
		@ColumnTransformer(read = "ID / 2")
		@Column(name = "ID")
		private int id;

		@ColumnTransformer(read = "FIELD_2 / 5")
		@Column(name = "FIELD_2")
		private int field2;

		@Column(name = "FIELD_3")
		private int field3;

		@OneToMany(mappedBy = "e1", cascade = CascadeType.ALL, orphanRemoval = true)
		private Set<Entity2> entity2 = new HashSet<Entity2>();
	}

	@Entity(name = "Entity2")
	@Table(name = "Entity_2")
	private static class Entity2 {

		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		private Entity1 e1;
	}
}
