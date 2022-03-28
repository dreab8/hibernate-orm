package org.hibernate.orm.test.schemaupdate.idgenerator;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.enhanced.StandardImplicitIdentifierDatabaseObjectNamingStrategy;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import static org.hibernate.id.enhanced.SequenceStyleGenerator.CONFIG_SEQUENCE_PER_ENTITY_SUFFIX;
import static org.hibernate.id.enhanced.SequenceStyleGenerator.DEF_SEQUENCE_SUFFIX;
import static org.junit.jupiter.api.Assertions.assertTrue;


@BaseUnitTest
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSequences.class)
public class SequenceIdGenerationStrategyTest {

	private File output;
	private ServiceRegistry ssr;
	private MetadataImplementor metadata;

	@BeforeEach
	public void setUp() throws Exception {
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
	}

	private void buildMetadata(Class annotatedClass) {
		buildMetadata( null, annotatedClass );
	}

	private void buildMetadata(String preferSequencePerEntity, Class annotatedClass) {
		StandardServiceRegistryBuilder standardServiceRegistryBuilder = new StandardServiceRegistryBuilder();
		standardServiceRegistryBuilder.applySetting( AvailableSettings.FORMAT_SQL, "false" );

		if ( preferSequencePerEntity != null ) {
			standardServiceRegistryBuilder.applySetting(
					AvailableSettings.ID_DB_STRUCTURE_NAMING_STRATEGY,
					preferSequencePerEntity
			);
		}

		ssr = standardServiceRegistryBuilder.build();
		final MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( annotatedClass );
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.validate();
	}

	@AfterEach
	public void tearDown() {
		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata );
		ServiceRegistryBuilder.destroy( ssr );
	}


	@Test
	public void testSequenceNameStandardStrategy() throws Exception {
		buildMetadata( TestEntity.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "TestEntity_SEQ" ) );
	}

	@Test
	public void testSequenceNameHibernateSequenceStrategy() throws Exception {
		buildMetadata( HibernateSequenceDatabaseNamingStrategy.class.getName(), TestEntity.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "hibernate_sequence" ) );
	}

	@Test
	public void testSequenceNamePreferGeneratorNameStrategy() throws Exception {
		buildMetadata(
				LegacyPreferGeneratorNameImplicitIdentifierDatabaseObjectNamingStrategy.class.getName(),
				TestEntity.class
		);

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "hibernate_sequence" ) );
	}

	@Test
	public void testNoGeneratorStandardStrategy() throws Exception {
		buildMetadata( TestEntity2.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "table_generator" ) );
	}

	@Test
	public void testNoGeneratorHibernateSequenceStrategy() throws Exception {
		buildMetadata( HibernateSequenceDatabaseNamingStrategy.class.getName(), TestEntity2.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "hibernate_sequence" ) );
	}

	@Test
	public void testNoGeneratorPreferGeneratorNameStrategy() throws Exception {
		buildMetadata(
				LegacyPreferGeneratorNameImplicitIdentifierDatabaseObjectNamingStrategy.class.getName(),
				TestEntity2.class
		);

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "table_generator" ) );
	}

	@Test
	public void testGeneratorWithoutSequenceNameStandardStrategy() throws Exception {
		buildMetadata( TestEntity3.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "table_generator" ) );
	}

	@Test
	public void testGeneratorWithoutSequenceNameHibernateSequenceStrategy() throws Exception {
		buildMetadata( HibernateSequenceDatabaseNamingStrategy.class.getName(), TestEntity3.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "hibernate_sequence" ) );
	}

	@Test
	public void testGeneratorWithoutSequenceNamePreferGeneratorNameStrategy() throws Exception {
		buildMetadata(
				LegacyPreferGeneratorNameImplicitIdentifierDatabaseObjectNamingStrategy.class.getName(),
				TestEntity3.class
		);

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "table_generator" ) );
	}

	@Test
	public void testGeneratorWithSequenceNameStandardStrategy() throws Exception {
		buildMetadata( TestEntity4.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "test_sequence" ) );
	}

	@Test
	public void testGeneratorWithSequenceNameHibernateSequenceStrategy() throws Exception {
		buildMetadata( HibernateSequenceDatabaseNamingStrategy.class.getName(), TestEntity4.class );

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "test_sequence" ) );
	}

	@Test
	public void testGeneratorWithSequenceNamePreferGeneratorNameStrategy() throws Exception {
		buildMetadata(
				LegacyPreferGeneratorNameImplicitIdentifierDatabaseObjectNamingStrategy.class.getName(),
				TestEntity4.class
		);

		createSchema();

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertTrue( fileContent.contains( "test_sequence" ) );
	}


	private void createSchema() {
		final SchemaExport schemaExport = new SchemaExport();
		schemaExport.setOutputFile( output.getAbsolutePath() );
		schemaExport.create( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ), metadata );
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private Long id;

		private String name;
	}

	@Entity(name = "TestEntity2")
	public static class TestEntity2 {
		@Id
		@GeneratedValue(generator = "table_generator")
		private Long id;

		private String name;
	}

	@Entity(name = "TestEntity3")
	public static class TestEntity3 {
		@Id
		@GeneratedValue(generator = "table_generator")
		@SequenceGenerator(name = "table_generator")
		private Long id;

		private String name;
	}

	@Entity(name = "TestEntity4")
	public static class TestEntity4 {
		@Id
		@GeneratedValue(generator = "table_generator")
		@SequenceGenerator(name = "table_generator", sequenceName = "test_sequence")
		private Long id;

		private String name;
	}

	public static class HibernateSequenceDatabaseNamingStrategy
			extends StandardImplicitIdentifierDatabaseObjectNamingStrategy {

		public QualifiedName determineSequenceName(
				Identifier catalogName,
				Identifier schemaName,
				Map<?, ?> configValues,
				ServiceRegistry serviceRegistry) {
			final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );

			return new QualifiedSequenceName(
					catalogName,
					schemaName,
					jdbcEnvironment.getIdentifierHelper().toIdentifier( "hibernate_sequence" )
			);
		}

	}

	public static class LegacyPreferGeneratorNameImplicitIdentifierDatabaseObjectNamingStrategy
			extends StandardImplicitIdentifierDatabaseObjectNamingStrategy {

		public QualifiedName determineSequenceName(
				Identifier catalogName,
				Identifier schemaName,
				Map<?, ?> configValues,
				ServiceRegistry serviceRegistry) {
			final JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );

			return new QualifiedSequenceName(
					catalogName,
					schemaName,
					jdbcEnvironment.getIdentifierHelper().toIdentifier( implicitName( configValues ) )
			);
		}

		private String implicitName(Map<?, ?> configValues) {
			final String suffix = ConfigurationHelper.getString(
					CONFIG_SEQUENCE_PER_ENTITY_SUFFIX,
					configValues,
					DEF_SEQUENCE_SUFFIX
			);

			if ( !Objects.equals( suffix, DEF_SEQUENCE_SUFFIX ) ) {
				return "hibernate_sequence";
			}

			final String annotationGeneratorName = ConfigurationHelper.getString(
					IdentifierGenerator.GENERATOR_NAME,
					configValues
			);
			if ( StringHelper.isNotEmpty( annotationGeneratorName ) ) {
				return annotationGeneratorName;
			}

			return "hibernate_sequence";
		}

	}
}
