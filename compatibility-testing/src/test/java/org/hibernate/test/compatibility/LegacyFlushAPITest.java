/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.compatibility;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import javax.persistence.FlushModeType;

/**
 * @author Sanne Grinovero
 */
public class LegacyFlushAPITest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void testQueryCacheInvalidation() throws Exception {
		try ( Session s = sessionFactory().openSession() ) {
			FlushMode flushMode = s.getFlushMode();
		}
	}

}