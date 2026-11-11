package org.hibernate.orm.test.jpa.persistenceunit;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.hibernate.jpa.boot.spi.PersistenceXmlParser;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.MappingSettings.XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES;
import static org.hibernate.internal.util.ConfigHelper.findAsResource;

/**
 * Verifies that {@code persistence.xml} DTD/entity support is disabled by default and
 * can be enabled through settings available before the document is parsed.
 *
 * @see org.hibernate.cfg.MappingSettings#XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES
 */
public class PersistenceXmlDtdTest {
	// declares <!ENTITY puName "dtdpu"> and also sets the setting to true in its own <properties>
	private static final URL PERSISTENCE_XML =
			findAsResource( "org/hibernate/jpa/test/persistenceunit/META-INF/dtd-entity.xml" );

	@Test
	public void rejectedByDefault() {
		assertRejected( PersistenceXmlParser.create() );
	}

	@Test
	public void rejectedWhenExplicitlyDisabled() {
		assertRejected( PersistenceXmlParser.create( Map.of( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, "false" ) ) );
	}

	@Test
	public void acceptedWhenEnabledByIntegrationSettings() {
		final var parser = PersistenceXmlParser.create( Map.of( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, "true" ) );
		assertThat( parser.parse( List.of( PERSISTENCE_XML ) ) ).containsOnlyKeys( "dtdpu" );
	}

	private static void assertRejected(PersistenceXmlParser parser) {
		assertThatThrownBy( () -> parser.parse( List.of( PERSISTENCE_XML ) ) )
				.hasStackTraceContaining( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES );
	}
}
