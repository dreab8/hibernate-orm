 package org.hibernate.orm.test.tool;

 import java.lang.reflect.Method;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.Set;

 import org.hibernate.boot.model.TruthValue;
 import org.hibernate.dialect.Dialect;
 import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
 import org.hibernate.engine.jdbc.internal.Formatter;
 import org.hibernate.metamodel.model.relational.internal.ColumnMappingsImpl;
 import org.hibernate.metamodel.model.relational.spi.Column;
 import org.hibernate.metamodel.model.relational.spi.ForeignKey;
 import org.hibernate.metamodel.model.relational.spi.Namespace;
 import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
 import org.hibernate.metamodel.model.relational.spi.PhysicalTable;
 import org.hibernate.metamodel.model.relational.spi.Table;
 import org.hibernate.naming.Identifier;
 import org.hibernate.naming.NamespaceName;
 import org.hibernate.naming.QualifiedTableName;
 import org.hibernate.service.Service;
 import org.hibernate.service.ServiceRegistry;
 import org.hibernate.tool.schema.extract.internal.ColumnInformationImpl;
 import org.hibernate.tool.schema.extract.internal.ForeignKeyInformationImpl;
 import org.hibernate.tool.schema.extract.internal.TableInformationImpl;
 import org.hibernate.tool.schema.extract.spi.ColumnInformation;
 import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
 import org.hibernate.tool.schema.extract.spi.ForeignKeyInformation;
 import org.hibernate.tool.schema.extract.spi.ForeignKeyInformation.ColumnReferenceMapping;
 import org.hibernate.tool.schema.extract.spi.InformationExtractor;
 import org.hibernate.tool.schema.extract.spi.NameSpaceTablesInformation;
 import org.hibernate.tool.schema.extract.spi.TableInformation;
 import org.hibernate.tool.schema.internal.AbstractSchemaMigrator;
 import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
 import org.hibernate.tool.schema.internal.exec.GenerationTarget;
 import org.hibernate.tool.schema.spi.ExecutionOptions;

 import org.junit.Assert;
 import org.junit.Test;

 import org.mockito.Mockito;

/**
 * @author Milo van der Zee
 */
public class CheckForExistingForeignKeyTest {

	private class FakeHibernateSchemaManagementTool extends HibernateSchemaManagementTool {
		private class FakeServiceRegistry implements ServiceRegistry{

			@Override
			public ServiceRegistry getParentServiceRegistry() {
				return null;
			}

			@Override
			public <R extends Service> R getService(Class<R> serviceRole) {
				return null;
			}

			@Override
			public void close() {

			}
		}
		@Override
		public ServiceRegistry getServiceRegistry() {
			return new FakeServiceRegistry();
		}

	}

	private class SchemaMigrator extends AbstractSchemaMigrator {

		/**
		 * Needed constructor.
		 */
		public SchemaMigrator() {
			super( new FakeHibernateSchemaManagementTool(), null , null);
		}

		/**
		 * Needed implementation. Not used in test.
		 */
		@Override
		protected NameSpaceTablesInformation performTablesMigration(
				DatabaseInformation existingDatabase,
				ExecutionOptions options,
				Dialect dialect,
				Formatter formatter,
				Set<String> exportIdentifiers,
				boolean tryToCreateCatalogs,
				boolean tryToCreateSchemas,
				Set<Identifier> exportedCatalogs,
				Namespace namespace,
				GenerationTarget[] targets) {
			return null;
		}
	}

	private class ColumnReferenceMappingImpl implements ColumnReferenceMapping {

		private ColumnInformation referencingColumnMetadata;
		private ColumnInformation referencedColumnMetadata;

		public ColumnReferenceMappingImpl(ColumnInformation referencingColumnMetadata, ColumnInformation referencedColumnMetadata) {
			this.referencingColumnMetadata = referencingColumnMetadata;
			this.referencedColumnMetadata = referencedColumnMetadata;
		}

		@Override
		public ColumnInformation getReferencingColumnMetadata() {
			return referencingColumnMetadata;
		}

