/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

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
package org.wip60.Inheritance;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.UnknownEntityTypeException;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Andrea Boriero
 *
 * When using MappedSuperclass, the inheritance is visible in the domain model only,
 * and each database table contains both the base class and the subclass properties.
 *
 * Because the @MappedSuperclass inheritance model is not mirrored at the database level,
 * it’s not possible to use polymorphic queries (fetching subclasses by their base class).
 */
public class MappedSuperclassTest extends BaseCoreFunctionalTestCase {
	private C c;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				A.class,
				B.class,
				C.class
		};
	}

	@Before
	public void setUp() {
		c = new C();
		c.setAge( 12 );
		c.setCreateDate( new Date() );
		doInHibernate(
				this::sessionFactory,
				session -> {

					session.persist( c );
				}
		);
	}

	@Test(expected = UnknownEntityTypeException.class)
	public void testLoadSuperclass() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					session.get( A.class, c.getId() );
				}
		);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPolymorphicQuery() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					session.createQuery( "from A a " ).list().get( 0 );
				}
		);
	}

	@Test
	public void testQuerySubclass() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					/*
					select
						hierarchym0_.id as id1_1_,
						hierarchym0_.createDate as createDa2_1_,
						hierarchym0_.age as age3_1_
					from
						C hierarchym0_
					 */
					session.createQuery( "from C c " ).list().get( 0 );
				}
		);
	}

	@MappedSuperclass
	public static abstract class A {
		@Column(nullable = false)
		private Date createDate;

		public Date getCreateDate() {
			return createDate;
		}

		public void setCreateDate(Date createDate) {
			this.createDate = createDate;
		}
	}

	@Entity(name = "B")
	public static class B extends A {
		@Id
		@GeneratedValue
		private Integer id;

		private String name;


		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "C")
	public static class C extends A {
		@Id
		@GeneratedValue
		private Integer id;

		private int age;

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public Integer getId() {
			return id;
		}
	}

}
