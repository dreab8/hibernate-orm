/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.wip60.crud;

import java.util.Calendar;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import org.wip60.gambit.EntityWithOneToMany;
import org.wip60.gambit.SimpleEntity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Andrea Boriero
 */
public class EntityWithOneToManyCrudTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{EntityWithOneToMany.class, SimpleEntity.class};
	}

	@Test
	public void testSave() {
		EntityWithOneToMany entity = new EntityWithOneToMany( 1, "first", Integer.MAX_VALUE );

		SimpleEntity other = new SimpleEntity(
				2,
				Calendar.getInstance().getTime(),
				null,
				Integer.MAX_VALUE,
				Long.MAX_VALUE,
				null
		);

		entity.addOther( other );

		doInHibernate(this::sessionFactory,
				session -> {
					session.save( other );
				} );

		doInHibernate(this::sessionFactory,
				session -> {
					session.save( entity );
				} );

		doInHibernate(this::sessionFactory,
				session -> {
					EntityWithOneToMany retrieved = session.get( EntityWithOneToMany.class, 1 );
					assertThat( retrieved, notNullValue()  );
				} );

	}
}

