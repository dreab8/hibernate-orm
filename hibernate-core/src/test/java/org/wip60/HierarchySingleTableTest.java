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
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.annotations.DiscriminatorOptions;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;

/**
 * @author Andrea Boriero
 */
public class HierarchySingleTableTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				A.class,
				B.class,
				C.class,
		};
	}

	@Test
	public void testSaveA() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					A a = new A("Bob");

					B b = new B("Fab");
					session.persist( a );
					session.persist( b );
					session.flush();
					session.clear();
					a = session.get( A.class, a.getId() );
					assertNotNull( a.getName() );
				}
		);

	}

	@Test
	public void testSaveB() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					B b = new B("Fab");
					session.persist( b );
					session.flush();
					session.clear();
					b = session.get( B.class, b.getId() );
					assertNotNull( b.getName() );
				}
		);

	}

	@Entity
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "DB_TYPE")
	@DiscriminatorOptions(force = true, insert = false)
	public static class A {
		@Id
		@GeneratedValue
		private Integer id;
		@Column(nullable = false)
		private String name;

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

	@Entity
	@DiscriminatorValue( "B" )
	public static class B extends A {
		private int age;

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

	@Entity
	public static class C extends A {
		private int prop;

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


}
