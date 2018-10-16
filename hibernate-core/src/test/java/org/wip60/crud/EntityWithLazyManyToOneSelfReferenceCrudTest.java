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


import java.util.List;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.wip60.gambit.EntityWithLazyManyToOneSelfReference;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Andrea Boriero
 */
public class EntityWithLazyManyToOneSelfReferenceCrudTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EntityWithLazyManyToOneSelfReference.class };
	}

	@Before
	public void setUp() {
		final EntityWithLazyManyToOneSelfReference entity1 = new EntityWithLazyManyToOneSelfReference(
				1,
				"first",
				Integer.MAX_VALUE
		);
		final EntityWithLazyManyToOneSelfReference entity2 = new EntityWithLazyManyToOneSelfReference(
				2,
				"second",
				Integer.MAX_VALUE,
				entity1
		);

		doInHibernate( this::sessionFactory, session -> {
			session.save( entity1 );
			session.save( entity2 );
		} );
	}

	@After
	public void tearDown() {
		doInHibernate( this::sessionFactory, session -> {
						   final EntityWithLazyManyToOneSelfReference loaded = session.get(
								   EntityWithLazyManyToOneSelfReference.class,
								   2
						   );
						   session.remove( loaded.getOther() );
						   session.remove( loaded );
					   }
		);
	}

	@Test
	public void testGet() {
		doInHibernate( this::sessionFactory, session -> {
						   final EntityWithLazyManyToOneSelfReference loaded = session.get(
								   EntityWithLazyManyToOneSelfReference.class,
								   1
						   );
						   assert loaded != null;
						   assertThat( loaded.getName(), equalTo( "first" ) );
						   assertThat( loaded.getOther(), nullValue() );
					   }
		);

		doInHibernate( this::sessionFactory, session -> {
						   final EntityWithLazyManyToOneSelfReference loaded = session.get(
								   EntityWithLazyManyToOneSelfReference.class,
								   2
						   );
						   assert loaded != null;
						   assertThat( loaded.getName(), equalTo( "second" ) );
						   assert loaded.getOther() != null;
						   assertThat( loaded.getOther().getName(), equalTo( "first" ) );
					   }
		);

	}

	@Test
	public void testByMultipleIds() {
		doInHibernate( this::sessionFactory, session -> {
						   final List<EntityWithLazyManyToOneSelfReference> list = session.byMultipleIds(
								   EntityWithLazyManyToOneSelfReference.class )
								   .multiLoad( 1, 3 );
						   assert list.size() == 1;
						   final EntityWithLazyManyToOneSelfReference loaded = list.get( 0 );
						   assert loaded != null;
						   assertThat( loaded.getName(), equalTo( "first" ) );
					   }
		);

		doInHibernate( this::sessionFactory, session -> {
						   final List<EntityWithLazyManyToOneSelfReference> list = session.byMultipleIds(
								   EntityWithLazyManyToOneSelfReference.class )
								   .multiLoad( 2, 3 );
						   assert list.size() == 1;
						   final EntityWithLazyManyToOneSelfReference loaded = list.get( 0 );
						   assert loaded != null;
						   assertThat( loaded.getName(), equalTo( "second" ) );
						   assert loaded.getOther() != null;
						   assertThat( loaded.getOther().getName(), equalTo( "first" ) );
					   }
		);
	}

	@Test
	public void testHqlSelect() {
		// todo (6.0) : the restriction here uses the wrong table alias...
		doInHibernate( this::sessionFactory, session -> {
						   final String value = session.createQuery(
								   "select e.name from EntityWithLazyManyToOneSelfReference e where e.other.name = 'first'",
								   String.class
						   ).uniqueResult();
						   assertThat( value, equalTo( "second" ) );
					   }
		);
	}

}
