/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.sql.exec.onetoone.bidirectional;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.validator.internal.util.Contracts;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.orm.test.sql.exec.onetoone.bidirectional.EntityWithBidirectionalOneToOneTest.AdoptedChild;
import static org.hibernate.orm.test.sql.exec.onetoone.bidirectional.EntityWithBidirectionalOneToOneTest.Child;
import static org.hibernate.orm.test.sql.exec.onetoone.bidirectional.EntityWithBidirectionalOneToOneTest.Mother;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Chris CranfinitializersCountord
 */
@DomainModel(
		annotatedClasses = {
				Mother.class,
				Child.class,
				AdoptedChild.class
		}
)
@ServiceRegistry
@SessionFactory(statementInspectorClass = SQLStatementInspector.class)
public class EntityWithBidirectionalOneToOneTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Mother mother = new Mother( 1, "Giulia" );

			Child child = new Child( 2, "Luis", mother );

			AdoptedChild adoptedChild = new AdoptedChild( 3, "Fab", mother );

			session.save( mother );
			session.save( child );
			session.save( adoptedChild );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery( "delete from AdoptedChild" ).executeUpdate();
			session.createQuery( "delete from Mother" ).executeUpdate();
			session.createQuery( "delete from Child" ).executeUpdate();
		} );
	}

	@Test
	public void testGetMother(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final Mother mother = session.get( Mother.class, 1 );

			Child child = mother.getProcreatedChild();
			assertThat( child, notNullValue() );
			assertTrue(
					Hibernate.isInitialized( child ),
					"The child eager OneToOne association is not initialized"
			);
			assertThat( child.getName(), is( "Luis" ) );
			assertSame( child.getMother(), mother );

			AdoptedChild adoptedChild = mother.getAdoptedChild();
			assertThat( adoptedChild.getName(), is( "Fab" ) );
			assertTrue(
					Hibernate.isInitialized( adoptedChild ),
					"The adoptedChild eager OneToOne association is not initialized"
			);
			assertThat( adoptedChild.getName(), equalTo( "Fab" ) );

			assertSame( adoptedChild.getStepMother(), mother );
			assertThat( adoptedChild.getBiologicalMother(), is( nullValue() ) );

			statementInspector.assertExecutedCount( 1 );
			statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 3 );

		} );
	}

	@Test
	public void testUnrealCaseWhenMotherIsAlsoStepMother(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Mother mother = new Mother( 4, "Jiny" );

			Child child = new Child( 5, "Carlo", mother );

			AdoptedChild adoptedChild = new AdoptedChild( 6, "Andrea", mother );

			adoptedChild.setBiologicalMother( mother );

			session.save( mother );
			session.save( child );
			session.save( adoptedChild );
		} );

		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final Mother mother = session.get( Mother.class, 4 );

			Child child = mother.getProcreatedChild();
			assertThat( child, notNullValue() );
			assertTrue(
					Hibernate.isInitialized( child ),
					"The child eager OneToOne association is not initialized"
			);
			assertThat( child.getName(), is( "Carlo" ) );
			assertSame( child.getMother(), mother );

			AdoptedChild adoptedChild = mother.getAdoptedChild();
			assertThat( adoptedChild, notNullValue() );
			assertTrue(
					Hibernate.isInitialized( adoptedChild ),
					"The adoptedChild eager OneToOne association is not initialized"
			);
			assertThat( adoptedChild.getName(), equalTo( "Andrea" ) );

			assertSame( adoptedChild.getStepMother(), mother );
			assertSame( adoptedChild.getBiologicalMother(), mother );

			statementInspector.assertExecutedCount( 1 );
			statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 3 );
		} );
	}

	@Test
	public void testGetMother3(SessionFactoryScope scope) {
		scope.inTransaction( session -> {

			Mother parent = new Mother( 4, "Catia" );

			Child child = new Child( 5, "Stefano", parent );

			AdoptedChild adoptedChild = new AdoptedChild( 7, "Luisa", parent );

			Mother biologicalMother = new Mother( 6, "Rebecca" );
			adoptedChild.setBiologicalMother( biologicalMother );

			Child anotherChild = new Child( 8, "Igor", biologicalMother );

			session.save( parent );
			session.save( biologicalMother );
			session.save( child );
			session.save( adoptedChild );
			session.save( anotherChild );
		} );

		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final Mother mother = session.get( Mother.class, 4 );

			assertThat( mother.getName(), equalTo( "Catia" ) );

			Child child = mother.getProcreatedChild();
			assertThat( child, notNullValue() );
			assertTrue(
					Hibernate.isInitialized( child ),
					"The child eager OneToOne association is not initialized"
			);
			assertThat( child.getName(), equalTo( "Stefano" ) );
			assertSame( child.getMother(), mother );

			AdoptedChild child2 = mother.getAdoptedChild();
			assertThat( child2, notNullValue() );
			assertTrue(
					Hibernate.isInitialized( child2 ),
					"The child2 eager OneToOne association is not initialized"
			);
			assertThat( child2.getName(), equalTo( "Luisa" ) );
			assertSame( child2.getStepMother(), mother );

			Mother biologicalMother = child2.getBiologicalMother();
			assertThat( biologicalMother.getId(), equalTo( 6 ) );
			assertThat( biologicalMother.getAdoptedChild(), nullValue() );

			Child anotherChild = biologicalMother.getProcreatedChild();
			assertThat( anotherChild.getId(), equalTo( 8 ) );
			assertThat( anotherChild.getName(), equalTo( "Igor" ) );
			assertSame( anotherChild.getMother(), biologicalMother );

			statementInspector.assertExecutedCount( 3 );
			statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 3 );
			statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 3 );
			statementInspector.assertNumberOfOccurrenceInQuery( 2, "join", 3 );
		} );
	}

	@Test
	public void testGetChild(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final Child child = session.get( Child.class, 2 );

			Mother mother = child.getMother();
			assertTrue(
					Hibernate.isInitialized( mother ),
					"The mother eager OneToOne association is not initialized"
			);
			assertThat( mother, notNullValue() );
			assertThat( mother.getName(), is( "Giulia" ) );

			Child child1 = mother.getProcreatedChild();
			assertSame( child1, child );
			assertTrue(
					Hibernate.isInitialized( child1 ),
					"The child eager OneToOne association is not initialized"
			);

			AdoptedChild adoptedChild = mother.getAdoptedChild();
			assertThat( adoptedChild, notNullValue() );
			assertTrue(
					Hibernate.isInitialized( adoptedChild ),
					"The adoptedChild eager OneToOne association is not initialized"
			);
			assertSame( adoptedChild.getStepMother(), mother );
			assertThat( adoptedChild.getBiologicalMother(), nullValue() );

			statementInspector.assertExecutedCount( 1 );
			statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 3 );
		} );
	}

	@Test
	public void testGetChild2(SessionFactoryScope scope) {
		scope.inTransaction( session -> {

			Mother mother = new Mother( 4, "Giulia" );
			Child child = new Child( 5, "Stefano", mother );

			AdoptedChild child2 = new AdoptedChild( 7, "Fab2", mother );

			Mother biologicalMother = new Mother( 6, "Hibernate OGM" );
			child2.setBiologicalMother( biologicalMother );

			Child child3 = new Child( 8, "Carla", biologicalMother );

			session.save( mother );
			session.save( biologicalMother );
			session.save( child );
			session.save( child2 );
			session.save( child3 );
		} );

		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final Child child = session.get( Child.class, 5 );

			Mother mother = child.getMother();
			Contracts.assertTrue(
					Hibernate.isInitialized( mother ),
					"The mother eager OneToOne association is not initialized"
			);
			assertThat( mother, notNullValue() );
			assertThat( mother.getName(), is( "Giulia" ) );

			Child child1 = mother.getProcreatedChild();
			assertSame( child1, child );
			Contracts.assertTrue(
					Hibernate.isInitialized( child1 ),
					"The child eager OneToOne association is not initialized"
			);

			AdoptedChild adoptedChild = mother.getAdoptedChild();
			assertThat( adoptedChild, notNullValue() );
			Contracts.assertTrue(
					Hibernate.isInitialized( adoptedChild ),
					"The adoptedChild eager OneToOne association is not initialized"
			);

			Assert.assertSame( adoptedChild.getStepMother(), mother );

			Mother biologicalMother = adoptedChild.getBiologicalMother();
			assertThat( biologicalMother, notNullValue() );
			assertThat( biologicalMother.getId(), equalTo( 6 ) );

			Child anotherChild = biologicalMother.getProcreatedChild();
			assertThat( anotherChild, notNullValue() );
			assertThat( anotherChild.getId(), equalTo( 8 ) );

			Assert.assertSame( anotherChild.getMother(), biologicalMother );

			assertThat( biologicalMother.getAdoptedChild(), nullValue() );

			statementInspector.assertExecutedCount( 3 );
			statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 3 );
			statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 3 );
			statementInspector.assertNumberOfOccurrenceInQuery( 2, "join", 3 );
		} );
	}

	@Test
	public void testHqlSelectMother(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					final Mother mother = session.createQuery(
							"SELECT p FROM Mother p JOIN p.procreatedChild WHERE p.id = :id",
							Mother.class
					)
							.setParameter( "id", 1 )
							.getSingleResult();

					Child child = mother.getProcreatedChild();
					assertThat( child, notNullValue() );
					assertThat( child.getName(), is( "Luis" ) );
					statementInspector.assertExecutedCount( 3 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 2 );
					statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 3 );
					statementInspector.assertNumberOfOccurrenceInQuery( 2, "join", 3 );
				}
		);
	}

	@Test
	public void testHqlSelectChild(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					final String queryString = "SELECT c FROM Child c JOIN c.mother d WHERE d.id = :id";
					final Child child = session.createQuery( queryString, Child.class )
							.setParameter( "id", 1 )
							.getSingleResult();

					Mother mother = child.getMother();
					assertThat( mother, notNullValue() );

					assertThat( mother.getName(), is( "Giulia" ) );
					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 2 );
					statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 3 );
				}
		);
	}

	@Entity(name = "Mother")
	public static class Mother {
		@Id
		private Integer id;
		private String name;

		@OneToOne
		private Child procreatedChild;

		@OneToOne(mappedBy = "stepMother")
		private AdoptedChild adoptedChild;

		Mother() {
		}

		public Mother(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		Mother(Integer id) {
			this.id = id;
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

		public Child getProcreatedChild() {
			return procreatedChild;
		}

		public void setProcreatedChild(Child procreatedChild) {
			this.procreatedChild = procreatedChild;
		}

		public AdoptedChild getAdoptedChild() {
			return adoptedChild;
		}

		public void setAdoptedChild(AdoptedChild adoptedChild) {
			this.adoptedChild = adoptedChild;
		}
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		private Integer id;
		private String name;

		@OneToOne(mappedBy = "procreatedChild")
		private Mother mother;

		Child() {

		}

		Child(Integer id, String name, Mother mother) {
			this.id = id;
			this.name = name;
			this.mother = mother;
			this.mother.setProcreatedChild( this );
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

		public Mother getMother() {
			return mother;
		}

		public void setMother(Mother mother) {
			this.mother = mother;
		}
	}

	@Entity(name = "AdoptedChild")
	@Table(name = "ADOPTED_CHILD")
	public static class AdoptedChild {
		@Id
		private Integer id;

		private String name;

		@OneToOne
		private Mother biologicalMother;

		@OneToOne
		private Mother stepMother;

		AdoptedChild() {
		}

		AdoptedChild(Integer id, String name, Mother stepMother) {
			this.id = id;
			this.name = name;
			this.stepMother = stepMother;
			this.stepMother.setAdoptedChild( this );
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

		public Mother getBiologicalMother() {
			return biologicalMother;
		}

		public void setBiologicalMother(Mother biologicalMother) {
			this.biologicalMother = biologicalMother;
		}

		public Mother getStepMother() {
			return stepMother;
		}

		public void setStepMother(Mother stepMother) {
			this.stepMother = stepMother;
		}

		@Override
		public String toString() {
			return "AdoptedChild{" +
					"id=" + id +
					", name='" + name + '\'' +
					'}';
		}
	}
}
