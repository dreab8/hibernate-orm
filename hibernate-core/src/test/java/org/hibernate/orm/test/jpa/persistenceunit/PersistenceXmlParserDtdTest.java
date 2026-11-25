/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.persistenceunit;

import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import jakarta.persistence.PersistenceException;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.MappingSettings.XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES;

@JiraKey("HHH-21047")
public class PersistenceXmlParserDtdTest {
	private static final String DTD_ENTITY_PU = "dtd-entity";
	private static final String DOCTYPE_ONLY_PU = "doctype-only";

	@Test
	public void parse_rejectsDtdAndExternalEntitiesByDefault() {
		assertThatThrownBy( () -> parse( Collections.emptyMap() ) )
				.isInstanceOf( PersistenceException.class );
	}

	@Test
	public void parse_integrationSettingFalseRejectsDtdAndExternalEntities() {
		assertThatThrownBy( () -> parse( Map.of( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, false ) ) )
				.isInstanceOf( PersistenceException.class );
	}

	@Test
	public void parse_failsFastWhenParserCannotDisallowDoctype() {
		withParser( FeaturelessDocumentBuilderFactory.class, () ->
				assertThatThrownBy( () -> parse( DOCTYPE_ONLY_PU, Collections.emptyMap() ) )
						.isInstanceOf( PersistenceException.class )
						.hasMessageContaining( FeaturelessDocumentBuilderFactory.class.getName() )
						.hasMessageContaining( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES ) );
		// the document is never handed to the parser
		assertThat( FeaturelessDocumentBuilderFactory.BUILDERS_CREATED ).hasValue( 0 );
	}

	@Test
	public void parse_acceptsDoctypeWhenAllowedAndParserCannotDisallowIt() {
		withParser( FeaturelessDocumentBuilderFactory.class, () ->
				assertThat( parse( DOCTYPE_ONLY_PU, Map.of( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, true ) ) )
						.containsKey( DOCTYPE_ONLY_PU ) );
	}

	@Test
	public void parse_rejectsDoctypeWhenParserIgnoresFeatures() {
		withParser( FeatureIgnoringDocumentBuilderFactory.class, () ->
				assertThatThrownBy( () -> parse( DOCTYPE_ONLY_PU, Collections.emptyMap() ) )
						.isInstanceOf( PersistenceException.class )
						.hasMessageContaining( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES ) );
	}

	@Test
	public void parse_doesNotFetchExternalEntityWhenParserIgnoresFeatures() {
		withParser( FeatureIgnoringDocumentBuilderFactory.class, () ->
				assertThatThrownBy( () -> parse( DTD_ENTITY_PU, Collections.emptyMap() ) )
						.isInstanceOf( PersistenceException.class )
						.hasMessageContaining( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES )
						.hasStackTraceContaining( "is disallowed" ) );
	}

	private Map<String, ParsedPersistenceXmlDescriptor> parse(Map<String, Object> integration) {
		return parse( DTD_ENTITY_PU, integration );
	}

	private Map<String, ParsedPersistenceXmlDescriptor> parse(String resource, Map<String, Object> integration) {
		final URL url = getClass().getClassLoader()
				.getResource( "org/hibernate/jpa/test/persistenceunit/META-INF/" + resource + ".xml" );
		assertThat( url ).isNotNull();
		return PersistenceXmlParser.parse( url, PersistenceUnitTransactionType.RESOURCE_LOCAL, integration );
	}

	private static void withParser(Class<? extends DocumentBuilderFactory> factoryClass, Runnable action) {
		final String property = DocumentBuilderFactory.class.getName();
		final String previous = System.getProperty( property );
		System.setProperty( property, factoryClass.getName() );
		FeaturelessDocumentBuilderFactory.BUILDERS_CREATED.set( 0 );
		try {
			action.run();
		}
		finally {
			if ( previous == null ) {
				System.clearProperty( property );
			}
			else {
				System.setProperty( property, previous );
			}
		}
	}

	/**
	 * A parser which supports none of the features and attributes used to disable
	 * DTDs and external entities, as could be the case of an old or unusual implementation.
	 */
	public static class FeaturelessDocumentBuilderFactory extends DocumentBuilderFactory {
		static final AtomicInteger BUILDERS_CREATED = new AtomicInteger();

		private final DocumentBuilderFactory delegate = DocumentBuilderFactory.newDefaultInstance();

		@Override
		public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
			BUILDERS_CREATED.incrementAndGet();
			delegate.setNamespaceAware( isNamespaceAware() );
			return delegate.newDocumentBuilder();
		}

		@Override
		public void setAttribute(String name, Object value) {
			throw new IllegalArgumentException( name );
		}

		@Override
		public Object getAttribute(String name) {
			throw new IllegalArgumentException( name );
		}

		@Override
		public void setFeature(String name, boolean value) throws ParserConfigurationException {
			throw new ParserConfigurationException( name );
		}

		@Override
		public boolean getFeature(String name) {
			return false;
		}
	}

	/**
	 * A parser which accepts the features and attributes used to disable
	 * DTDs and external entities, but does not honor them.
	 */
	public static class FeatureIgnoringDocumentBuilderFactory extends DocumentBuilderFactory {
		private final DocumentBuilderFactory delegate = DocumentBuilderFactory.newDefaultInstance();

		@Override
		public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
			delegate.setNamespaceAware( isNamespaceAware() );
			return delegate.newDocumentBuilder();
		}

		@Override
		public void setAttribute(String name, Object value) {
		}

		@Override
		public Object getAttribute(String name) {
			return null;
		}

		@Override
		public void setFeature(String name, boolean value) {
		}

		@Override
		public boolean getFeature(String name) {
			return false;
		}
	}
}