		@Override
		public ColumnInformation getReferencedColumnMetadata() {
			return referencedColumnMetadata;
		}
	}

	private class IdentifierHelperImpl implements IdentifierHelper {

		@Override
		public Identifier normalizeQuoting(Identifier identifier) {
			return null;
		}

		@Override
		public Identifier toIdentifier(String text) {
			return null;
		}

		@Override
		public Identifier toIdentifier(String text, boolean quoted) {
			return null;
		}

		@Override
		public Identifier applyGlobalQuoting(String text) {
			return null;
		}

		@Override
		public boolean isReservedWord(String word) {
			return false;
		}

		@Override
		public String toMetaDataCatalogName(Identifier catalogIdentifier) {
			return null;
		}

		@Override
		public String toMetaDataSchemaName(Identifier schemaIdentifier) {
			return null;
		}

		@Override
		public String toMetaDataObjectName(Identifier identifier) {
			return identifier.getText();
		}
	}

	/**
	 * If the key has no name it should never be found. Result is that those keys are always recreated. But keys always
	 * have a name so this is no problem.
	 */
	@Test
	public void testForeignKeyWithoutName() throws Exception {
		// Get the private method
		Method method = AbstractSchemaMigrator.class.getDeclaredMethod( "checkForExistingForeignKey", ForeignKey.class, TableInformation.class );
		method.setAccessible( true );

		// foreignKey name with same name should match
		ForeignKey foreignKey = new ForeignKey("not_important",true, "", false,false, null,null,null);
		TableInformation tableInformation = new TableInformationImpl( null, null, null, false, null );
		boolean found = (boolean) method.invoke( new SchemaMigrator(), foreignKey, tableInformation );
		Assert.assertFalse( "Key should not be found", found );
	}

	/**
	 * Test key not found if tableinformation is missing.
	 */
	@Test
	public void testMissingTableInformation() throws Exception {
		// Get the private method
		Method method = AbstractSchemaMigrator.class.getDeclaredMethod( "checkForExistingForeignKey", ForeignKey.class, TableInformation.class );
		method.setAccessible( true );

		// foreignKey name with same name should match
		ForeignKey foreignKey = new ForeignKey("objectId2id",true,"",false,false,null,null,null);
		boolean found = (boolean) method.invoke( new SchemaMigrator(), foreignKey, null );
		Assert.assertFalse( "Key should not be found", found );
	}

	/**
	 * Check detection of existing foreign keys with the same name exists.
	 */
	@Test
	public void testKeyWithSameNameExists() throws Exception {
		// Get the private method
		Method method = AbstractSchemaMigrator.class.getDeclaredMethod( "checkForExistingForeignKey", ForeignKey.class, TableInformation.class );
		method.setAccessible( true );

		String columnName = "objectId2id";
		String tableName = "table1";
		Table referringTable;
		List<Column> referringColumns  = new ArrayList<>(  );

		QualifiedTableName referringTableName = new QualifiedTableName( null, null, new Identifier( tableName, false ) );
		referringTable = new PhysicalTable( null, referringTableName, false, "" );

		referringColumns.add( new PhysicalColumn( referringTable, new Identifier( columnName, false ), null, null, "", "", false, false, null )  );

		List<Column> targetColumns  = new ArrayList<>(  );
		QualifiedTableName targetTableName = new QualifiedTableName( null, null, new Identifier( "table2", false ) );
		Table targetTable = new PhysicalTable( null, targetTableName, false, "" );
		targetColumns.add( new PhysicalColumn( referringTable, new Identifier( "objectId2id" ,false ), null, null, "","",false,false, null )  );

		ForeignKey foreignKey = new ForeignKey("objectId2id", true, "", false, false, referringTable,null, new ColumnMappingsImpl(
				referringTable, targetTable, referringColumns, targetColumns ) );

		InformationExtractor informationExtractor = Mockito.mock( InformationExtractor.class );
		IdentifierHelper identifierHelper = new IdentifierHelperImpl();
		List<ForeignKeyInformation> fks = new ArrayList<>();
		fks.add( new ForeignKeyInformationImpl( new Identifier( "objectId2id", false ), new ArrayList<>() ) );
		Mockito.when( informationExtractor.getForeignKeys( Mockito.any() ) ).thenReturn( fks );
		NamespaceName schemaName = new NamespaceName( new Identifier( "-", false ), new Identifier( "-", false ) );
		TableInformation tableInformation = new TableInformationImpl( informationExtractor, identifierHelper, referringTableName, false, null );

		// foreignKey name with same name should match
		boolean found = (boolean) method.invoke( new SchemaMigrator(), foreignKey, tableInformation );
		Assert.assertTrue( "Key should be found", found );
	}

