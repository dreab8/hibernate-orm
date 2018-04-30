/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.nationalized;

import java.sql.NClob;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.Type;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

/**
 * @author Steve Ebersole
 */
public class SimpleNationalizedTest extends BaseUnitTestCase {

	@SuppressWarnings({"UnusedDeclaration", "SpellCheckingInspection"})
	@Entity( name="NationalizedEntity")
	public static class NationalizedEntity {
		@Id
		private Integer id;

		@Nationalized
		private String nvarcharAtt;

		@Lob
		@Nationalized
		private String materializedNclobAtt;

		@Lob
		@Nationalized
		private NClob nclobAtt;

		@Nationalized
		private Character ncharacterAtt;
		
		@Nationalized
		private Character[] ncharArrAtt;
		
		@Type(type = "ntext")
		private String nlongvarcharcharAtt;
	}

	@Test
	public void simpleNationalizedTest() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();

		try {
			final MetadataSources ms = new MetadataSources( ssr );
			ms.addAnnotatedClass( NationalizedEntity.class );

			final Metadata metadata = ms.buildMetadata();
			PersistentClass pc = metadata.getEntityBinding( NationalizedEntity.class.getName() );
			assertNotNull( pc );

			Property prop = pc.getProperty( "nvarcharAtt" );
			if(metadata.getDatabase().getDialect() instanceof PostgreSQL81Dialect ){
				// See issue HHH-10693
				assertSameBasicType( StandardSpiBasicTypes.STRING, prop.getType() );
			}else{
				assertSameBasicType( StandardSpiBasicTypes.NSTRING, prop.getType() );
			}

			prop = pc.getProperty( "materializedNclobAtt" );
			if(metadata.getDatabase().getDialect() instanceof PostgreSQL81Dialect ){
				// See issue HHH-10693
				assertSameBasicType( StandardSpiBasicTypes.MATERIALIZED_CLOB, prop.getType() );
			}else {
				assertSameBasicType( StandardSpiBasicTypes.MATERIALIZED_NCLOB, prop.getType() );
			}
			prop = pc.getProperty( "nclobAtt" );
			assertSameBasicType( StandardSpiBasicTypes.NCLOB, prop.getType() );

			prop = pc.getProperty( "nlongvarcharcharAtt" );
			assertSameBasicType( StandardSpiBasicTypes.NTEXT, prop.getType() );

			prop = pc.getProperty( "ncharArrAtt" );
			if ( metadata.getDatabase().getDialect() instanceof PostgreSQL81Dialect ) {
				// See issue HHH-10693
				assertSameBasicType( StandardSpiBasicTypes.CHARACTER_ARRAY, prop.getType() );
			}
			else {
				assertSameBasicType( StandardSpiBasicTypes.NSTRING, prop.getType() );
			}

			prop = pc.getProperty( "ncharacterAtt" );
			if ( metadata.getDatabase().getDialect() instanceof PostgreSQL81Dialect ) {
				// See issue HHH-10693
				assertSameBasicType( StandardSpiBasicTypes.CHARACTER, prop.getType() );
			}
			else {
				assertSameBasicType( StandardSpiBasicTypes.CHARACTER_NCHAR, prop.getType() );
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	private void assertSameBasicType(BasicType expected, org.hibernate.type.Type actual){
		assertThat( actual, instanceOf(BasicType.class) );
		BasicType actualBasicType = (BasicType) actual;
		assertSame( expected.getJavaTypeDescriptor(), actualBasicType.getJavaTypeDescriptor() );
		assertSame( expected.getSqlTypeDescriptor(), actualBasicType.getSqlTypeDescriptor() );
	}
}
