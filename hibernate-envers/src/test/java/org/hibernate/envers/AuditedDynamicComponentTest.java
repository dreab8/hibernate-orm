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
package org.hibernate.envers;

import java.util.Collections;

import org.hibernate.Session;
import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

@TestForIssue(jiraKey = "HHH-8049")
public class AuditedDynamicComponentTest extends BaseEnversFunctionalTestCase {
	ClassAccessNoSettersEntity entity;

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { ClassAccessNoSettersEntity.class };
	}

	//@Test
	public void testAuditedDynamicComponentFailure() {
		doInHibernate( this::sessionFactory, session -> {
			ClassAccessNoSettersEntity loaded = session.find( ClassAccessNoSettersEntity.class, entity.getCode() );
			assertThat( loaded, is( entity ) );
		} );
	}

	@Test
	@Priority(10)
	public void initData() {
		Session session = openSession();

		entity = ClassAccessNoSettersEntity.of( 123, "Germany" );

		// Revision 1
		session.getTransaction().begin();
		session.save( entity );
		session.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assertThat(
				getAuditReader().getRevisions( ClassAccessNoSettersEntity.class, entity.getCode() ),
				is( Collections.singletonList( 1 ) )
		);
	}

	@Test
	public void testHistoryOfId1() {

		assertThat( getAuditReader().find( ClassAccessNoSettersEntity.class, entity.getCode(), 1 ), is( entity ) );

	}
}
