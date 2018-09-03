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
package org.wip60;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;
import javax.persistence.SecondaryTable;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;

/**
 * @author Andrea Boriero
 */
public class HierarchyTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				A.class,
				B.class,
				C.class,
				D.class
		};
	}

	@Test
	public void testIt() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					C c = new C();
					c.setAge( 12 );
					c.setCreateDate( new Date() );
					c.setName( "Bob" );
					session.persist( c );
					session.flush();
					session.clear();
					c = session.get( C.class, c.getId() );
					assertNotNull( c.getCreateDate() );
					assertNotNull( c.getName() );
				}
		);

	}

	@Test
	public void testIt_3() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					C c = new C();
					c.setAge( 12 );
					c.setCreateDate( new Date() );
					c.setName( "Bob" );
					session.persist( c );

					D d = new D();
					d.setName( "name" );
					d.setCreateDate( new Date() );
					session.persist( d );
				}
		);

	}

	@Test
	public void testIt2() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					B b = new B();
					b.setCreateDate( new Date() );
					b.setName( "Bob" );
					session.persist( b );
					session.flush();
					session.clear();
					b = session.get( B.class, b.getId() );
					assertNotNull( b.getCreateDate() );
					assertNotNull( b.getName() );
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

	@Entity
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class B extends A {
		@Id
		@GeneratedValue
		private Integer id;
		@Column(nullable = false)
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

	@Entity
//	@DiscriminatorValue("C")
	@SecondaryTable(name = "C")
	public static class C extends B {
		@Column(table = "C")
		private int age;


		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}
	}

	@Entity
	public static class D extends B {
		private int prop;

		public int getProp() {
			return prop;
		}

		public void setProp(int prop) {
			this.prop = prop;
		}
	}


}
