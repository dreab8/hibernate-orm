/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.schemaupdate;

import java.util.EnumSet;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.Helper;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Max Rydahl Andersen
 * @author Brett Meyer
 */
public class MigrationTest extends BaseUnitTestCase {
	private ServiceRegistry serviceRegistry;

	@Before
	public void setUp() {
		serviceRegistry = new StandardServiceRegistryBuilder().build();
	}

	@After
	public void tearDown() {
		StandardServiceRegistryBuilder.destroy( serviceRegistry );
		serviceRegistry = null;
	}

	@Test
	public void testSimpleColumnAddition() {
		String resource1 = "org/hibernate/orm/test/schemaupdate/1_Version.hbm.xml";
		String resource2 = "org/hibernate/orm/test/schemaupdate/2_Version.hbm.xml";

		final MetadataImplementor v1metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
				.addResource( resource1 )
				.buildMetadata();

		final DatabaseModel databaseModel = Helper.buildDatabaseModel( v1metadata );

		new SchemaExport( databaseModel, serviceRegistry ).drop( EnumSet.of( TargetType.DATABASE ) );

		final SchemaUpdate v1schemaUpdate = new SchemaUpdate( databaseModel, serviceRegistry );
		v1schemaUpdate.execute( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ) );

		assertEquals( 0, v1schemaUpdate.getExceptions().size() );

		final MetadataImplementor v2metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
				.addResource( resource2 )
				.buildMetadata();

		final DatabaseModel v2DatabaseModel = Helper.buildDatabaseModel( v2metadata );
		final SchemaUpdate v2schemaUpdate = new SchemaUpdate( v2DatabaseModel, serviceRegistry);
		v2schemaUpdate.execute(	EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ));
		assertEquals( 0, v2schemaUpdate.getExceptions().size() );

		new SchemaExport(v2DatabaseModel, serviceRegistry).drop( EnumSet.of( TargetType.DATABASE ) );

	}

//	/**
//	 * 3_Version.hbm.xml contains a named unique constraint and an un-named
//	 * unique constraint (will receive a randomly-generated name).  Create
//	 * the original schema with 2_Version.hbm.xml.  Then, run SchemaUpdate
//	 * TWICE using 3_Version.hbm.xml.  Neither RECREATE_QUIETLY nor SKIP should
//	 * generate any exceptions.
//	 */
//	@Test
//	@TestForIssue( jiraKey = "HHH-8162" )
//	public void testConstraintUpdate() {
//		doConstraintUpdate(UniqueConstraintSchemaUpdateStrategy.DROP_RECREATE_QUIETLY);
//		doConstraintUpdate(UniqueConstraintSchemaUpdateStrategy.RECREATE_QUIETLY);
//		doConstraintUpdate(UniqueConstraintSchemaUpdateStrategy.SKIP);
//	}
//
//	private void doConstraintUpdate(UniqueConstraintSchemaUpdateStrategy strategy) {
//		// original
//		String resource1 = "org/hibernate/test/schemaupdate/2_Version.hbm.xml";
//		// adds unique constraint
//		String resource2 = "org/hibernate/test/schemaupdate/3_Version.hbm.xml";
//
//		MetadataImplementor v1metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
//				.addResource( resource1 )
//				.buildMetadata();
//		MetadataImplementor v2metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
//				.addResource( resource2 )
//				.buildMetadata();
//
//		new SchemaExport( v1metadata ).execute( false, true, true, false );
//
//		// adds unique constraint
//		Configuration v2cfg = new Configuration();
//		v2cfg.getProperties().put( AvailableSettings.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY, strategy );
//		v2cfg.addResource( resource2 );
//		SchemaUpdate v2schemaUpdate = new SchemaUpdate( serviceRegistry, v2cfg );
//		v2schemaUpdate.execute( true, true );
//		assertEquals( 0, v2schemaUpdate.getExceptions().size() );
//
//		Configuration v3cfg = new Configuration();
//		v3cfg.getProperties().put( AvailableSettings.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY, strategy );
//		v3cfg.addResource( resource2 );
//		SchemaUpdate v3schemaUpdate = new SchemaUpdate( serviceRegistry, v3cfg );
//		v3schemaUpdate.execute( true, true );
//		assertEquals( 0, v3schemaUpdate.getExceptions().size() );
//
//		new SchemaExport( serviceRegistry, v3cfg ).drop( false, true );
//	}


	@Test
	@TestForIssue(jiraKey = "HHH-9713")
	public void testIndexCreationViaSchemaUpdate() {
		final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
				.addAnnotatedClass( EntityWithIndex.class )
				.buildMetadata();
		final DatabaseModel databaseModel = Helper.buildDatabaseModel( metadata );

		// drop and then create the schema
		new SchemaExport(databaseModel, serviceRegistry).execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.BOTH );

		try {
			// update the schema
			new SchemaUpdate(databaseModel, serviceRegistry).execute( EnumSet.of( TargetType.DATABASE ) );
		}
		finally {
			// drop the schema
			new SchemaExport(databaseModel, serviceRegistry).drop( EnumSet.of( TargetType.DATABASE ) );
		}
	}

	@Entity(name = "EntityWithIndex")
	@Table(name = "T_Entity_With_Index", indexes = @Index(columnList = "name"))
	public static class EntityWithIndex {
		@Id
		public Integer id;
		public String name;
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9550")
	public void testSameTableNameDifferentExplicitSchemas() {
		final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
				.addAnnotatedClass( CustomerInfo.class )
				.addAnnotatedClass( PersonInfo.class )
				.buildMetadata();
		final DatabaseModel databaseModel = Helper.buildDatabaseModel( metadata );

		// drop and then create the schema
		new SchemaExport(databaseModel, serviceRegistry).execute( EnumSet.of( TargetType.DATABASE ), SchemaExport.Action.BOTH );

		try {
			// update the schema
			new SchemaUpdate(databaseModel, serviceRegistry).execute( EnumSet.of( TargetType.DATABASE ) );
		}
		finally {
			// drop the schema
			new SchemaExport(databaseModel, serviceRegistry).drop( EnumSet.of( TargetType.DATABASE ) );
		}
	}

	@Entity
	@Table(name = "PERSON", schema = "CRM")
	public static class CustomerInfo {
		@Id
		private Integer id;
	}

	@Entity
	@Table(name = "PERSON", schema = "ERP")
	public static class PersonInfo {
		@Id
		private Integer id;
	}


}

