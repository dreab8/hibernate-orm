/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.schemaupdate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.schema.JdbcMetadaAccessStrategy;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-13788")
@RunWith(Parameterized.class)
public class SchemaUpdateWithUseJdbcMetadataDefaultsSettingToFalseTest {

	@Parameterized.Parameters
	public static String[] parameters() {
		return new String[] {
				JdbcMetadaAccessStrategy.GROUPED.toString(),
				JdbcMetadaAccessStrategy.INDIVIDUALLY.toString()
		};
	}

	@Parameterized.Parameter
	public String jdbcMetadataExtractorStrategy;

	private File updateOutputFile;
	private File createOutputFile;
	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;

	@Before
	public void setUp() throws IOException {
		createOutputFile = File.createTempFile( "create_script", ".sql" );
		createOutputFile.deleteOnExit();
		updateOutputFile = File.createTempFile( "update_script", ".sql" );
		updateOutputFile.deleteOnExit();
		ssr = new StandardServiceRegistryBuilder()
				.applySetting( "hibernate.temp.use_jdbc_metadata_defaults", "false" )
				.applySetting(
						AvailableSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY,
						jdbcMetadataExtractorStrategy
				)
				.build();

		final MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( TestEntity.class );

		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.validate();
	}

	@After
	public void tearDown() {
		new SchemaExport().setHaltOnError( true )
				.setFormat( false )
				.drop( EnumSet.of( TargetType.DATABASE ), metadata );
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testSchemaUpdateDoesNotTryToRecreateExistingTables()
			throws Exception {
		createSchema();

		new SchemaUpdate().setHaltOnError( true )
				.setOutputFile( updateOutputFile.getAbsolutePath() )
				.setFormat( false )
				.execute( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ), metadata );

		checkNoUpdateStatementHasBeenGenerated();
	}

	private void checkNoUpdateStatementHasBeenGenerated() throws IOException {
		final String fileContent = new String( Files.readAllBytes( updateOutputFile.toPath() ) );
		assertThat(
				"The update output file should be empty because the db schema had already been generated and the domain model was not modified",
				fileContent,
				is( "" )
		);
	}

	private void createSchema() throws Exception {
		new SchemaUpdate().setHaltOnError( true )
				.setOutputFile( createOutputFile.getAbsolutePath() )
				.execute( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ), metadata );
		new SchemaValidator().validate( metadata );
		checkSchemaHasBeenGenerated();
	}

	private void checkSchemaHasBeenGenerated() throws Exception {
		String fileContent = new String( Files.readAllBytes( createOutputFile.toPath() ) );
		Pattern fileContentPattern = Pattern.compile( "create( (column|row))? table test_entity" );
		Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		assertThat(
				"The schema has not been correctly generated, Script file : " + fileContent.toLowerCase(),
				fileContentMatcher.find(),
				is( true )
		);
	}

	@Entity(name = "test_entity")
	public static class TestEntity {
		@Id
		private Long id;

		private String name;
	}

}
