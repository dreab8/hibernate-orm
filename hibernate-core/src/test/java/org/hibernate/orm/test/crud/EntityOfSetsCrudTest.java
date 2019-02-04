/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.crud;

import java.util.Collection;
import java.util.List;

import org.hibernate.Hibernate;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.domain.gambit.Component;
import org.hibernate.testing.orm.domain.gambit.EntityOfSets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class EntityOfSetsCrudTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				EntityOfSets.class,
		};
	}

	@BeforeEach
	public void cleanUpTestData() {
		inTransaction(
				session -> {
					// select-and-delete to cascade deletes
					final List<EntityOfSets> results = session.createQuery( "select e from EntityOfSets e", EntityOfSets.class ).list();
					for ( EntityOfSets result : results ) {
						session.delete( result );
					}
				}
		);
	}

	@Test
	public void testOperations() {
		final EntityOfSets entity = new EntityOfSets( 1 );

		entity.getSetOfBasics().add( "first string" );
		entity.getSetOfBasics().add( "second string" );

		entity.getSetOfComponents().add(
				new Component(
						5,
						10L,
						15,
						"component string",
						new Component.Nested(
								"first nested string",
								"second nested string"
						)
				)
		);

		inTransaction( session -> {
			session.save( entity );
		} );

		inSession(
				session -> {
					final Integer value = session.createQuery( "select e.id from EntityOfSets e", Integer.class ).uniqueResult();
					assert value == 1;
				}
		);

		inSession(
				session -> {
					final EntityOfSets loaded = session.get( EntityOfSets.class, 1 );
					assert loaded != null;
					checkExpectedSize( loaded.getSetOfBasics(), 2 );
				}
		);

		inSession(
				session -> {
					final List<EntityOfSets> list = session.byMultipleIds( EntityOfSets.class )
							.multiLoad( 1, 2 );
					assert list.size() == 1;
					final EntityOfSets loaded = list.get( 0 );
					assert loaded != null;
					checkExpectedSize( loaded.getSetOfBasics(), 2 );
				}
		);
	}

	private void checkExpectedSize(Collection collection, int expectedSize) {
		if ( ! Hibernate.isInitialized( collection ) ) {
			Hibernate.initialize( collection );
		}

		assert Hibernate.isInitialized( collection );

		if ( collection.size() != expectedSize ) {
			fail(
					"Expecting Collection of size `" + expectedSize +
							"`, but passed Collection has `" + collection.size() + "` entries"
			);
		}

	}


	@Test
	public void testEagerOperations() {
		final EntityOfSets entity = new EntityOfSets( 1 );

		entity.getSetOfBasics().add( "first string" );
		entity.getSetOfBasics().add( "second string" );

		entity.getSetOfComponents().add(
				new Component(
						5,
						10L,
						15,
						"component string",
						new Component.Nested(
								"first nested string",
								"second nested string"
						)
				)
		);

		inTransaction( session -> {
			session.save( entity );
		} );

		inTransaction(
				session -> {
					final Integer value = session.createQuery( "select e.id from EntityOfSets e", Integer.class ).uniqueResult();
					assert value == 1;
				}
		);

		inTransaction(
				session -> {
					final EntityOfSets loaded = session.createQuery( "select e from EntityOfSets e left join fetch e.setOfBasics", EntityOfSets.class ).uniqueResult();
					assert loaded != null;
					checkExpectedSize( loaded.getSetOfBasics(), 2 );
				}
		);

		inTransaction(
				session -> {
					final EntityOfSets loaded = session.createQuery( "select e from EntityOfSets e inner join fetch e.setOfBasics", EntityOfSets.class ).uniqueResult();
					assert loaded != null;
					checkExpectedSize( loaded.getSetOfBasics(), 2 );
				}
		);

		inTransaction(
				session -> {
					final EntityOfSets loaded = session.createQuery( "select e from EntityOfSets e left join fetch e.setOfComponents", EntityOfSets.class ).uniqueResult();
					assert loaded != null;
					checkExpectedSize( loaded.getSetOfComponents(), 1 );
				}
		);

		inTransaction(
				session -> {
					final EntityOfSets loaded = session.createQuery( "select e from EntityOfSets e inner join fetch e.setOfComponents", EntityOfSets.class ).uniqueResult();
					assert loaded != null;
					checkExpectedSize( loaded.getSetOfComponents(), 1 );
				}
		);

		inTransaction(
				session -> {
					final EntityOfSets loaded = session.get( EntityOfSets.class, 1 );
					assert loaded != null;
					checkExpectedSize( loaded.getSetOfBasics(), 2 );
				}
		);

		inTransaction(
				session -> {
					final List<EntityOfSets> list = session.byMultipleIds( EntityOfSets.class )
							.multiLoad( 1, 2 );
					assert list.size() == 1;
					final EntityOfSets loaded = list.get( 0 );
					assert loaded != null;
					checkExpectedSize( loaded.getSetOfBasics(), 2 );
				}
		);
	}
}
