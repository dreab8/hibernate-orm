/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package benchmark;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;

import org.junit.jupiter.api.Test;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * @author Andrea Boriero
 */
public class EntityWithBidirectionalOneToOneJoinTableTest {

	@Test
	public void launchHqlSelectParent() {
		TestState state = new TestState();
		state.before();
		hqlSelectParent( state );
		hqlSelectParent( state );
		state.after();
	}

	@Benchmark
	public Parent getParent(TestState state) {
		return (Parent) state.inTransaction( session -> {
			return session.get( Parent.class, 1 );
		} );
	}

	@Benchmark
	public Child hqlSelectChild(TestState state) {
		return (Child) state.inTransaction( session -> {
			final String queryString = "SELECT c FROM Child c JOIN c.parent d WHERE d.id = :id";
			return session.createQuery( queryString, Child.class )
					.setParameter( "id", 1 )
					.getSingleResult();
		} );
	}

	@Benchmark
	public Parent hqlSelectParent(TestState state) {
		return (Parent) state.inTransaction( session -> {
			final String queryString = "SELECT p FROM Parent p JOIN p.child WHERE p.id = :id";
			return session.createQuery( queryString, Parent.class )
					.setParameter( "id", 1 )
					.getSingleResult();
		} );
	}

	@State(Scope.Thread)
	public static class TestState extends BenchmarkTestBaseSetUp {

		@Override
		protected void setUp() {
			inTransaction( session -> {
				Parent parent = new Parent( 1, "Hibernate" );
				Child child = new Child( 2, "Acme", parent );
				Child2 child2 = new Child2( 3, "Fab", parent );
				session.save( parent );
				session.save( child );
				session.save( child2 );
			} );
		}

		@Override
		protected void tearDown() {
			inTransaction( session -> {
				session.createQuery( "delete from Parent" ).executeUpdate();
				session.createQuery( "delete from Child" ).executeUpdate();
				session.createQuery( "delete from Child2" ).executeUpdate();
			} );
		}

		@Override
		public void applyMetadataSources(MetadataSources metadataSources) {
			metadataSources.addAnnotatedClass( Parent.class );
			metadataSources.addAnnotatedClass( Child.class );
			metadataSources.addAnnotatedClass( Child2.class );
		}
	}

	@Entity(name = "Parent")
	@Table(name = "PARENT")
	public static class Parent {
		@Id
		private Integer id;

		private String description;
		@OneToOne
		@JoinTable(name = "PARENT_CHILD", inverseJoinColumns = @JoinColumn(name = "child_id"), joinColumns = @JoinColumn(name = "parent_id"))
		private Child child;
		@OneToOne
		@JoinTable(name = "PARENT_CHILD2", inverseJoinColumns = @JoinColumn(name = "child_id"), joinColumns = @JoinColumn(name = "parent_id"))
		private Child2 child2;

		Parent() {
		}

		public Parent(Integer id, String description) {
			this.id = id;
			this.description = description;
		}

		public void setChild(Child other) {
			this.child = other;
		}

		public void setChild2(Child2 child2) {
			this.child2 = child2;
		}
	}

	@Entity(name = "Child")
	@Table(name = "CHILD")
	public static class Child {
		@Id
		private Integer id;

		private String name;
		@OneToOne(mappedBy = "child")
		private Parent parent;

		Child() {
		}

		Child(Integer id, String name, Parent parent) {
			this.id = id;
			this.name = name;
			this.parent = parent;
			this.parent.setChild( this );
		}
	}

	@Entity(name = "Child2")
	@Table(name = "CHILD2")
	public static class Child2 {
		@Id
		private Integer id;

		private String name;
		@OneToOne(mappedBy = "child2")
		private Parent parent;

		Child2() {
		}

		Child2(Integer id, String name, Parent child) {
			this.id = id;
			this.name = name;
			this.parent = child;
			this.parent.setChild2( this );
		}
	}
}
