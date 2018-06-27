/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.boot;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.jpa.test.persistenceunit.DuplicatePersistenceUnitNameTest;

import org.hibernate.test.jpa.xml.versions.JpaXsdVersionsTest;
import org.junit.Test;

import static org.hibernate.internal.util.ConfigHelper.findAsResource;

/**
 * @author Andrea Boriero
 */
public class BootFailureTest extends BaseEntityManagerFunctionalTestCase {

	@Test
	public void basicNewBootProcessTest() {

		final Map<String, Object> properties = new HashMap<>();
		properties.put( AvailableSettings.CLASSLOADERS, Arrays.asList( new TestClassLoader() ) );
		HibernatePersistenceProvider persistenceProvider = new HibernatePersistenceProvider();
		final EntityManagerFactory emf = persistenceProvider.createEntityManagerFactory("ExcludeUnlistedClassesTest1",properties
		);


		emf.close();
	}
	private static class TestClassLoader extends ClassLoader {
		final List<URL> urls;

		public TestClassLoader() {
			urls = Arrays.asList(
					findAsResource(
							"org/hibernate/jpa/test/persistenceunit/META-INF/persistenceUnitForNameDuplicationTest.xml"
					)
			);
		}

		@Override
		protected Enumeration<URL> findResources(String name) throws IOException {
			return name.equals( "META-INF/persistence.xml" ) ?
					Collections.enumeration( urls ) :
					Collections.emptyEnumeration();
		}
	}


	private PersistenceUnitDescriptor buildPersistenceUnitDescriptor() {
		return new TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() );
	}

	private class TestingPersistenceUnitDescriptorImpl implements PersistenceUnitDescriptor {
		public TestingPersistenceUnitDescriptorImpl(String simpleName) {
		}

		@Override
		public URL getPersistenceUnitRootUrl() {
			return null;
		}

		@Override
		public String getName() {
			throw new RuntimeException();
		}

		@Override
		public String getProviderClassName() {
			return null;
		}

		@Override
		public boolean isUseQuotedIdentifiers() {
			return false;
		}

		@Override
		public boolean isExcludeUnlistedClasses() {
			return false;
		}

		@Override
		public PersistenceUnitTransactionType getTransactionType() {
			return null;
		}

		@Override
		public ValidationMode getValidationMode() {
			return null;
		}

		@Override
		public SharedCacheMode getSharedCacheMode() {
			return null;
		}

		@Override
		public List<String> getManagedClassNames() {
			return null;
		}

		@Override
		public List<String> getMappingFileNames() {
			return null;
		}

		@Override
		public List<URL> getJarFileUrls() {
			return null;
		}

		@Override
		public Object getNonJtaDataSource() {
			return null;
		}

		@Override
		public Object getJtaDataSource() {
			return null;
		}

		@Override
		public Properties getProperties() {
			return null;
		}

		@Override
		public ClassLoader getClassLoader() {
			return null;
		}

		@Override
		public ClassLoader getTempClassLoader() {
			return null;
		}

		@Override
		public void pushClassTransformer(EnhancementContext enhancementContext) {

		}
	}
}

