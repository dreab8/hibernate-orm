/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.internal.SchemaValidatorImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.Skip;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class SchemaMigrationPerfTest extends BaseUnitTestCase {
	private File output;
	private StandardServiceRegistry ssr;

	@Before
	public void setUp() throws IOException {
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
		ssr = new StandardServiceRegistryBuilder().applySetting( AvailableSettings.HBM2DLL_CREATE_SCHEMAS, "true" )
				.build();
	}

	@After
	public void tearsDown() {
		StandardServiceRegistryBuilder.destroy( ssr );
	}


	@Test
	@TestForIssue(jiraKey = "HHH-10820")
	@Skip(condition = Skip.OperatingSystem.Windows.class, message = "On Windows, MySQL is case insensitive!")
	public void testSchemaUpdateWithQuotedTableName() throws Exception {
		final MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( TestEntity.class );
		metadataSources.addAnnotatedClass( TestEntity2.class );
		metadataSources.addAnnotatedClass( TestEntity3.class );
		metadataSources.addAnnotatedClass( TestEntity4.class );
		metadataSources.addAnnotatedClass( TestEntity5.class );
		metadataSources.addAnnotatedClass( TestEntity6.class );
		metadataSources.addAnnotatedClass( TestEntity7.class );
		metadataSources.addAnnotatedClass( TestEntity8.class );
		metadataSources.addAnnotatedClass( TestEntity9.class );
		metadataSources.addAnnotatedClass( TestEntity10.class );
		metadataSources.addAnnotatedClass( TestEntity11.class );
		metadataSources.addAnnotatedClass( TestEntity12.class );
		metadataSources.addAnnotatedClass( TestEntity13.class );
		metadataSources.addAnnotatedClass( TestEntity14.class );
		metadataSources.addAnnotatedClass( TestEntity15.class );
		metadataSources.addAnnotatedClass( TestEntity16.class );
		metadataSources.addAnnotatedClass( TestEntity17.class );
		metadataSources.addAnnotatedClass( TestEntity18.class );
		metadataSources.addAnnotatedClass( TestEntity19.class );
		metadataSources.addAnnotatedClass( TestEntity20.class );
		metadataSources.addAnnotatedClass( TestEntity21.class );
		metadataSources.addAnnotatedClass( TestEntity22.class );
		metadataSources.addAnnotatedClass( TestEntity23.class );
		metadataSources.addAnnotatedClass( TestEntity24.class );
		metadataSources.addAnnotatedClass( TestEntity25.class );
		metadataSources.addAnnotatedClass( TestEntity26.class );
		metadataSources.addAnnotatedClass( TestEntity27.class );
		metadataSources.addAnnotatedClass( TestEntity28.class );
		metadataSources.addAnnotatedClass( TestEntity29.class );
		metadataSources.addAnnotatedClass( TestEntity30.class );
		metadataSources.addAnnotatedClass( TestEntity31.class );
		metadataSources.addAnnotatedClass( TestEntity32.class );
		metadataSources.addAnnotatedClass( TestEntity33.class );
		metadataSources.addAnnotatedClass( TestEntity34.class );
		metadataSources.addAnnotatedClass( TestEntity35.class );
		metadataSources.addAnnotatedClass( TestEntity36.class );
		metadataSources.addAnnotatedClass( TestEntity37.class );
		metadataSources.addAnnotatedClass( TestEntity38.class );
		metadataSources.addAnnotatedClass( TestEntity39.class );
		metadataSources.addAnnotatedClass( TestEntity40.class );

		MetadataImplementor metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.validate();

		long startTime = System.currentTimeMillis();


		new SchemaUpdate().setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.execute( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ), metadata );
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		System.out.println(">>>>>>>>>>>>>>>>>>>>> Elapsed time = " + elapsedTime);
		output.delete();
		output.createNewFile();
		new SchemaValidator().validate( metadata );

		startTime = System.currentTimeMillis();


		new SchemaUpdate().setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.execute( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ), metadata );
		stopTime = System.currentTimeMillis();
		elapsedTime = stopTime - startTime;
		System.out.println(">>>>>>>>>>>>>>>>>>>>> Elapsed time = " + elapsedTime);

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertThat( "The update output file should be empty",fileContent, is( "" ) );

		new SchemaExport().setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.drop( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	public static class TestEntity {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity2")
	@Table(name = "TEST_ENTITY_2")
	public static class TestEntity2 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity3")
	@Table(name = "TEST_ENTITY_3")
	public static class TestEntity3 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity4")
	@Table(name = "TEST_ENTITY_4")
	public static class TestEntity4 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity5")
	@Table(name = "TEST_ENTITY_5")
	public static class TestEntity5 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity6")
	@Table(name = "TEST_ENTITY_6")
	public static class TestEntity6 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity7")
	@Table(name = "TEST_ENTITY_7")
	public static class TestEntity7 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity8")
	@Table(name = "TEST_ENTITY_8")
	public static class TestEntity8 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity9")
	@Table(name = "TEST_ENTITY_9")
	public static class TestEntity9 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity10")
	@Table(name = "TEST_ENTITY_10")
	public static class TestEntity10 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity11")
	@Table(name = "TEST_ENTITY_11")
	public static class TestEntity11 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity12")
	@Table(name = "TEST_ENTITY_12")
	public static class TestEntity12 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity13")
	@Table(name = "TEST_ENTITY_13")
	public static class TestEntity13 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity14")
	@Table(name = "TEST_ENTITY_14")
	public static class TestEntity14 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity15")
	@Table(name = "TEST_ENTITY_15")
	public static class TestEntity15 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity16")
	@Table(name = "TEST_ENTITY_16")
	public static class TestEntity16 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity17")
	@Table(name = "TEST_ENTITY_17")
	public static class TestEntity17 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity18")
	@Table(name = "TEST_ENTITY_18")
	public static class TestEntity18 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity19")
	@Table(name = "TEST_ENTITY_19")
	public static class TestEntity19 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity20")
	@Table(name = "TEST_ENTITY_20")
	public static class TestEntity20 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity21")
	@Table(name = "TEST_ENTITY_21")
	public static class TestEntity21 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity22")
	@Table(name = "TEST_ENTITY_22")
	public static class TestEntity22 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity23")
	@Table(name = "TEST_ENTITY_23")
	public static class TestEntity23 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity24")
	@Table(name = "TEST_ENTITY_24")
	public static class TestEntity24 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity25")
	@Table(name = "TEST_ENTITY_25")
	public static class TestEntity25 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity26")
	@Table(name = "TEST_ENTITY_26")
	public static class TestEntity26 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity27")
	@Table(name = "TEST_ENTITY_27")
	public static class TestEntity27 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity28")
	@Table(name = "TEST_ENTITY_28")
	public static class TestEntity28 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity29")
	@Table(name = "TEST_ENTITY_29")
	public static class TestEntity29 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity30")
	@Table(name = "TEST_ENTITY_30")
	public static class TestEntity30 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity31")
	@Table(name = "TEST_ENTITY_31")
	public static class TestEntity31 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity32")
	@Table(name = "TEST_ENTITY_32")
	public static class TestEntity32 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity33")
	@Table(name = "TEST_ENTITY_33")
	public static class TestEntity33 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity34")
	@Table(name = "TEST_ENTITY_34")
	public static class TestEntity34 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity35")
	@Table(name = "TEST_ENTITY_35")
	public static class TestEntity35 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity36")
	@Table(name = "TEST_ENTITY_36")
	public static class TestEntity36 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity37")
	@Table(name = "TEST_ENTITY_37")
	public static class TestEntity37 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity38")
	@Table(name = "TEST_ENTITY_38")
	public static class TestEntity38 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity39")
	@Table(name = "`TEST_ENTITY_40`")
	public static class TestEntity39 {
		@Id
		long id;

		String field1;
		String field2;
		String field3;
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}

	@Entity(name = "TestEntity40")
	@Table(name = "`TEST_entity_40`")
	public static class TestEntity40 {
		@Id
		long id;
		@Column(name = "`FieLd1`")
		String field1;
		@Column(name = "`FIELD_2`")
		String field2;
		@Column(name = "`field_3`")
		String field3;
		@Column(name = "`Field_3`")
		String field4;
		String field5;
		String field6;
		String field7;
		String field8;
		String field9;
		String field10;
		String field11;
		String field12;
		String field13;
		String field14;
		String field15;
		String field16;
		String field17;
		String field18;
		String field19;
		String field20;
	}
}
