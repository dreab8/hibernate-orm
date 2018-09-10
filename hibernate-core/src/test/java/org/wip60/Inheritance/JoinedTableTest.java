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
 * A discriminator column is not required for this mapping strategy.
 *
 * When using polymorphic queries, the base class table must be joined with all subclass tables to fetch every associated subclass instance.
 *
 * The joined table inheritance polymorphic queries can use several JOINS which might affect performance when fetching a large number of entities.
 */
public class JoinedTableTest extends BaseCoreFunctionalTestCase {
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
	public void testPolymorphicQuery() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					/*
					select
						joinedtabl0_.id as id1_0_,
						joinedtabl0_.name as name2_0_,
						joinedtabl0_1_.age as age1_1_,
						joinedtabl0_2_.prop as prop1_2_,
						joinedtabl0_3_.prop as prop1_3_,
						case
							when joinedtabl0_1_.id is not null then 1
							when joinedtabl0_2_.C_ID is not null then 2
							when joinedtabl0_3_.id is not null then 3
							when joinedtabl0_.id is not null then 0 end
						as clazz_
					from
						A joinedtabl0_
						left outer join B joinedtabl0_1_ on joinedtabl0_.id=joinedtabl0_1_.id
						left outer join C joinedtabl0_2_ on joinedtabl0_.id=joinedtabl0_2_.C_ID
						left outer join D joinedtabl0_3_ on joinedtabl0_.id=joinedtabl0_3_.id
					 */
					session.createQuery( "from A a" );
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
						joinedtabl0_.id as id1_0_0_,
						joinedtabl0_1_.name as name2_0_0_,
						joinedtabl0_.age as age1_1_0_
					from
						B joinedtabl0_
					inner join
						A joinedtabl0_1_
							on joinedtabl0_.id=joinedtabl0_1_.id
					where
						joinedtabl0_.id=?
					 */
					session.get( B.class, b.getId() );
				}
		);
	}

	@Entity(name = "A")
	@Inheritance(strategy = InheritanceType.JOINED)
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
