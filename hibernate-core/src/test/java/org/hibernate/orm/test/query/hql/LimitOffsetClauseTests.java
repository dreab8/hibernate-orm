/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: Apache License, Version 2.0
 * See the LICENSE file in the root directory or visit http://www.apache.org/licenses/LICENSE-2.0
 */
package org.hibernate.orm.test.query.hql;

import java.util.Calendar;
import java.util.List;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.domain.gambit.SimpleEntity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test various forms of selections
 *
 * @author Christian Beikov
 */
public class LimitOffsetClauseTests extends SessionFactoryBasedFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				SimpleEntity.class,
		};
	}

	@Test
	public void testParameterOffset() {
		inTransaction(
				session -> {
					List results = session.createQuery( "select o from SimpleEntity o offset :param" ).setParameter(
							"param",
							2
					).list();
					assertThat( results.size(), is( 0 ) );
				} );
	}

	@BeforeEach
	public void setUp() {
		inTransaction(
				session -> {
					SimpleEntity entity = new SimpleEntity(
							1,
							Calendar.getInstance().getTime(),
							null,
							Integer.MAX_VALUE,
							Long.MAX_VALUE,
							null
					);
					session.save( entity );

					SimpleEntity second_entity = new SimpleEntity(
							2,
							Calendar.getInstance().getTime(),
							null,
							Integer.MIN_VALUE,
							Long.MAX_VALUE,
							null
					);
					session.save( second_entity );

				} );
	}

	@AfterEach
	public void tearDown() {
		inTransaction(
				session -> {
					session.createQuery( "from SimpleEntity e" )
							.list()
							.forEach( simpleEntity -> session.delete( simpleEntity ) );
				} );
	}

}
