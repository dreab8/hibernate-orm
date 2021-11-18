/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.derivedidentities.bidirectional;

import java.util.List;

import org.hibernate.query.Query;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@DomainModel(
		annotatedClasses = {
				Foo.class,
				Bar.class,
				Person.class,
				PersonInfo.class
		}
)
@SessionFactory
public class OneToOneWithDerivedIdentityTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Bar" ).executeUpdate();
					session.createQuery( "delete from Foo" ).executeUpdate();
					session.createQuery( "delete from PersonInfo" ).executeUpdate();
					session.createQuery( "delete from Person" ).executeUpdate();
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11903")
	public void testInsertFooAndBarWithDerivedId(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Bar bar = new Bar();
					bar.setDetails( "Some details" );
					Foo foo = new Foo();
					foo.setBar( bar );
					bar.setFoo( foo );
					session.persist( foo );
					session.flush();

					assertNotNull( foo.getId() );
					assertEquals( foo.getId(), bar.getFoo().getId() );

					session.clear();

					Bar newBar = (Bar) session.createQuery( "SELECT b FROM Bar b WHERE b.foo.id = :id" )
							.setParameter( "id", foo.getId() )
							.uniqueResult();

					assertNotNull( newBar );
					assertNotNull( newBar.getFoo() );
					assertEquals( foo.getId(), newBar.getFoo().getId() );
					assertEquals( "Some details", newBar.getDetails() );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14389")
	public void testQueryById(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Bar bar = new Bar();
					bar.setDetails( "Some details" );
					Foo foo = new Foo();
					foo.setBar( bar );
					bar.setFoo( foo );
					session.persist( foo );
					session.flush();

					assertNotNull( foo.getId() );
					assertEquals( foo.getId(), bar.getFoo().getId() );

					session.clear();

					Bar newBar = (Bar) session.createQuery( "SELECT b FROM Bar b WHERE b.foo = :foo" )
							.setParameter( "foo", foo )
							.uniqueResult();

					assertNotNull( newBar );
					assertNotNull( newBar.getFoo() );
					assertEquals( foo.getId(), newBar.getFoo().getId() );
					assertEquals( "Some details", newBar.getDetails() );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14389")
	public void testFindById(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Bar bar = new Bar();
					bar.setDetails( "Some details" );
					Foo foo = new Foo();
					foo.setBar( bar );
					bar.setFoo( foo );
					session.persist( foo );
					session.flush();

					assertNotNull( foo.getId() );
					assertEquals( foo.getId(), bar.getFoo().getId() );

					session.clear();
					try {
						session.find( Bar.class, foo );
						fail( "Should have thrown IllegalArgumentException" );
					}
					catch (IllegalArgumentException expected) {
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14389")
	public void testFindByPrimaryKey(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Bar bar = new Bar();
					bar.setDetails( "Some details" );
					Foo foo = new Foo();
					foo.setBar( bar );
					bar.setFoo( foo );
					session.persist( foo );
					session.flush();

					assertNotNull( foo.getId() );
					assertEquals( foo.getId(), bar.getFoo().getId() );

					session.clear();

					Bar newBar = session.find( Bar.class, foo.getId() );

					assertNotNull( newBar );
					assertNotNull( newBar.getFoo() );
					assertEquals( foo.getId(), newBar.getFoo().getId() );
					assertEquals( "Some details", newBar.getDetails() );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10476")
	public void testInsertFooAndBarWithDerivedIdPC(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Bar bar = new Bar();
					bar.setDetails( "Some details" );
					Foo foo = new Foo();
					foo.setBar( bar );
					bar.setFoo( foo );
					session.persist( foo );
					session.flush();

					assertNotNull( foo.getId() );
					assertEquals( foo.getId(), bar.getFoo().getId() );

					session.clear();

					Bar barWithFoo = new Bar();
					barWithFoo.setFoo( foo );
					barWithFoo.setDetails( "wrong details" );
					bar = session.get( Bar.class, barWithFoo );

					assertSame( bar, barWithFoo );
					assertEquals( "Some details", bar.getDetails() );
					assertTrue( session.getPersistenceContext().isEntryFor( bar ) );
					assertFalse( session.getPersistenceContext().isEntryFor( bar.getFoo() ) );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6813")
	public void testSelectWithDerivedId(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Bar bar = new Bar();
					bar.setDetails( "Some details" );
					Foo foo = new Foo();
					foo.setBar( bar );
					bar.setFoo( foo );
					session.persist( foo );
					session.flush();

					assertNotNull( foo.getId() );
					assertEquals( foo.getId(), bar.getFoo().getId() );

					session.clear();

					Foo newFoo = (Foo) session.createQuery( "SELECT f FROM Foo f" ).uniqueResult();

					assertNotNull( newFoo );
					assertNotNull( newFoo.getBar() );
					assertSame( newFoo, newFoo.getBar().getFoo() );
					assertEquals( "Some details", newFoo.getBar().getDetails() );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6813")
	// Regression test utilizing multiple types of queries.
	public void testCase(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person p = new Person();
					p.setName( "Alfio" );
					PersonInfo pi = new PersonInfo();
					pi.setId( p );
					pi.setInfo( "Some information" );
					session.persist( p );
					session.persist( pi );

					session.getTransaction().commit();
					session.clear();

					session.getTransaction().begin();

					Query q = session.getNamedQuery( "PersonQuery" );
					List<Person> persons = q.list();
					assertEquals( persons.size(), 1 );
					assertEquals( persons.get( 0 ).getName(), "Alfio" );
					session.getTransaction().commit();
					session.clear();

					session.getTransaction().begin();

					p = session.get( Person.class, persons.get( 0 ).getId() );
					assertEquals( p.getName(), "Alfio" );
				}
		);
	}
}
