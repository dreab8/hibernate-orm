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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.junit.After;
import org.junit.Test;

import org.wip60.gambit.EntityWithManyToOneWithoutJoinTable;
import org.wip60.gambit.EntityWithOneToManyNotOwned;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;

/**
 * @author Chris Cranford
 */
public class EntityWithOneToManyWithoutJoinTableTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{EntityWithOneToManyNotOwned.class, EntityWithManyToOneWithoutJoinTable.class };
	}

	@After
	public void tearDown() {
		inTransaction(
				session -> {
					final EntityWithOneToManyNotOwned loaded = session.get(
							EntityWithOneToManyNotOwned.class,
							1
					);

					List<EntityWithManyToOneWithoutJoinTable> children = loaded.getChildren();
					children.forEach( child -> session.remove( child ) );
					session.remove( loaded );
				}
		);
	}


	@Test
	public void testSave() {
		EntityWithOneToManyNotOwned owner = new EntityWithOneToManyNotOwned();
		owner.setId( 1 );

		EntityWithManyToOneWithoutJoinTable child1 = new EntityWithManyToOneWithoutJoinTable( 2, Integer.MAX_VALUE );
		EntityWithManyToOneWithoutJoinTable child2 = new EntityWithManyToOneWithoutJoinTable( 3, Integer.MIN_VALUE );
		owner.addChild( child1 );
		owner.addChild( child2 );

		inTransaction(
				session -> {
					session.save( child1 );
					session.save( child2 );
					session.save( owner );
				} );

		inTransaction(
				session -> {
					EntityWithOneToManyNotOwned retrieved = session.get( EntityWithOneToManyNotOwned.class, 1 );
					assertThat( retrieved, notNullValue() );
					List<EntityWithManyToOneWithoutJoinTable> children = retrieved.getChildren();

					assertFalse(
							Hibernate.isInitialized( children )
					);
					assertThat( children.size(), is( 2 ) );

					Map<Integer, EntityWithManyToOneWithoutJoinTable> othersById = new HashMap<>();
					for ( EntityWithManyToOneWithoutJoinTable child : children ) {
						othersById.put( child.getId(), child );
					}

					assertThat( othersById.get( 2 ).getSomeInteger(), is( Integer.MAX_VALUE ) );
					assertThat( othersById.get( 3 ).getSomeInteger(), is( Integer.MIN_VALUE ) );
				} );
	}

	@Test
	public void testSaveWithoutChildren() {
		EntityWithOneToManyNotOwned owner = new EntityWithOneToManyNotOwned();
		owner.setId( 1 );

		inTransaction(
				session -> {
					session.save( owner );
				} );

		inTransaction(
				session -> {
					EntityWithOneToManyNotOwned retrieved = session.get( EntityWithOneToManyNotOwned.class, 1 );
					assertThat( retrieved, notNullValue() );
					List<EntityWithManyToOneWithoutJoinTable> children = retrieved.getChildren();

					assertFalse(
							Hibernate.isInitialized( children )
					);
					assertThat( children.size(), is( 0 ) );
				} );
	}

	@Test
	public void testUpdate() {
		EntityWithOneToManyNotOwned owner = new EntityWithOneToManyNotOwned();
		owner.setId( 1 );

		EntityWithManyToOneWithoutJoinTable child1 = new EntityWithManyToOneWithoutJoinTable( 2, Integer.MAX_VALUE );

		owner.addChild( child1 );

		inTransaction(
				session -> {
					session.save( child1 );
					session.save( owner );
				} );

		EntityWithManyToOneWithoutJoinTable child2 = new EntityWithManyToOneWithoutJoinTable( 3, Integer.MIN_VALUE );
		System.out.println( "=========================================================" );
		inTransaction(
				session -> {
					EntityWithOneToManyNotOwned retrieved = session.get( EntityWithOneToManyNotOwned.class, 1 );
					retrieved.addChild( child2 );
					session.save( child2 );
				} );
		System.out.println( "=========================================================" );

		inTransaction(
				session -> {
					EntityWithOneToManyNotOwned retrieved = session.get( EntityWithOneToManyNotOwned.class, 1 );
					assertThat( retrieved, notNullValue() );
					List<EntityWithManyToOneWithoutJoinTable> children = retrieved.getChildren();

					assertFalse(
							Hibernate.isInitialized( children )
					);
					assertThat( children.size(), is( 2 ) );

					Map<Integer, EntityWithManyToOneWithoutJoinTable> othersById = new HashMap<>();
					for ( EntityWithManyToOneWithoutJoinTable child : children ) {
						othersById.put( child.getId(), child );
					}

					assertThat( othersById.get( 2 ).getSomeInteger(), is( Integer.MAX_VALUE ) );
					assertThat( othersById.get( 3 ).getSomeInteger(), is( Integer.MIN_VALUE ) );
				} );
	}
}
