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

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Andrea Boriero
 *
 * When omitting an explicit inheritance strategy (e.g. @Inheritance), JPA will choose the SINGLE_TABLE strategy by default.
 *
 * Each subclass in a hierarchy must define a unique discriminator value, which is used to differentiate between rows belonging to separate subclass types.
 * If this is not specified, the DTYPE column is used as a discriminator, storing the associated subclass name.
 *
 * Among all other inheritance alternatives, the single table strategy performs the best since it requires access to one table only.
 * Because all subclass columns are stored in a single table, it’s not possible to use NOT NULL constraints anymore,
 * so integrity checks must be moved either into the data access layer or enforced through CHECK or TRIGGER constraints.
 *
 *  You can also use @DiscriminatorFormula to express in SQL a virtual discriminator column.
 *  This is particularly useful when the discriminator value can be extracted from one or more columns of the table.
 *  Both @DiscriminatorColumn and @DiscriminatorFormula are to be set on the root entity (once per persisted hierarchy).
 *
 *  @org.hibernate.annotations.DiscriminatorOptions allows to optionally specify Hibernate specific discriminator options which are not standardized in JPA.
 *  The available options are force and insert.
 *
 * The force attribute is useful if the table contains rows with extra discriminator values that are not mapped to a persistent class.
 * This could, for example, occur when working with a legacy database.
 * If force is set to true Hibernate will specify the allowed discriminator values in the SELECT query, even when retrieving all instances of the root class.
 *
 * The second option, insert, tells Hibernate whether or not to include the discriminator column in SQL INSERTs. Usually, the column should be part of the INSERT statement,
 * but if your discriminator column is also part of a mapped composite identifier you have to set this option to false.
 *
 * the @DiscriminatorValue can take two additional values:
 *
 * null
 * If the underlying discriminator column is null, the null discriminator mapping is going to be used.
 * [the schema generator sets the discriminator column as not null -- ??? ERROR ???]
 * not null
 * If the underlying discriminator column has a not-null value that is not explicitly mapped to any entity, the not-null discriminator mapping used.
 *
 *
 */
public class SingleTableTest extends BaseCoreFunctionalTestCase {
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
						hierarchys0_.id as id2_0_0_,
						hierarchys0_.name as name3_0_0_,
						hierarchys0_.age as age4_0_0_,
						hierarchys0_.prop as prop5_0_0_,
						hierarchys0_.DB_TYPE as DB_TYPE1_0_0_
					from
						SingleTableTest$A hierarchys0_
					where
						hierarchys0_.id=?
						and hierarchys0_.DB_TYPE in (
							'B', 'SingleTableTest$C'
						)
					 */
					session.get( A.class, b.getId() );
				}
		);
	}

	@Test
	public void testPolymorphicQuery() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					/*
					@DiscriminatorOptions(force = true)
						select
							hierarchys0_.id as id2_0_0_,
							hierarchys0_.name as name3_0_0_,
							hierarchys0_.age as age4_0_0_,
							hierarchys0_.prop as prop5_0_0_,
							hierarchys0_.DB_TYPE as DB_TYPE1_0_0_
						from
							SingleTableTest$A hierarchys0_
						where
							hierarchys0_.DB_TYPE in (
								'B', 'SingleTableTest$C'
							)
					@DiscriminatorOptions(force = true)
						select
							hierarchys0_.id as id2_0_,
							hierarchys0_.name as name3_0_,
							hierarchys0_.age as age4_0_,
							hierarchys0_.prop as prop5_0_,
							hierarchys0_.DB_TYPE as DB_TYPE1_0_
						from
							A hierarchys0_
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
							hierarchys0_.id as id2_0_0_,
							hierarchys0_.name as name3_0_0_,
							hierarchys0_.age as age4_0_0_
						from
							SingleTableTest$A hierarchys0_
						where
							hierarchys0_.id=?
							and hierarchys0_.DB_TYPE='B'
					 */
					session.get( B.class, b.getId() );
				}
		);
	}

	@Test
	public void testLoadSubclassWithNotNull() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					/*
					 */
					session.get( D.class, b.getId() );
				}
		);
	}

	@Entity(name = "A")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
//	@DiscriminatorColumn(name = "DB_TYPE") // default is DTYPE
//	@DiscriminatorOptions(force = true) // "Forces" Hibernate to specify the allowed discriminator values, even when retrieving all instances of the root class.
//	@DiscriminatorValue( "null" )
	//@DiscriminatorFormula( "case when age > 10 then 'B' else 'C' end" )
	public static class A { // it can be abstract
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
	@DiscriminatorValue("B")
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
	@DiscriminatorValue( "C" )
	public static class C extends A {
		private Integer prop;

		public C() {
			super();
		}

		public C(String name) {
			super( name );
		}

		public Integer getProp() {
			return prop;
		}

		public void setProp(Integer prop) {
			this.prop = prop;
		}
	}

	@Entity(name = "D")
	@DiscriminatorValue( "not null" )
	public static class D extends A {
		private Integer prop2;

		public D() {
			super();
		}

		public D(String name) {
			super( name );
		}

		public Integer getProp2() {
			return prop2;
		}

		public void setProp2(Integer prop) {
			this.prop2 = prop;
		}
	}

}
