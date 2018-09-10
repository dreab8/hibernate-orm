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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.PrimaryKeyJoinColumn;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Andrea Boriero
 *
 * An option is to map only the concrete classes of an inheritance hierarchy to tables.
 * This is called the table-per-concrete-class strategy.
 *
 * When using polymorphic queries, a UNION is required to fetch the base class table along with all subclass tables as well.
 * Polymorphic queries require multiple UNION queries, so be aware of the performance implications of a large class hierarchy.
 */
public class TablePerClassTest extends BaseCoreFunctionalTestCase {
	private B b;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				A.class,
				B.class,
				C.class,
				D.class
		};
	}

	@Before
	public void setUp() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					b = new B( "Bob" );
					session.persist( b );
					A a = new A( "Fab" );
					session.persist( a );
				}
		);
	}

	@Test
	public void testLoadSuperclass() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					/*
					select
							tablepercl0_.id as id1_0_0_,
							tablepercl0_.name as name2_0_0_,
							tablepercl0_.age as age1_1_0_,
							tablepercl0_.prop as prop1_2_0_,
							tablepercl0_.prop as prop1_3_0_,
							tablepercl0_.clazz_ as clazz_0_
						from
							( select
								id,
								name,
								null as age,
								null as prop,
								0 as clazz_
							from
								A
							union
							all select
								id,
								name,
								age,
								null as prop,
								1 as clazz_
							from
								B
							union
							all select
								id,
								name,
								null as age,
								prop,
								2 as clazz_
							from
								C
							union
							all select
								id,
								name,
								null as age,
								prop,
								3 as clazz_
							from
								D
						) tablepercl0_
					where
						tablepercl0_.id=?
					 */
					session.get( A.class, b.getId() );
				}
		);
	}

	@Test
	public void testLoadSubclass() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					/*
					select
						tablepercl0_.id as id1_0_0_,
						tablepercl0_.name as name2_0_0_,
						tablepercl0_.age as age1_1_0_
					from
						B tablepercl0_
					where
						tablepercl0_.id=?
					 */
					session.get( B.class, b.getId() );
				}
		);
	}

	@Entity(name = "A")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static class A { // can be abstract
		@Id
		@GeneratedValue
		private Integer id;
		@Column(nullable = false)
		private String name;

		public A() {
		}

		public A(String name) {
			this.name = name;
		}

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

	@Entity(name = "B")
	public static class B extends A {
		private int age;

		public B() {
			super();
		}

		public B(String name) {
			super( name );
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}
	}

	@Entity(name = "C")
	@PrimaryKeyJoinColumn(name = "C_ID")
	public static class C extends A {
		private int prop;

		public C() {
			super();
		}

		public C(String name) {
			super( name );
		}

		public int getProp() {
			return prop;
		}

		public void setProp(int prop) {
			this.prop = prop;
		}
	}

	@Entity(name = "D")
	public static class D extends A {
		private int prop;

		public D() {
			super();
		}

		public D(String name) {
			super( name );
		}

		public int getProp() {
			return prop;
		}

		public void setProp(int prop) {
			this.prop = prop;
		}
	}
}