	/**
	 * Check detection of existing foreign keys with the same name exists.
	 */
//	@Test
//	public void testKeyWithSameNameNotExists() throws Exception {
//		// Get the private method
//		Method method = AbstractSchemaMigrator.class.getDeclaredMethod( "checkForExistingForeignKey", ForeignKey.class, TableInformation.class );
//		method.setAccessible( true );
//
//		ForeignKey foreignKey = new ForeignKey();
//		foreignKey.setName( "objectId2id_1" );
//		foreignKey.addColumn( new Column( "id", true ) );
//		foreignKey.setReferencedTable( new Table( "table2" ) );
//
//		InformationExtractor informationExtractor = Mockito.mock( InformationExtractor.class );
//		IdentifierHelper identifierHelper = new IdentifierHelperImpl();
//		List<ForeignKeyInformation> fks = new ArrayList<>();
//		fks.add( new ForeignKeyInformationImpl( new Identifier( "objectId2id_2", false ), new ArrayList<>() ) );
//		Mockito.when( informationExtractor.getForeignKeys( Mockito.any() ) ).thenReturn( fks );
//		NamespaceName schemaName = new NamespaceName( new Identifier( "-", false ), new Identifier( "-", false ) );
//		QualifiedTableName tableName = new QualifiedTableName( schemaName, new Identifier( "-", false ) );
//		TableInformation tableInformation = new TableInformationImpl( informationExtractor, identifierHelper, tableName, false, null );
//
//		// foreignKey name with same name should match
//		boolean found = (boolean) method.invoke( new SchemaMigrator(), foreignKey, tableInformation );
//		Assert.assertFalse( "Key should not be found", found );
//	}
//
//	/**
//	 * Check detection of existing foreign key with the same mappings for a simple mapping (table1.objectId =>
//	 * table2.id).
//	 */
//	@Test
//	public void testCheckForExistingForeignKeyOne2One() throws Exception {
//		// Get the private method
//		Method method = AbstractSchemaMigrator.class.getDeclaredMethod( "checkForExistingForeignKey", ForeignKey.class, TableInformation.class );
//		method.setAccessible( true );
//
//		ForeignKey foreignKey = new ForeignKey();
//		foreignKey.setName( "objectId2id_1" ); // Make sure the match is not successful based on key name
//		foreignKey.addColumn( new Column( "id", true ) );
//		foreignKey.setReferencedTable( new Table( "table2" ) );
//
//		NamespaceName schemaName = new NamespaceName( new Identifier( "-", false ), new Identifier( "-", false ) );
//		InformationExtractor informationExtractor = Mockito.mock( InformationExtractor.class );
//		IdentifierHelper identifierHelper = new IdentifierHelperImpl();
//		List<ForeignKeyInformation> fks = new ArrayList<>();
//		fks.add( getForeignKeyInformation( "table2", "id", "object2Id_2" ) );
//		Mockito.when( informationExtractor.getForeignKeys( Mockito.any() ) ).thenReturn( fks );
//		QualifiedTableName tableName = new QualifiedTableName( schemaName, new Identifier( "-", false ) );
//		TableInformation tableInformation = new TableInformationImpl( informationExtractor, identifierHelper, tableName, false, null );
//		AbstractSchemaMigrator schemaMigrator = new SchemaMigrator();
//
//		// Check single-column-key to single-column-key, existing (table1.objectId => table2.id)
//		boolean found = (boolean) method.invoke( schemaMigrator, foreignKey, tableInformation );
//		Assert.assertTrue( "Key should be found", found );
//	}
//
//	/**
//	 * Check detection of not existing foreign key with the same mappings for a simple mapping (table1.objectId =>
//	 * table2.id).
//	 */
//	@Test
//	public void testCheckForNotExistingForeignKeyOne2One() throws Exception {
//		// Get the private method
//		Method method = AbstractSchemaMigrator.class.getDeclaredMethod( "checkForExistingForeignKey", ForeignKey.class, TableInformation.class );
//		method.setAccessible( true );
//
//		ForeignKey foreignKey = new ForeignKey();
//		foreignKey.setName( "objectId2id_1" ); // Make sure the match is not successful based on key name
//		foreignKey.addColumn( new Column( "id", true ) );
//		foreignKey.setReferencedTable( new Table( "table2" ) );
//
//		NamespaceName schemaName = new NamespaceName( new Identifier( "-", false ), new Identifier( "-", false ) );
//		InformationExtractor informationExtractor = Mockito.mock( InformationExtractor.class );
//		IdentifierHelper identifierHelper = new IdentifierHelperImpl();
//		List<ForeignKeyInformation> fks = new ArrayList<>();
//		fks.add( getForeignKeyInformation( "table2", "blah", "blahKey_001" ) );
//		fks.add( getForeignKeyInformation( "table3", "id", "blahKey_002" ) );
//		fks.add( getForeignKeyInformation( "table3", "blah", "blahKey_003" ) );
//		Mockito.when( informationExtractor.getForeignKeys( Mockito.any() ) ).thenReturn( fks );
//		QualifiedTableName tableName = new QualifiedTableName( schemaName, new Identifier( "-", false ) );
//		TableInformation tableInformation = new TableInformationImpl( informationExtractor, identifierHelper, tableName, false, null );
//		AbstractSchemaMigrator schemaMigrator = new SchemaMigrator();
//
//		// Check single-column-key to single-column-key, existing (table1.objectId => table2.id)
//		boolean found = (boolean) method.invoke( schemaMigrator, foreignKey, tableInformation );
//		Assert.assertFalse( "Key should not be found", found );
//	}

