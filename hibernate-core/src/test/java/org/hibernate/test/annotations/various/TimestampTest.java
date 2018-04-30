/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.various;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.domain.spi.TimestampVersionSupport;
import org.hibernate.metamodel.model.domain.spi.VersionSupport;
import org.hibernate.type.DbTimestampType;
import org.hibernate.type.spi.BasicType;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Test for the @Timestamp annotation.
 *
 * @author Hardy Ferentschik
 */
public class TimestampTest extends BaseUnitTestCase {
	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;

	@BeforeClassOnce
	public void setUp() {
		ssr = new StandardServiceRegistryBuilder().build();
		metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( VMTimestamped.class )
				.addAnnotatedClass( DBTimestamped.class )
				.getMetadataBuilder()
				.build();
	}

	@AfterClassOnce
	public void tearDown() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testTimestampSourceIsVM() {
		assertTimestampSource( VMTimestamped.class, TimestampVersionSupport.class );
	}

	@Test
	public void testTimestampSourceIsDB() {
		assertTimestampSource( DBTimestamped.class, DbTimestampType.class );
	}

	private void assertTimestampSource(Class<?> clazz, Class<?> expectedVersionSupport) {
		PersistentClass persistentClass = metadata.getEntityBinding( clazz.getName() );
		assertNotNull( persistentClass );
		Property versionProperty = persistentClass.getVersion();
		assertNotNull( versionProperty );
		assertThat( versionProperty.getType(), instanceOf( BasicType.class ) );
		VersionSupport versionSupport = ( (BasicType) versionProperty.getType() ).getJavaTypeDescriptor()
				.getVersionSupport();
		assertEquals( "Wrong timestamp type", versionSupport, expectedVersionSupport );
	}
}
