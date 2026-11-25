/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.util.dtd;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Verifies that, by default, mapping documents declaring entities are rejected, while a plain DOCTYPE is accepted.
 *
 * @see org.hibernate.cfg.AvailableSettings#XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES
 */
public class EntityResolverDisallowedTest extends BaseUnitTestCase {
	private StandardServiceRegistry serviceRegistry;

	@Before
	public void setUp() {
		serviceRegistry = new StandardServiceRegistryBuilder().build();
	}

	@After
	public void tearDown() {
		StandardServiceRegistryBuilder.destroy( serviceRegistry );
	}

	@Test
	public void testEntityIncludeRejectedByDefault() {
		// Parent.orm.xml declares a DTD with the following entity include:
		//		<!ENTITY child SYSTEM "classpath://org/hibernate/test/util/dtd/child-entity.xml">
		// which must not be processed unless explicitly allowed.
		assertRejected( "org/hibernate/test/util/dtd/Parent.orm.xml" );
	}

	@Test
	public void testInternalEntityRejectedByDefault() {
		// InternalEntity.orm.xml declares a DTD with the following internal entity:
		//		<!ENTITY package "org.hibernate.test.util.dtd">
		// which, not being external, is still not expanded unless explicitly allowed.
		assertRejected( "org/hibernate/test/util/dtd/InternalEntity.orm.xml" );
	}

	@Test
	public void testDoctypeWithoutEntitiesAcceptedByDefault() {
		// DoctypeWithoutEntities.orm.xml declares an unreachable external DTD and an internal subset
		// with no entities: the external DTD must not be fetched, and the document must still bind.
		new MetadataSources( serviceRegistry )
				.addResource( "org/hibernate/test/util/dtd/DoctypeWithoutEntities.orm.xml" )
				.buildMetadata();
	}

	private void assertRejected(String resource) {
		try {
			new MetadataSources( serviceRegistry )
					.addResource( resource )
					.buildMetadata();
			fail( "Expecting the mapping document [" + resource + "] to be rejected" );
		}
		catch (Exception e) {
			final MappingException mappingException = assertTyping( MappingException.class, e.getCause() );
			assertTrue(
					mappingException.getMessage(),
					mappingException.getMessage().contains( AvailableSettings.XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES )
			);
		}
	}
}
