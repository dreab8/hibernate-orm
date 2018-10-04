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
package org.wip60.crud.OneToOne.bidirectional;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.Hibernate;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
public class EntityWithBidirectionalOneToOneJoinTableTest extends BaseCoreFunctionalTestCase {


	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Parent.class, Child.class, Child2.class };
	}


	@Before
	public void setUp() {
		doInHibernate(
				this::sessionFactory, session -> {
					Parent parent = new Parent( 1, "Hibernate" );
					Child child = new Child( 2, parent );
					child.setName( "Acme" );
					Child2 child2 = new Child2( 3, parent );
					child2.setName( "Fab" );
					session.save( parent );
					session.save( child );
					session.save( child2 );
				} );
	}

	@After
	public void tearDown() {
		doInHibernate(
				this::sessionFactory, session -> {
					session.createQuery( "delete from Parent" ).executeUpdate();
					session.createQuery( "delete from Child" ).executeUpdate();
					session.createQuery( "delete from Child2" ).executeUpdate();
				} );
	}

	@Test
	public void testGetParent() {
		doInHibernate(
				this::sessionFactory, session -> {
					final Parent parent = session.get( Parent.class, 1 );
					Child child = parent.getChild();
					assertThat( child, CoreMatchers.notNullValue() );
					assertTrue(
							"The child eager OneToOne association is not initialized",
							Hibernate.isInitialized( child )
					);
					assertThat( child.getName(), equalTo( "Acme" ) );

					Child2 child2 = parent.getChild2();
					assertThat( child2, CoreMatchers.notNullValue() );
					assertTrue(
							"The child2 eager OneToOne association is not initialized",
							Hibernate.isInitialized( child2 )
					);
					assertThat( child2.getName(), equalTo( "Fab" ) );
				} );
	}

	@Test
	public void testGetChild() {
		doInHibernate(
				this::sessionFactory, session -> {
			final Child child = session.get( Child.class, 2 );
			Parent parent = child.getParent();
			assertThat( parent, CoreMatchers.notNullValue() );
			assertTrue(
					"The parent eager OneToOne association is not initialized",
					Hibernate.isInitialized( parent )
			);
			assertThat( parent.getDescription(), CoreMatchers.notNullValue() );
			Child child1 = parent.getChild();
			assertThat( child1, CoreMatchers.notNullValue() );
			assertTrue(
					"The child eager OneToOne association is not initialized",
					Hibernate.isInitialized( child1 )
			);
			Child2 child2 = parent.getChild2();
			assertThat( child2, CoreMatchers.notNullValue() );
			assertTrue(
					"The child2 eager OneToOne association is not initialized",
					Hibernate.isInitialized( child2 )
			);
		} );
	}

	@Test
	public void testHqlSelectChild() {
		doInHibernate(
				this::sessionFactory, session -> {
					final String queryString = "SELECT c FROM Child c JOIN c.parent d WHERE d.id = :id";
					final Child child = session.createQuery( queryString, Child.class )
							.setParameter( "id", 1 )
							.getSingleResult();

					assertThat( child.getParent(), CoreMatchers.notNullValue() );

					String description = child.getParent().getDescription();
					assertThat( description, CoreMatchers.notNullValue() );
				}
		);
	}

	@Test
	public void testHqlSelectParent() {
		doInHibernate(
				this::sessionFactory, session -> {
					final Parent parent = session.createQuery(
							"SELECT p FROM Parent p JOIN p.child WHERE p.id = :id",
							Parent.class
					)
							.setParameter( "id", 1 )
							.getSingleResult();

					Child child = parent.getChild();
					assertThat( child, CoreMatchers.notNullValue() );
					assertTrue(
							"the child have to be initialized",
							Hibernate.isInitialized( child )
					);
					String name = child.getName();
					assertThat( name, CoreMatchers.notNullValue() );
				}

		);
	}

	@Entity(name = "Parent")
	@Table(name = "PARENT")
	public static class Parent {
		private Integer id;

		private String description;
		private Child child;
		private Child2 child2;

		Parent() {
		}

		public Parent(Integer id, String description) {
			this.id = id;
			this.description = description;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		@OneToOne
		@JoinTable(name = "PARENT_CHILD", inverseJoinColumns = @JoinColumn(name = "child_id"), joinColumns = @JoinColumn(name = "parent_id"))
		public Child getChild() {
			return child;
		}

		public void setChild(Child other) {
			this.child = other;
		}

		@OneToOne
		@JoinTable(name = "PARENT_CHILD2", inverseJoinColumns = @JoinColumn(name = "child_id"), joinColumns = @JoinColumn(name = "parent_id"))
		public Child2 getChild2() {
			return child2;
		}

		public void setChild2(Child2 child2) {
			this.child2 = child2;
		}
	}

	@Entity(name = "Child")
	@Table(name = "CHILD")
	public static class Child {
		private Integer id;

		private String name;
		private Parent parent;

		Child() {
		}

		Child(Integer id, Parent parent) {
			this.id = id;
			this.parent = parent;
			this.parent.setChild( this );
		}

		@Id
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

		@OneToOne(mappedBy = "child")
		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}

	@Entity(name = "Child2")
	@Table(name = "CHILD2")
	public static class Child2 {
		private Integer id;

		private String name;
		private Parent parent;

		Child2() {
		}

		Child2(Integer id, Parent child) {
			this.id = id;
			this.parent = child;
			this.parent.setChild2( this );
		}

		@Id
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

		@OneToOne(mappedBy = "child2")
		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}
}
