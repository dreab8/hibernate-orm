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
package org.wip60;

import java.util.Calendar;
import java.util.List;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.wip60.gambit.SimpleEntity;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Andrea Boriero
 */
public class SimpleSelectionTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SimpleEntity.class };
	}


	@Test
	public void testSelectEntityAttribute() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					List<Object> results = session.createQuery(
							"select s.someString from SimpleEntity s where s.id = :id" )
							.setParameter( "id", 1 )
							.list();
					assertThat( results.size(), is( 1 ) );
					assertThat( results.get( 0 ), is( "a" ) );
				}
		);
	}

	@Test
	public void testSelectAnIntegerConstant() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					List<Object> results = session.createQuery(
							"select 1 from SimpleEntity s where s.id = :id" )
							.setParameter( "id", 1 )
							.list();
					assertThat( results.size(), is( 1 ) );
					assertThat( results.get( 0 ), is( 1 ) );
				}
		);
	}

	@Test
	public void testSelectACharConstant() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					List<Object> results = session.createQuery(
							"select 'a' from SimpleEntity s where s.id = :id" )
							.setParameter( "id", 1 )
							.list();
					assertThat( results.size(), is( 1 ) );
					assertThat( results.get( 0 ), is( "a" ) );
				}
		);
	}

	@Before
	public void setUp() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					SimpleEntity entity = new SimpleEntity(
							1,
							Calendar.getInstance().getTime(),
							null,
							Integer.MAX_VALUE,
							Long.MAX_VALUE,
							"a"
					);
					session.save( entity );

					SimpleEntity second_entity = new SimpleEntity(
							2,
							Calendar.getInstance().getTime(),
							null,
							Integer.MIN_VALUE,
							Long.MAX_VALUE,
							"b"
					);
					session.save( second_entity );

				}
		);
	}

	@After
	public void tearDown() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					session.createQuery( "from SimpleEntity e" )
							.list()
							.forEach( simpleEntity -> session.delete( simpleEntity ) );
				}
		);
	}
}
