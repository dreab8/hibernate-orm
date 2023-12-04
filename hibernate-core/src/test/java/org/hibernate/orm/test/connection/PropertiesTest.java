/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.connection;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.cfgxml.spi.LoadedConfig;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.tool.schema.Action;

import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author kbow
 */
@BaseUnitTest
public class PropertiesTest {

	@Test
	public void testProperties() {
		final Properties props = new Properties();

		props.put( "rpt.1.hibernate.dialect", "org.hibernate.dialect.DerbyDialect" );
		props.put( "rpt.2.hibernate.connection.driver_class", "org.apache.derby.jdbc.ClientDriver" );
		props.put( "rpt.3.hibernate.connection.url", "jdbc:derby://localhost:1527/db/reports.db" );
		props.put( "rpt.4.hibernate.connection.username", "sa" );
		props.put( "rpt.5.hibernate.connection.password_enc", "76f271db3661fd50082e68d4b953fbee" );
		props.put( "rpt.6.hibernate.connection.password_enc", "76f271db3661fd50082e68d4b953fbee" );
		props.put( "hibernate.connection.create", "true" );

		final Properties outputProps = ConnectionProviderInitiator.getConnectionProperties( PropertiesHelper.map( props ) );
		assertEquals( 1, outputProps.size() );
		assertEquals( "true", outputProps.get( "create" ) );
	}

	@Test
	@JiraKey("HHH-17463")
	public void testUsingJakartaJdbUrlSetting() {
		try (final BootstrapServiceRegistry bootstrapServiceRegistry = new BootstrapServiceRegistryBuilder().build()) {
			final StandardServiceRegistryBuilder standardServiceRegistryBuilder = new MyStandardServiceRegistryBuilder(
					bootstrapServiceRegistry, new HashMap<>(), LoadedConfig.baseline() );
			standardServiceRegistryBuilder
					.applySetting( AvailableSettings.JAKARTA_JDBC_URL, ConnectionProviderBuilder.URL )
					.applySetting( AvailableSettings.JAKARTA_JDBC_DRIVER, ConnectionProviderBuilder.DRIVER )
					.applySetting( AvailableSettings.JAKARTA_JDBC_USER, ConnectionProviderBuilder.USER )
					.applySetting( AvailableSettings.JAKARTA_JDBC_PASSWORD, ConnectionProviderBuilder.PASS )
					.applySetting( AvailableSettings.HBM2DDL_AUTO, Action.UPDATE );

			final MetadataSources metadataSources = new MetadataSources();


			try (final StandardServiceRegistry build = standardServiceRegistryBuilder.build()) {
				final MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder( build );

				final SessionFactoryBuilder sessionFactoryBuilder = metadataBuilder.build().getSessionFactoryBuilder();

				final SessionFactory sessionFactory = sessionFactoryBuilder.build();
				sessionFactory.close();
			}
		}
	}

	/*
		All the StandardServiceRegistryBuilder public constructors populate the settings reading from hibernate.properties file,
		that sets the deprecated property AvailableSettings.URL
	 	MyStandardServiceRegistryBuilder gives access to the StandardServiceRegistryBuilder protected constructor
	 	that does not read the hibernate.properties,
	 */
	public static class MyStandardServiceRegistryBuilder extends StandardServiceRegistryBuilder {

		public MyStandardServiceRegistryBuilder(
				BootstrapServiceRegistry bootstrapServiceRegistry,
				Map<String, Object> settings,
				LoadedConfig loadedConfig) {
			super( bootstrapServiceRegistry, settings, loadedConfig );
		}
	}


}