	/**
	 * @param referencedTableName - String
	 * @param referencingColumnName - String
	 * @param keyName - String
	 * @return ForeignKeyInformation
	 */
	private ForeignKeyInformation getForeignKeyInformation(
			String referencedTableName,
			String referencingColumnName,
			String keyName) {
		List<ColumnReferenceMapping> columnMappingList = new ArrayList<>();
		ColumnInformation referencingColumnMetadata = getColumnInformation( "-", referencingColumnName );
		ColumnInformation referencedColumnMetadata = getColumnInformation( referencedTableName, "-" );
		ColumnReferenceMapping columnReferenceMapping = new ColumnReferenceMappingImpl( referencingColumnMetadata, referencedColumnMetadata );
		columnMappingList.add( columnReferenceMapping );
		ForeignKeyInformationImpl foreignKeyInformation = new ForeignKeyInformationImpl( new Identifier( keyName, false ), columnMappingList );
		return foreignKeyInformation;
	}

	private ColumnInformation getColumnInformation(String tableName, String columnName) {
		NamespaceName schemaName = new NamespaceName( new Identifier( "-", false ), new Identifier( "-", false ) );
		TableInformation containingTableInformation = new TableInformationImpl( null, null,
				new QualifiedTableName( schemaName, new Identifier( tableName, false ) ), false, null );
		Identifier columnIdentifier = new Identifier( columnName, false );
		int typeCode = 0;
		String typeName = null;
		int columnSize = 0;
		int decimalDigits = 0;
		TruthValue nullable = null;
		ColumnInformationImpl columnInformation = new ColumnInformationImpl( containingTableInformation, columnIdentifier, typeCode, typeName, columnSize,
				decimalDigits, nullable );
		return columnInformation;
	}
}
