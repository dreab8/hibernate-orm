/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.persistenceunit;

import java.net.URL;
import java.util.Collections;
import java.util.Map;

import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import javax.persistence.PersistenceException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.AvailableSettings.XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES;

@TestForIssue(jiraKey = "HHH-21047")
public class PersistenceXmlParserDtdTest extends BaseUnitTestCase {
	private static final String DTD_ENTITY_PU = "dtd-entity";

	@Test
	public void parse_rejectsDtdAndExternalEntitiesByDefault() {
		assertThatThrownBy( () -> parse( Collections.emptyMap() ) )
				.isInstanceOf( PersistenceException.class )
				.hasMessageContaining( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES );
	}

	@Test
	public void parse_integrationSettingFalseRejectsDtdAndExternalEntities() {
		assertThatThrownBy( () -> parse( Collections.singletonMap( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, false ) ) )
				.isInstanceOf( PersistenceException.class )
				.hasMessageContaining( XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES );
	}

	private ParsedPersistenceXmlDescriptor parse(Map<String, Object> integration) {
		final URL url = getClass().getClassLoader()
				.getResource( "org/hibernate/jpa/test/persistenceunit/META-INF/" + DTD_ENTITY_PU + ".xml" );
		assertThat( url ).isNotNull();
		// PersistenceXmlParser#parse ignores the given URL and looks for META-INF/persistence.xml resources instead
		return PersistenceXmlParser.locateIndividualPersistenceUnit( url, integration );
	}
}
