/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.type;

import java.net.URL;

import org.hibernate.type.spi.BasicType;
import org.hibernate.type.StandardBasicTypeImplTemplate;
import org.hibernate.type.descriptor.java.internal.UrlJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.VarcharSqlDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;

/**
 * Test making sure StandardBasicTypeImplTemplate works
 *
 * @author Steve Ebersole
 */
public class StandardBasicTypeTemplateTest extends BaseUnitTestCase {

	public static final String REG_KEY = "validating-url";

	@Test
	public void testContributedBasicType() {
		TypeConfiguration typeConfiguration = new TypeConfiguration();
		typeConfiguration.getJavaTypeDescriptorRegistry().addDescriptor( ValidatingUrlJavaJavaDescriptor.INSTANCE );
		typeConfiguration.getBasicTypeRegistry().register(
				new StandardBasicTypeImplTemplate<>(
						VarcharSqlDescriptor.INSTANCE,
						ValidatingUrlJavaJavaDescriptor.INSTANCE,
						REG_KEY
				)
		);

		final BasicType registeredType = typeConfiguration.getBasicTypeRegistry().getRegisteredType( REG_KEY );
		assertThat( registeredType, notNullValue() );
		assertTyping( StandardBasicTypeImplTemplate.class, registeredType );
	}

	private static class ValidatingUrlJavaJavaDescriptor extends UrlJavaDescriptor {
		/**
		 * Singleton access
		 */
		public static final ValidatingUrlJavaJavaDescriptor INSTANCE = new ValidatingUrlJavaJavaDescriptor();

		@Override
		public URL fromString(String string) {
			if ( "invalid".equals( string ) ) {
				throw new IllegalStateException( "Invalid url" );
			}
			return super.fromString( string );
		}
	}
}
