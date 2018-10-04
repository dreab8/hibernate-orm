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
import org.junit.Before;
import org.junit.Test;

import org.wip60.gambit.EntityWithManyToOneSelfReference;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Steve Ebersole
 */
public class EntityWithManyToOneSelfReferenceCrudTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EntityWithManyToOneSelfReference.class };
	}


	@Before
	public void setUp() {
		final EntityWithManyToOneSelfReference entity1 = new EntityWithManyToOneSelfReference(
				1,
				"first",
				Integer.MAX_VALUE
		);
		final EntityWithManyToOneSelfReference entity2 = new EntityWithManyToOneSelfReference(
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

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	public void testGetEntityWithTheAssociation() {


		doInHibernate( this::sessionFactory, session -> {
						   final EntityWithManyToOneSelfReference loaded = session.get(
								   EntityWithManyToOneSelfReference.class,
								   2
						   );
						   assert loaded != null;
						   assertThat( loaded.getName(), equalTo( "second" ) );
					   }
		);
	}

	@Test
	public void testGetEntityWithutTheAssociatio() {

		doInHibernate( this::sessionFactory, session -> {
						   final EntityWithManyToOneSelfReference loaded = session.get(
								   EntityWithManyToOneSelfReference.class,
								   1
						   );
						   assert loaded != null;
						   assertThat( loaded.getName(), equalTo( "first" ) );
					   }
		);
//
//		doInHibernate( this::sessionFactory, session -> {
//						   final EntityWithManyToOneSelfReference loaded = session.get(
//								   EntityWithManyToOneSelfReference.class,
//								   2
//						   );
//						   assert loaded != null;
//						   assertThat( loaded.getName(), equalTo( "second" ) );
//						   assert loaded.getOther() != null;
//						   assertThat( loaded.getOther().getName(), equalTo( "first" ) );
//					   }
//		);
//
//		doInHibernate( this::sessionFactory, session -> {
//						   final List<EntityWithManyToOneSelfReference> list = session.byMultipleIds(
//								   EntityWithManyToOneSelfReference.class )
//								   .multiLoad( 1, 3 );
//						   assert list.size() != 0;
//						   final EntityWithManyToOneSelfReference loaded = list.get( 0 );
//						   assert loaded != null;
//						   assertThat( loaded.getName(), equalTo( "first" ) );
//					   }
//		);
//
//		doInHibernate( this::sessionFactory, session -> {
//						   final List<EntityWithManyToOneSelfReference> list = session.byMultipleIds(
//								   EntityWithManyToOneSelfReference.class )
//								   .multiLoad( 2, 3 );
//						   assert list.size() != 0;
//						   final EntityWithManyToOneSelfReference loaded = list.get( 0 );
//						   assert loaded != null;
//						   assertThat( loaded.getName(), equalTo( "second" ) );
//						   assert loaded.getOther() != null;
//						   assertThat( loaded.getOther().getName(), equalTo( "first" ) );
//					   }
//		);
//
//		// todo (6.0) : the restriction here uses the wrong table alias...
//		doInHibernate( this::sessionFactory, session -> {
//						   final String value = session.createQuery(
//								   "select e.name from EntityWithManyToOneSelfReference e where e.other.name = 'first'",
//								   String.class
//						   ).uniqueResult();
//						   assertThat( value, equalTo( "second" ) );
//					   }
//		);
	}

}
