/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.wip60.crud;


import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import org.wip60.gambit.EntityOfDynamicComponent;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Chris Cranford
 */
public class EntityWithDynamicComponentTest extends BaseCoreFunctionalTestCase {
	@Override
	protected String[] getMappings() {
		return new String[] { "crud/EntityOfDynamicComponent.hbm.xml" };
	}

	@Override
	protected String getBaseForMappings() {
		return "org/wip60/";
	}

	@Test
	public void testDynamicComponentLifecycle() {
		// Create entity

		doInHibernate( this::sessionFactory, session -> {
			final EntityOfDynamicComponent entity = new EntityOfDynamicComponent();
			entity.setId( 1L );
			entity.setNote( "Initial Commit" );
			entity.getValuesWithProperties().put( "prop1", 50 );
			entity.getValuesWithProperties().put( "prop2", "Initial String" );
			session.save( entity );
		} );

		// Test entity was saved properly
		doInHibernate( this::sessionFactory, session -> {
			final EntityOfDynamicComponent entity = session.find( EntityOfDynamicComponent.class, 1L );
			assertThat( entity, notNullValue() );
			assertThat( entity.getNote(), is( "Initial Commit" ) );
			// this is only size = 1 because of $type$
			// this is only size = 3 because of inclusion of $type$
			assertThat( entity.getValuesWithProperties().size(), is( 2 ) );
			assertThat( entity.getValuesWithProperties().get( "prop1" ), is( 50 ) );
			assertThat( entity.getValuesWithProperties().get( "prop2" ), is( "Initial String" ) );
		} );

		// todo (6.0) - it seems isDirty is seeing the state of the HashMaps not changing
		// 		so right now updates won't work in terms of validating that the values change.

//		// Update entity
//		sessionFactoryScope().inTransaction( session -> {
//			final EntityOfDynamicComponent entity = session.find( EntityOfDynamicComponent.class, 1 );
//			entity.setNote( "Updated Note" );
//			entity.getValues().put( "v2", 30 );
//			entity.getValuesWithProperties().remove( "prop1" );
//			entity.getValuesWithProperties().put( "prop1", 75 );
//			session.update( entity );
//		} );
//
//		// Test entity was updated properly
//		sessionFactoryScope().inTransaction( session -> {
//			final EntityOfDynamicComponent entity = session.find( EntityOfDynamicComponent.class, 1 );
//			assertThat( entity, notNullValue() );
//			assertThat( entity.getNote(), is( "Updated Note" ) );
//			// this is only size = 1 because of $type$
//			assertThat( entity.getValues().size(), is( 1 ) );
//			// this is only size = 3 because of inclusion of $type$
//			assertThat( entity.getValuesWithProperties().size(), is( 3 ) );
//			assertThat( entity.getValuesWithProperties().get( "prop1" ), is( 75 ) );
//			assertThat( entity.getValuesWithProperties().get( "prop2" ), is( "Initial String" ) );
//		} );

		// Delete entity
		doInHibernate( this::sessionFactory, session -> {
			session.delete( session.find( EntityOfDynamicComponent.class, 1L ) );
		} );

		// Test entity was deleted properly
		doInHibernate( this::sessionFactory, session -> {
			final EntityOfDynamicComponent entity = session.find( EntityOfDynamicComponent.class, 1L );
			assertThat( entity, nullValue() );
		} );
	}
}
