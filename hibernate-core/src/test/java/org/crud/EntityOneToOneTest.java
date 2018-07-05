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
package org.crud;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Chris Cranford
 */
public class EntityOneToOneTest extends BaseCoreFunctionalTestCase {

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private Integer id;
		@OneToOne(mappedBy = "parent", fetch = FetchType.LAZY)
		private Child child;

		private String parentDescription;

		Parent() {

		}

		Parent(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Child getChild() {
			return child;
		}

		public void setChild(Child child) {
			this.child = child;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Parent parent = (Parent) o;
			return Objects.equals( id, parent.id );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id );
		}
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		private Integer id;
		@OneToOne()
		private Parent parent;

		private String description;

		Child() {

		}

		Child(Integer id, Parent parent) {
			this.id = id;
			this.parent = parent;
			this.parent.setChild( this );
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Child child = (Child) o;
			return Objects.equals( id, child.id ) &&
					Objects.equals( parent, child.parent );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, parent );
		}
	}


	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{Parent.class , Child.class };
	}


	@Test
	public void testOneToOne() {
		doInHibernate( this::sessionFactory, session -> {
			Parent parent = new Parent( 1 );
			Child child = new Child( 2, parent );
			session.save( parent );
			session.save( child );
		} );

		doInHibernate( this::sessionFactory, session -> {
			Parent load = session.get(
					Parent.class,
					1
			);
			load.getChild().getId();

		} );

//		doInHibernate( this::sessionFactory, session -> {
////			Parent parent = session.createQuery(
////						"SELECT p FROM Parent p JOIN p.child WHERE p.id = :id",
////						Parent.class
////					)
////					.setParameter( "id", 1 )
////					.getSingleResult();
//			/*
//			select
//				entityonet0_.id as id1_1_
//			from
//				Parent entityonet0_
//					inner join
//				Child entityonet1_
//				on entityonet0_.id=entityonet1_.parent_id
//			where
//			entityonet0_.id=?
//
//
//			select
//				entityonet0_.id as id1_0_1_,
//		        entityonet0_.description as descript2_0_1_,
//				entityonet0_.parent_id as parent_i2_0_1_,
//
//				entityonet1_.id as id1_1_0_
//				entityonet1_.parentDescription as parentDe2_1_0_
//			from
//				Child entityonet0_
//			left outer join
//				Parent entityonet1_
//					on entityonet0_.parent_id=entityonet1_.id
//			where
//				entityonet0_.parent_id=?
//			*/
//
////			assertThat( parent.getChild(), CoreMatchers.notNullValue() );
//		} );
	}
}
