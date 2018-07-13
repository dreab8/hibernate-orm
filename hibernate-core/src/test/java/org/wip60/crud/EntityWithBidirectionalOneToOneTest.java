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
package org.wip60.crud;

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
public class EntityWithBidirectionalOneToOneTest extends BaseCoreFunctionalTestCase {

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private Integer id;
		private String description;
		@OneToOne(mappedBy = "parent")
		private Child child;

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

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
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
			return Objects.equals( id, parent.id ) &&
					Objects.equals( description, parent.description );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, description );
		}
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		private Integer id;
		private String name;
		@OneToOne(fetch = FetchType.LAZY)
		private Parent parent;

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

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
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
					Objects.equals( name, child.name ) &&
					Objects.equals( parent, child.parent );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, name, parent );
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Parent.class, Child.class };
	}


	@Test
	public void testOneToOne() {
		doInHibernate( this::sessionFactory, session -> {
			Parent parent = new Parent( 1 );
			parent.setDescription( "Hibernate" );
			Child child = new Child( 2, parent );
			child.setName( "Acme" );
			session.save( parent );
			session.save( child );
		} );

		doInHibernate(
				this::sessionFactory,
				session -> {
					final Child child = session.createQuery(
							"SELECT c FROM Child c JOIN c.parent d WHERE d.id = :id",
							Child.class
					)
							.setParameter( "id", 1 )
							.getSingleResult();
					/*
					triggered by the SingleResult();
						>>> EAGER
							select
								entitywith0_.id as id1_1_0_,
								entitywith0_.description as descript2_1_0_,
								entitywith1_.id as id1_0_1_,
								entitywith1_.name as name2_0_1_,
								entitywith1_.parent_id as parent_i3_0_1_
							from
								Parent entitywith0_
							left outer join
								Child entitywith1_
									on entitywith0_.id=entitywith1_.parent_id
							where
								entitywith0_.id=?

						+

							select
								entitywith0_.id as id1_0_1_,
								entitywith0_.name as name2_0_1_,
								entitywith0_.parent_id as parent_i3_0_1_,
								entitywith1_.id as id1_1_0_,
								entitywith1_.description as descript2_1_0_
							from
								Child entitywith0_
							left outer join
								Parent entitywith1_
									on entitywith0_.parent_id=entitywith1_.id
							where
								entitywith0_.parent_id=?

						>>> LAZY

							select
								entitywith0_.id as id1_0_,
								entitywith0_.name as name2_0_,
								entitywith0_.parent_id as parent_i3_0_
							from
								Child entitywith0_
							inner join
								Parent entitywith1_
									on entitywith0_.parent_id=entitywith1_.id
							where
								entitywith1_.id=?

					 */
//					Parent parent = child.getParent();
//					assertThat( child.getParent(), CoreMatchers.notNullValue() );
					/*
					triggered by child.getParent()
					>>> EAGER
						select
							entitywith0_.id as id1_0_1_,
							entitywith0_.name as name2_0_1_,
							entitywith0_.parent_id as parent_i3_0_1_,
							entitywith1_.id as id1_1_0_,
							entitywith1_.description as descript2_1_0_
						from
							Child entitywith0_
						left outer join
							Parent entitywith1_
								on entitywith0_.parent_id=entitywith1_.id
						where
							entitywith0_.parent_id=?

					>>> LAZY
						NO QUERY
					 */

					String description = child.getParent().getDescription();
					assertThat( description, CoreMatchers.notNullValue() );

					/*
					>>> EAGER
						NO QUERY

					>>> LAZY
						select
							entitywith0_.id as id1_1_0_,
							entitywith0_.description as descript2_1_0_,
							entitywith1_.id as id1_0_1_,
							entitywith1_.name as name2_0_1_,
							entitywith1_.parent_id as parent_i3_0_1_
						from
							Parent entitywith0_
						left outer join
							Child entitywith1_
								on entitywith0_.id=entitywith1_.parent_id
						where
							entitywith0_.id=?
						 */
//					assertThat( description, CoreMatchers.notNullValue() );
				}

		);
//
//
//		doInHibernate(
//				this::sessionFactory,
//				session -> {
//					final Parent parent = session.createQuery(
//							"SELECT p FROM Parent p JOIN p.child WHERE p.id = :id",
//							Parent.class
//					)
//							.setParameter( "id", 1 )
//							.getSingleResult();
//
//					assertThat( parent.getChild(), CoreMatchers.notNullValue() );
//					String name = parent.getChild().getName();
//					assertThat( name, CoreMatchers.notNullValue() );
//				}
//
//		);
//
//		doInHibernate( this::sessionFactory, session -> {
//			final Parent parent = session.get( Parent.class, 1 );
//			assertThat( parent.getChild(), CoreMatchers.notNullValue() );
//			assertThat( parent.getChild().getName(), CoreMatchers.notNullValue() );
//		} );
	}
}
