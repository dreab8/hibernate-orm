/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.joins;

import java.util.Date;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Andrea Boriero
 */
public class ToDeleteTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );

		metadataSources.addAnnotatedClass( A.class );
		metadataSources.addAnnotatedClass( B.class );
		metadataSources.addAnnotatedClass( C.class );
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@Test
	public void testMappedSuperclassAndSecondaryTable() {
		sessionFactoryScope().inTransaction(
				session -> {
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
