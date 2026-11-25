/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.boot;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import org.hibernate.jpa.test.PersistenceUnitDescriptorAdapter;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.AvailableSettings.CFG_XML_FILE;
import static org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER;
import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.DRIVER;
import static org.hibernate.cfg.AvailableSettings.URL;
import static org.hibernate.cfg.AvailableSettings.XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES;
import static org.hibernate.cfg.AvailableSettings.XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES;

/**
 * Tests that {@code hibernate.xml.allow_dtd_and_external_entities} is honored for the
 * {@code cfg.xml} selected via {@code hibernate.cfg_xml_file} on the JPA bootstrap path.
 */
@TestForIssue(jiraKey = "HHH-21193")
public class JpaCfgXmlDtdAndExternalEntitiesTest extends BaseUnitTestCase {
	private static final String DTD_ENTITY_CFG_XML = "org/hibernate/test/boot/cfgXml/dtd-entity.cfg.xml";
	private static final String DTD_ENTITY_PROPERTY = "hibernate.test.entity.property";

	@Test
	public void selectedHibernateXmlRejectsDtdAndExternalEntitiesByDefault() {
		final Properties properties = baseProperties();
		properties.put( CFG_XML_FILE, DTD_ENTITY_CFG_XML );
		assertThatThrownBy( () -> builder( descriptor( properties ), Collections.emptyMap() ) )
				.isInstanceOf( HibernateException.class );
	}

	@Test
	public void unitPropertyAllowsDtdAndExternalEntitiesInSelectedHibernateXml() {
		final Properties properties = baseProperties();
		properties.put( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, "true" );
		properties.put( CFG_XML_FILE, DTD_ENTITY_CFG_XML );
		final EntityManagerFactoryBuilderImpl builder = builder( descriptor( properties ), Collections.emptyMap() );
		try {
			assertThat( builder.getConfigurationValues() ).containsEntry( DTD_ENTITY_PROPERTY, "resolved" );
		}
		finally {
			builder.cancel();
		}
	}

	@Test
	public void integrationSettingAllowsDtdAndExternalEntitiesInSelectedHibernateXml() {
		final Map<String, Object> integrationSettings = new HashMap<>();
		integrationSettings.put( CFG_XML_FILE, DTD_ENTITY_CFG_XML );
		integrationSettings.put( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, true );
		final EntityManagerFactoryBuilderImpl builder = builder( descriptor( baseProperties() ), integrationSettings );
		try {
			assertThat( builder.getConfigurationValues() ).containsEntry( DTD_ENTITY_PROPERTY, "resolved" );
		}
		finally {
			builder.cancel();
		}
	}

	@Test
	public void integrationSettingOverridesUnitPropertyForSelectedHibernateXml() {
		final Properties properties = baseProperties();
		properties.put( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, "true" );
		properties.put( CFG_XML_FILE, DTD_ENTITY_CFG_XML );
		assertThatThrownBy( () -> builder(
				descriptor( properties ),
				Collections.singletonMap( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, false )
		) ).isInstanceOf( HibernateException.class );
	}

	private static Properties baseProperties() {
		final Properties properties = new Properties();
		properties.put( DIALECT, H2Dialect.class.getName() );
		// 5.6 has no AvailableSettings.ALLOW_METADATA_ON_BOOT, this is its predecessor
		properties.put( "hibernate.temp.use_jdbc_metadata_defaults", false );
		properties.put( CONNECTION_PROVIDER, new UserSuppliedConnectionProviderImpl() );
		properties.put( URL, "jdbc:unit" );
		properties.put( DRIVER, "unit-driver" );
		return properties;
	}

	private static PersistenceUnitDescriptor descriptor(Properties properties) {
		return new PersistenceUnitDescriptorAdapter() {
			@Override
			public Properties getProperties() {
				return properties;
			}
		};
	}

	private static EntityManagerFactoryBuilderImpl builder(
			PersistenceUnitDescriptor descriptor,
			Map<String, Object> integrationSettings) {
		return new EntityManagerFactoryBuilderImpl( descriptor, integrationSettings );
	}
}
