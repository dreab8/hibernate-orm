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
package org.wip60;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
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
public class EntityWithManyToOneNonJoinTableCrudTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EntityWithManyToOneWithoutJoinTable.class, EntityWithOneToManyNotOwned.class };
	}

	@Test
	public void testOperations() {
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

		inTransaction(
				session -> {
					session.save( child2 );
				} );

		inTransaction(
				session -> {
					owner.addChild( child2 );
				} );

		inTransaction(
				session -> {
					EntityWithOneToManyNotOwned retrieved = session.get( EntityWithOneToManyNotOwned.class, 1 );
					assertThat( retrieved, notNullValue() );
					List<EntityWithManyToOneWithoutJoinTable> childs = retrieved.getChildren();

					assertFalse(
							Hibernate.isInitialized( childs )
					);
					assertThat( childs.size(), is( 2 ) );

					Map<Integer, EntityWithManyToOneWithoutJoinTable> othersById = new HashMap<>();
					for ( EntityWithManyToOneWithoutJoinTable child : childs ) {
						othersById.put( child.getId(), child );
					}

					assertThat( othersById.get( 2 ).getSomeInteger(), is( Integer.MAX_VALUE ) );
					assertThat( othersById.get( 3 ).getSomeInteger(), is( Integer.MIN_VALUE ) );
				} );
	}
}
