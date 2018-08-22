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
package org.hibernate.envers.test.wip60;

import java.util.Arrays;


import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertThat;

/**
 * @author Chris Cranford
 */
public class FieldAccessTypeTest extends BaseEnversFunctionalTestCase {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { FieldAccessTypeEntity.class };
	}

	@Test
	@Priority(10)
	public void prepareAuditData() {
		this.id = doInHibernate( this::sessionFactory, session -> {
			final FieldAccessTypeEntity entity = new FieldAccessTypeEntity( "data" );
			session.save( entity );
			return entity.getIdSafe();
		} );

		doInHibernate( this::sessionFactory, session -> {
			final FieldAccessTypeEntity entity = session.find( FieldAccessTypeEntity.class, this.id );
			entity.setDataSafe( "data2" );
		} );
	}

	@Test
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( FieldAccessTypeEntity.class, id ), is( Arrays.asList( 1, 2 ) ) );
	}

	@Test
	public void testHistoryOfEntity1() {
		FieldAccessTypeEntity ver1 = new FieldAccessTypeEntity( id, "data" );
		FieldAccessTypeEntity ver2 = new FieldAccessTypeEntity( id, "data2" );

		assertThat( getAuditReader().find( FieldAccessTypeEntity.class, id, 1 ), is( ver1 ) );
		assertThat( getAuditReader().find( FieldAccessTypeEntity.class, id, 2 ), is( ver2 ) );
	}
}
