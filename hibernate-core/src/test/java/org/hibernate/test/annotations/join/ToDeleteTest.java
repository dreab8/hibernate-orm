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
package org.hibernate.test.annotations.join;

import java.util.Date;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;

/**
 * @author Andrea Boriero
 */
public class ToDeleteTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { A.class, B.class, C.class };
	}


	@Test
	public void testMappedSuperclassAndSecondaryTable() {
		doInHibernate( this::sessionFactory, session -> {
			C c = new C();
			c.setAge( 12 );
			c.setCreateDate( new Date() );
			c.setName( "Bob" );
			session.persist( c );
			session.flush();
			session.clear();
			c = session.get( C.class, c.getId() );
			assertNotNull( c.getCreateDate() );
			assertNotNull( c.getName() );
		} );
	}
}
