package org.hibernate.orm.test.jpa.boot;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.PersistenceConfigurationDescriptor;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import jakarta.persistence.PersistenceConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.AvailableSettings.ALLOW_METADATA_ON_BOOT;
import static org.hibernate.cfg.AvailableSettings.CFG_XML_FILE;
import static org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER;
import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES;

/// Verifies that a `cfg.xml` loaded during JPA bootstrap rejects DTD entities by default and
/// honors [XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES] only from settings outside the descriptors.
@BaseUnitTest
class JpaCfgXmlDtdTest {
	// declares an external entity contributing this property
	private static final String CFG_XML = "org/hibernate/orm/test/boot/cfgXml/dtd-entity.cfg.xml";
	private static final String ENTITY_PROPERTY = "hibernate.test.entity.property";

	@Test
	void rejectedByDefault() {
		assertRejected( configuration( new PersistenceConfiguration( "dtd" ) ), Map.of() );
	}

	@Test
	void rejectedWhenExplicitlyDisabled() {
		assertRejected( configuration( new PersistenceConfiguration( "dtd" ) ),
				Map.of( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, "false" ) );
	}

	@Test
	void rejectedWhenEnabledOnlyByUnitProperties() {
		assertRejected( configuration( new PersistenceConfiguration( "dtd" ) )
				.property( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, "true" ), Map.of() );
	}

	@Test
	void acceptedWhenEnabledByIntegrationSettings() {
		assertAccepted( new EntityManagerFactoryBuilderImpl(
				new PersistenceConfigurationDescriptor( configuration( new PersistenceConfiguration( "dtd" ) ) ),
				Map.of( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, "true" ) ) );
	}

	@Test
	void acceptedWhenEnabledByHibernatePersistenceConfiguration() {
		assertAccepted( new EntityManagerFactoryBuilderImpl( (HibernatePersistenceConfiguration)
				configuration( new HibernatePersistenceConfiguration( "dtd" ) )
						.property( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, "true" ) ) );
	}

	private static void assertRejected(PersistenceConfiguration configuration, Map<String, Object> integration) {
		assertThatThrownBy( () -> new EntityManagerFactoryBuilderImpl(
				new PersistenceConfigurationDescriptor( configuration ), integration ) )
				.hasStackTraceContaining( "Undeclared general entity \"properties\"" );
	}

	private static void assertAccepted(EntityManagerFactoryBuilderImpl builder) {
		try {
			assertThat( builder.getConfigurationValues() ).containsEntry( ENTITY_PROPERTY, "resolved" );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( builder.getStandardServiceRegistry() );
		}
	}

	private static PersistenceConfiguration configuration(PersistenceConfiguration configuration) {
		return configuration.property( DIALECT, H2Dialect.class.getName() )
				.property( ALLOW_METADATA_ON_BOOT, false )
				.property( CONNECTION_PROVIDER, new UserSuppliedConnectionProviderImpl() )
				.property( CFG_XML_FILE, CFG_XML );
	}
}
