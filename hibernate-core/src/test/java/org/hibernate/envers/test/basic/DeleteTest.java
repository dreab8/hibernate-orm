/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.basic;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.BasicPartialAuditedEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public class DeleteTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer id1;
	private Integer id2;
	private Integer id3;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BasicPartialAuditedEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransaction(
				entityManager -> {
					BasicPartialAuditedEntity e1 = new BasicPartialAuditedEntity( "x", "a" );
					BasicPartialAuditedEntity e2 = new BasicPartialAuditedEntity( "y", "b" );
					BasicPartialAuditedEntity e3 = new BasicPartialAuditedEntity( "z", "c" );
					entityManager.persist( e1 );
					entityManager.persist( e2 );
					entityManager.persist( e3 );

					this.id1 = e1.getId();
					this.id2 = e2.getId();
					this.id3 = e3.getId();
				}
		);

		inTransaction(
				entityManager -> {
					BasicPartialAuditedEntity e1 = entityManager.find( BasicPartialAuditedEntity.class, this.id1 );
					BasicPartialAuditedEntity e2 = entityManager.find( BasicPartialAuditedEntity.class, this.id2 );
					BasicPartialAuditedEntity e3 = entityManager.find( BasicPartialAuditedEntity.class, this.id3 );
					e1.setStr1( "x2" );
					e2.setStr2( "b2" );
					entityManager.remove( e3 );
				}
		);

		inTransaction(
				entityManager -> {
					BasicPartialAuditedEntity e2 = entityManager.find( BasicPartialAuditedEntity.class, this.id2 );
					entityManager.remove( e2 );
				}
		);

		inTransaction(
				entityManager -> {
					BasicPartialAuditedEntity e1 = entityManager.find( BasicPartialAuditedEntity.class, this.id1 );
					entityManager.remove( e1 );
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( BasicPartialAuditedEntity.class, id1 ), hasItems( 1, 2, 4 ) );
		assertThat( getAuditReader().getRevisions( BasicPartialAuditedEntity.class, id2 ), hasItems( 1, 3 ) );
		assertThat( getAuditReader().getRevisions( BasicPartialAuditedEntity.class, id3 ), hasItems( 1, 2 ) );
	}

	@DynamicTest
	public void testRevisionHistoryOfId1() {
		BasicPartialAuditedEntity ver1 = new BasicPartialAuditedEntity( id1, "x", null );
		BasicPartialAuditedEntity ver2 = new BasicPartialAuditedEntity( id1, "x2", null );

		assertThat( getAuditReader().find( BasicPartialAuditedEntity.class, id1, 1 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicPartialAuditedEntity.class, id1, 2 ), is( ver2 ) );
		assertThat( getAuditReader().find( BasicPartialAuditedEntity.class, id1, 3 ), is( ver2 ) );
		assertThat( getAuditReader().find( BasicPartialAuditedEntity.class, id1, 4 ), nullValue() );
	}

	@DynamicTest
	public void testRevisionHistoryOfId2() {
		BasicPartialAuditedEntity ver1 = new BasicPartialAuditedEntity( id2, "y", null );

		assertThat( getAuditReader().find( BasicPartialAuditedEntity.class, id2, 1 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicPartialAuditedEntity.class, id2, 2 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicPartialAuditedEntity.class, id2, 3 ), nullValue() );
		assertThat( getAuditReader().find( BasicPartialAuditedEntity.class, id2, 4 ), nullValue() );
	}

	@DynamicTest
	public void testRevisionHistoryOfId3() {
		BasicPartialAuditedEntity ver1 = new BasicPartialAuditedEntity( id3, "z", null );

		assertThat( getAuditReader().find( BasicPartialAuditedEntity.class, id3, 1 ), is( ver1 ) );
		assertThat( getAuditReader().find( BasicPartialAuditedEntity.class, id3, 2 ), nullValue() );
		assertThat( getAuditReader().find( BasicPartialAuditedEntity.class, id3, 3 ), nullValue() );
		assertThat( getAuditReader().find( BasicPartialAuditedEntity.class, id3, 4 ), nullValue() );
	}
}
