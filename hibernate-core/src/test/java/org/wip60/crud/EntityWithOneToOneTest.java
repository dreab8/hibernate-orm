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
package org.wip60.crud;

import java.util.Calendar;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.junit.Test;

import org.hamcrest.CoreMatchers;
import org.wip60.gambit.EntityWithOneToOne;
import org.wip60.gambit.SimpleEntity;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Andrea Boriero
 */
public class EntityWithOneToOneTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EntityWithOneToOne.class, SimpleEntity.class };
	}


	@Test
	public void testOperations() {
		EntityWithOneToOne entityWithOneToOne = new EntityWithOneToOne( 1, "first", Integer.MAX_VALUE );

		SimpleEntity other = new SimpleEntity(
				2,
				Calendar.getInstance().getTime(),
				null,
				Integer.MAX_VALUE,
				Long.MAX_VALUE,
				null
		);

		entityWithOneToOne.setOther( other );
		doInHibernate( this::sessionFactory, session -> {

			session.save( other );
		} );

		doInHibernate( this::sessionFactory, session -> {
			session.save( entityWithOneToOne );
		} );

		doInHibernate(
				this::sessionFactory,
				session -> {
					final EntityWithOneToOne loaded = session.get( EntityWithOneToOne.class, 1 );
					assert loaded != null;
					assertThat( loaded.getName(), equalTo( "first" ) );
					assert loaded.getOther() != null;
					assertThat( loaded.getOther().getId(), equalTo( 2 ) );
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					final SimpleEntity loaded = session.get( SimpleEntity.class, 2 );
					assert loaded != null;
					assertThat( loaded.getSomeInteger(), equalTo( Integer.MAX_VALUE ) );
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					final String value = session.createQuery(
							"select e.name from EntityWithOneToOne e where e.other.id = 2",
							String.class
					).uniqueResult();
					assertThat( value, equalTo( "first" ) );
				}
		);

		doInHibernate(
				this::sessionFactory,				session -> {
					final EntityWithOneToOne loaded = session.get( EntityWithOneToOne.class, 1 );
					assert loaded != null;
					assert loaded.getOther() != null;
					session.remove( loaded );
				}
		);

		doInHibernate(
				this::sessionFactory,				session -> {
					final EntityWithOneToOne notfound = session.find( EntityWithOneToOne.class, 1 );
					assertThat( notfound, CoreMatchers.nullValue() );
				}
		);

		doInHibernate(
				this::sessionFactory,				session -> {
					final SimpleEntity simpleEntity = session.find( SimpleEntity.class, 2 );
					assertThat( simpleEntity, notNullValue() );
				}
		);
	}
}
