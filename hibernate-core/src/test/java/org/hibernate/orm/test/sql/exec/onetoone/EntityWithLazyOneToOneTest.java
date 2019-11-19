/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.sql.exec.onetoone;

import java.util.Calendar;

import org.hibernate.Hibernate;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.domain.gambit.EntityWithLazyOneToOne;
import org.hibernate.testing.orm.domain.gambit.SimpleEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Andrea Boriero
 */
public class EntityWithLazyOneToOneTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				EntityWithLazyOneToOne.class,
				SimpleEntity.class
		};
	}

	@BeforeEach
	public void setUp() {
		EntityWithLazyOneToOne entity = new EntityWithLazyOneToOne( 1, "first", Integer.MAX_VALUE );

		SimpleEntity other = new SimpleEntity(
				2,
				Calendar.getInstance().getTime(),
				null,
				Integer.MAX_VALUE,
				Long.MAX_VALUE,
				null
		);

		entity.setOther( other );

		inTransaction( session -> {
			session.save( other );
			session.save( entity );
		} );
	}

	@AfterEach
	public void tearDown() {
		inTransaction( session -> {
			deleteAll();
		} );
	}

	@Test
	public void testGet() {
		inTransaction(
				session -> {
					final EntityWithLazyOneToOne loaded = session.get( EntityWithLazyOneToOne.class, 1 );
					assert loaded != null;
					assertThat( loaded.getName(), equalTo( "first" ) );
					assertFalse(
							Hibernate.isInitialized( loaded.getOther() ),
							"The lazy association should not be initialized"
					);

					SimpleEntity loadedOther = loaded.getOther();
					assert loadedOther != null;
					assertThat( loaded.getOther().getId(), equalTo( 2 ) );
					assertFalse(
							Hibernate.isInitialized( loaded.getOther() ),
							"getId() should not trigger the lazy association initialization"

					);

					loadedOther.getSomeDate();
					assertTrue(
							Hibernate.isInitialized( loaded.getOther() ),
							"The lazy association should be initialized"
					);

				}
		);

		inTransaction(
				session -> {
					final SimpleEntity loaded = session.get( SimpleEntity.class, 2 );
					assert loaded != null;
					assertThat( loaded.getSomeInteger(), equalTo( Integer.MAX_VALUE ) );
				}
		);
	}

	@Test
	public void testHqlSelect() {

		inTransaction(
				session -> {
					final String value = session.createQuery(
							"select e.name from EntityWithLazyOneToOne e where e.other.id = 2",
							String.class
					).uniqueResult();
					assertThat( value, equalTo( "first" ) );
				}
		);
	}

	private void deleteAll() {

		inTransaction(
				session -> {
					final EntityWithLazyOneToOne loaded = session.get( EntityWithLazyOneToOne.class, 1 );
					assert loaded != null;
					assert loaded.getOther() != null;
					session.remove( loaded );
				}
		);

		inTransaction(
				session -> {
					final EntityWithLazyOneToOne notfound = session.find( EntityWithLazyOneToOne.class, 1 );
					assertThat( notfound, CoreMatchers.nullValue() );
				}
		);

		inTransaction(
				session -> {
					final SimpleEntity simpleEntity = session.find( SimpleEntity.class, 2 );
					assertThat( simpleEntity, notNullValue() );
					session.remove( simpleEntity );
				}
		);
	}
}

