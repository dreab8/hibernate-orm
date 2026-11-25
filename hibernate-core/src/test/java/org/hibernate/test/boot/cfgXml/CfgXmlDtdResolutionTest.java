/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.boot.cfgXml;

import java.io.InputStream;
import java.util.Collections;

import org.hibernate.HibernateException;
import org.hibernate.boot.cfgxml.internal.JaxbCfgProcessor;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.cfg.spi.JaxbCfgConfigPropertyType;
import org.hibernate.boot.jaxb.cfg.spi.JaxbCfgHibernateConfiguration;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * Tests that {@code hibernate.cfg.xml} DTD and external entity references are resolved locally
 * through {@link org.hibernate.boot.jaxb.internal.stax.LocalXmlResourceResolver} when
 * {@value AvailableSettings#XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES} is enabled, and rejected otherwise.
 */
@TestForIssue(jiraKey = "HHH-21047")
public class CfgXmlDtdResolutionTest extends BaseUnitTestCase {
	private static final String CFG_XML = "org/hibernate/test/boot/cfgXml/dtd-entity.cfg.xml";

	private ClassLoaderService classLoaderService;

	@Before
	public void setUp() {
		classLoaderService = new ClassLoaderServiceImpl();
	}

	@After
	public void tearDown() {
		classLoaderService.stop();
	}

	@Test
	public void testExternalEntityResolvedFromClasspathWhenAllowed() throws Exception {
		final JaxbCfgProcessor processor = new JaxbCfgProcessor(
				classLoaderService,
				Collections.singletonMap( AvailableSettings.XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES, "true" )
		);
		final JaxbCfgHibernateConfiguration configuration = unmarshal( processor );
		assertThat( configuration.getSessionFactory().getProperty() )
				.extracting( JaxbCfgConfigPropertyType::getName, JaxbCfgConfigPropertyType::getValue )
				.containsExactly( tuple( "hibernate.test.entity.property", "resolved" ) );
	}

	@Test
	public void testDtdRejectedByDefault() {
		final JaxbCfgProcessor processor = new JaxbCfgProcessor( classLoaderService );
		assertThatThrownBy( () -> unmarshal( processor ) )
				.isInstanceOf( HibernateException.class );
	}

	private JaxbCfgHibernateConfiguration unmarshal(JaxbCfgProcessor processor) throws Exception {
		try ( InputStream stream = classLoaderService.locateResourceStream( CFG_XML ) ) {
			return processor.unmarshal( stream, new Origin( SourceType.RESOURCE, CFG_XML ) );
		}
	}
}
