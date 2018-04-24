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
import org.hibernate.type.CharacterArrayTypeImpl;
import org.hibernate.type.CharacterNCharTypeImpl;
import org.hibernate.type.CharacterTypeImpl;
import org.hibernate.type.MaterializedClobTypeImpl;
import org.hibernate.type.MaterializedNClobTypeImpl;
import org.hibernate.type.NClobTypeImpl;
import org.hibernate.type.NTextTypeImpl;
import org.hibernate.type.StringNVarcharTypeImpl;
import org.hibernate.type.StringTypeImpl;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

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
				assertSame( StringTypeImpl.INSTANCE, prop.getType() );
			}else{
				assertSame( StringNVarcharTypeImpl.INSTANCE, prop.getType() );
			}

			prop = pc.getProperty( "materializedNclobAtt" );
			if(metadata.getDatabase().getDialect() instanceof PostgreSQL81Dialect ){
				// See issue HHH-10693
				assertSame( MaterializedClobTypeImpl.INSTANCE, prop.getType() );
			}else {
				assertSame( MaterializedNClobTypeImpl.INSTANCE, prop.getType() );
			}
			prop = pc.getProperty( "nclobAtt" );
			assertSame( NClobTypeImpl.INSTANCE, prop.getType() );

			prop = pc.getProperty( "nlongvarcharcharAtt" );
			assertSame( NTextTypeImpl.INSTANCE, prop.getType() );

			prop = pc.getProperty( "ncharArrAtt" );
			if(metadata.getDatabase().getDialect() instanceof PostgreSQL81Dialect ){
				// See issue HHH-10693
				assertSame( CharacterArrayTypeImpl.INSTANCE, prop.getType() );
			}else {
				assertSame( StringNVarcharTypeImpl.INSTANCE, prop.getType() );
			}

			prop = pc.getProperty( "ncharacterAtt" );
			if ( metadata.getDatabase().getDialect() instanceof PostgreSQL81Dialect ) {
				// See issue HHH-10693
				assertSame( CharacterTypeImpl.INSTANCE, prop.getType() );
			}
			else {
				assertSame( CharacterNCharTypeImpl.INSTANCE, prop.getType() );
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
