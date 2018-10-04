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

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Andrea Boriero
 */
public class EntityWithOneToOneJoinTableTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Child.class, Parent.class };
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return false;
	}

	@Before
	public void setUp() {
		doInHibernate(
				this::sessionFactory, session -> {
					Child child = new Child( 2 );
					child.setName( "Acme" );
					Child child2 = new Child( 3 );
					child.setName( "Acme 2" );
					Parent parent = new Parent( 1, "Hibernate", child);
					session.save( parent );
					session.save( child );
					session.save( child2 );
				}
		);
	}

	@Test
	public void testGetParent() {
		doInHibernate(
				this::sessionFactory, session -> {
					final Parent parent = session.get( Parent.class, 1 );
//					assertThat( parent.getChild(), CoreMatchers.notNullValue() );
//					assertThat( parent.getChild().getName(), CoreMatchers.notNullValue() );
				} );
	}

	@Test
	public void testGetChild() {
		doInHibernate(
				this::sessionFactory, session -> {
					final Child child = session.get( Child.class, 2 );
					/*
					select
        entitywith0_.id as id1_0_0_,
        entitywith0_.name as name2_0_0_,
        entitywith0_1_.parent_id as parent_i0_2_0_,
        entitywith1_.id as id1_1_1_,
        entitywith1_.description as descript2_1_1_,
        entitywith1_1_.child_id as child_id1_2_1_,
        entitywith2_.id as id1_0_2_,
        entitywith2_.name as name2_0_2_,
        entitywith2_1_.parent_id as parent_i0_2_2_
    from
        Child entitywith0_
    left outer join
        PARENT_CHILD entitywith0_1_
            on entitywith0_.id=entitywith0_1_.child_id
    left outer join
        Parent entitywith1_
            on entitywith0_1_.parent_id=entitywith1_.id
    left outer join
        PARENT_CHILD entitywith1_1_
            on entitywith1_.id=entitywith1_1_.parent_id
    left outer join
        Child entitywith2_
            on entitywith1_1_.child_id=entitywith2_.id
    left outer join
        PARENT_CHILD entitywith2_1_
            on entitywith2_.id=entitywith2_1_.child_id
    where
        entitywith0_.id=?
					 */
					assertThat( child.getParent(), CoreMatchers.notNullValue() );
					assertThat( child.getParent().getDescription(), CoreMatchers.notNullValue() );
				} );
	}

	@Test
	public void testOneToOne() {
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


		doInHibernate(
				this::sessionFactory, session -> {
					final Parent parent = session.createQuery(
							"SELECT p FROM Parent p JOIN p.child WHERE p.id = :id",
							Parent.class
					)
							.setParameter( "id", 1 )
							.getSingleResult();

					assertThat( parent.getChild1(), CoreMatchers.notNullValue() );
					String name = parent.getChild1().getName();
					assertThat( name, CoreMatchers.notNullValue() );
				}
		);

	}

	@Entity(name = "Parent")
	public static class Parent {
		private Integer id;

		private String description;
		private Child child1;
		private Child child2;

		Parent() {
		}

		public Parent(Integer id, String description, Child child) {
			this.id = id;
			this.description = description;
			this.child1 = child;
			child.setParent( this );
			this.child2 = child;
			child.setParent2( this );
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
		@JoinTable(name = "PARENT_CHILD", inverseJoinColumns = @JoinColumn(name = "child_id_1"), joinColumns = @JoinColumn(name = "parent_id"))
		public Child getChild1() {
			return child1;
		}

		public void setChild1(Child other) {
			this.child1 = other;
		}

		@OneToOne
		@JoinTable(name = "PARENT_CHILD_2", inverseJoinColumns = @JoinColumn(name = "child_id_2"), joinColumns = @JoinColumn(name = "parent_id"))
		public Child getChild2() {
			return child2;
		}

		public void setChild2(Child other) {
			this.child2 = other;
		}

	}


	@Entity(name = "Child")
	public static class Child {
		private Integer id;

		private String name;
		private Parent parent;
		private Parent parent2;

		Child() {
		}

		Child(Integer id) {
			this.id = id;
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

		@OneToOne(mappedBy = "child1")
		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		@OneToOne(mappedBy = "child2")
		public Parent getParent2() {
			return parent2;
		}

		public void setParent2(Parent parent2) {
			this.parent2 = parent2;
		}
	}
}
