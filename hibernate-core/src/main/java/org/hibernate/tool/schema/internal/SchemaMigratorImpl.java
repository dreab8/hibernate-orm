/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Constraint;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.ForeignKeyInformation;
import org.hibernate.tool.schema.extract.spi.IndexInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaMigrator;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.jboss.logging.Logger;

import static org.hibernate.cfg.AvailableSettings.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY;


/**
 * @author Steve Ebersole
 */
public class SchemaMigratorImpl implements SchemaMigrator {
	private static final Logger log = Logger.getLogger( SchemaMigratorImpl.class );

	private final HibernateSchemaManagementTool tool;
	private final SchemaFilter schemaFilter;

	private UniqueConstraintSchemaUpdateStrategy uniqueConstraintStrategy;

	public SchemaMigratorImpl(HibernateSchemaManagementTool tool) {
		this( tool, DefaultSchemaFilter.INSTANCE );
	}

	public SchemaMigratorImpl(HibernateSchemaManagementTool tool, SchemaFilter schemaFilter) {
		this.tool = tool;
		this.schemaFilter = schemaFilter;
	}

	/**
	 * For testing...
	 */
	public void setUniqueConstraintStrategy(UniqueConstraintSchemaUpdateStrategy uniqueConstraintStrategy) {
		this.uniqueConstraintStrategy = uniqueConstraintStrategy;
	}

	@Override
	public void doMigration(Metadata metadata, ExecutionOptions options, TargetDescriptor targetDescriptor) {
		if ( targetDescriptor.getTargetTypes().isEmpty() ) {
			return;
		}

		final JdbcContext jdbcContext = tool.resolveJdbcContext( options.getConfigurationValues() );

		final DdlTransactionIsolator ddlTransactionIsolator = tool.getDdlTransactionIsolator( jdbcContext );

		try {
			ddlTransactionIsolator.prepare();

			final DdlTransactionIsolator sharedDdlTransactionIsolator = new DdlTransactionIsolatorSharedImpl( ddlTransactionIsolator );

			final DatabaseInformation databaseInformation = Helper.buildDatabaseInformation(
					tool.getServiceRegistry(),
					sharedDdlTransactionIsolator,
					metadata.getDatabase().getDefaultNamespace().getName()
			);

			final GenerationTarget[] targets = tool.buildGenerationTargets(
					targetDescriptor,
					sharedDdlTransactionIsolator,
					options.getConfigurationValues()
			);

			try {
				doMigration( metadata, databaseInformation, options, jdbcContext.getDialect(), targets );
			}
			finally {
				try {
					databaseInformation.cleanup();
				}
				catch (Exception e) {
					log.debug( "Problem releasing DatabaseInformation : " + e.getMessage() );
				}
			}
		}
		finally {
			ddlTransactionIsolator.release();
		}
	}

	public void doMigration(
			Metadata metadata,
			DatabaseInformation existingDatabase,
			ExecutionOptions options,
			Dialect dialect,
			GenerationTarget... targets) {
		for ( GenerationTarget target : targets ) {
			target.prepare();
		}

		try {
			performMigration( metadata, existingDatabase, options, dialect, targets );
		}
		finally {
			for ( GenerationTarget target : targets ) {
				try {
					target.release();
				}
				catch (Exception e) {
					log.debugf( "Problem releasing GenerationTarget [%s] : %s", target, e.getMessage() );
				}
			}
		}
	}

	private void performMigration(
			Metadata metadata,
			DatabaseInformation existingDatabase,
			ExecutionOptions options,
			Dialect dialect,
			GenerationTarget... targets) {
		final boolean format = Helper.interpretFormattingEnabled( options.getConfigurationValues() );
		final Formatter formatter = format ? FormatStyle.DDL.getFormatter() : FormatStyle.NONE.getFormatter();

		final Set<String> exportIdentifiers = new HashSet<String>( 50 );

		final Database database = metadata.getDatabase();

		// Drop all AuxiliaryDatabaseObjects
		database.getAuxiliaryDatabaseObjects().parallelStream().forEach( auxiliaryDatabaseObject -> {
			if ( auxiliaryDatabaseObject.appliesToDialect( dialect ) ) {
				applySqlStrings(
						true,
						dialect.getAuxiliaryDatabaseObjectExporter()
								.getSqlDropStrings( auxiliaryDatabaseObject, metadata ),
						formatter,
						options,
						targets
				);
			}
		} );

		// Create beforeQuery-table AuxiliaryDatabaseObjects
		database.getAuxiliaryDatabaseObjects().parallelStream().forEach( auxiliaryDatabaseObject -> {
			if ( !auxiliaryDatabaseObject.beforeTablesOnCreation() && auxiliaryDatabaseObject.appliesToDialect( dialect ) ) {
				applySqlStrings(
						true,
						auxiliaryDatabaseObject.sqlCreateStrings( dialect ),
						formatter,
						options,
						targets
				);
			}
		} );

		boolean tryToCreateCatalogs = false;
		boolean tryToCreateSchemas = false;
		if ( options.shouldManageNamespaces() ) {
			if ( dialect.canCreateSchema() ) {
				tryToCreateSchemas = true;
			}
			if ( dialect.canCreateCatalog() ) {
				tryToCreateCatalogs = true;
			}
		}

		Set<Identifier> exportedCatalogs = new HashSet<>();
		for ( Namespace namespace : database.getNamespaces() ) {
			if ( schemaFilter.includeNamespace( namespace ) ) {
				if ( tryToCreateCatalogs || tryToCreateSchemas ) {
					if ( tryToCreateCatalogs ) {
						final Identifier catalogLogicalName = namespace.getName().getCatalog();
						final Identifier catalogPhysicalName = namespace.getPhysicalName().getCatalog();

						if ( catalogPhysicalName != null
								&& !exportedCatalogs.contains( catalogLogicalName )
								&& !existingDatabase.catalogExists( catalogLogicalName )
								) {
							applySqlStrings(
									false,
									dialect.getCreateCatalogCommand( catalogPhysicalName.render( dialect ) ),
									formatter,
									options,
									targets
							);
							exportedCatalogs.add( catalogLogicalName );
						}
					}

					if ( tryToCreateSchemas
							&& namespace.getPhysicalName().getSchema() != null
							&& !existingDatabase.schemaExists( namespace.getName() ) ) {
						applySqlStrings(
								false,
								dialect.getCreateSchemaCommand(
										namespace.getPhysicalName()
												.getSchema()
												.render( dialect )
								),
								formatter,
								options,
								targets
						);
					}
				}
			}
		}

		database.getNamespaces().parallelStream().forEach( namespace -> {
			namespace.getTables().parallelStream().forEach( table -> {
				if ( table.isPhysicalTable() && schemaFilter.includeTable( table ) ) {
					checkExportIdentifier( table, exportIdentifiers );
					final TableInformation tableInformation = existingDatabase.getTableInformation( table.getQualifiedTableName() );
					if ( tableInformation != null && tableInformation.isPhysicalTable() ) {
						migrateTable( table, tableInformation, dialect, metadata, formatter, options, targets );
					}
					else if ( tableInformation == null ) {
						createTable( table, dialect, metadata, formatter, options, targets );
					}
				}
			} );

			namespace.getTables().parallelStream().forEach( table -> {
				if ( table.isPhysicalTable() && schemaFilter.includeTable( table ) ) {
					final TableInformation tableInformation = existingDatabase.getTableInformation( table.getQualifiedTableName() );
					if ( (tableInformation != null && tableInformation.isPhysicalTable()) || tableInformation == null ) {
						applyIndexes( table, tableInformation, dialect, metadata, formatter, options, targets );
						applyUniqueKeys( table, tableInformation, dialect, metadata, formatter, options, targets );
					}
				}
			} );

			namespace.getSequences().parallelStream().forEach( sequence -> {
				checkExportIdentifier( sequence, exportIdentifiers );
				final SequenceInformation sequenceInformation = existingDatabase.getSequenceInformation( sequence.getName() );
				if ( sequenceInformation == null ) {
					applySqlStrings(
							false,
							dialect.getSequenceExporter().getSqlCreateStrings(
									sequence,
									metadata
							),
							formatter,
							options,
							targets
					);
				}
			} );
		} );

		//NOTE : Foreign keys must be created *afterQuery* all tables of all namespaces for cross namespace fks. see HHH-10420
		database.getNamespaces().parallelStream().forEach( namespace -> {
			if ( schemaFilter.includeNamespace( namespace ) ) {
				for ( Table table : namespace.getTables() ) {
					if ( schemaFilter.includeTable( table ) ) {
						final TableInformation tableInformation = existingDatabase.getTableInformation( table.getQualifiedTableName() );
						if ( (tableInformation != null && tableInformation.isPhysicalTable()) || tableInformation == null ) {
							applyForeignKeys( table, tableInformation, dialect, metadata, formatter, options, targets );
						}
					}
				}
			}
		} );

		// Create afterQuery-table AuxiliaryDatabaseObjects
		database.getAuxiliaryDatabaseObjects().parallelStream().forEach( auxiliaryDatabaseObject -> {
			if ( auxiliaryDatabaseObject.beforeTablesOnCreation() && auxiliaryDatabaseObject.appliesToDialect( dialect ) ) {
				applySqlStrings(
						true,
						auxiliaryDatabaseObject.sqlCreateStrings( dialect ),
						formatter,
						options,
						targets
				);
			}
		} );
	}

	private void createTable(
			Table table,
			Dialect dialect,
			Metadata metadata,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		applySqlStrings(
				false,
				dialect.getTableExporter().getSqlCreateStrings( table, metadata ),
				formatter,
				options,
				targets
		);
	}

	private void migrateTable(
			Table table,
			TableInformation tableInformation,
			Dialect dialect,
			Metadata metadata,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		final Database database = metadata.getDatabase();

		//noinspection unchecked
		applySqlStrings(
				false,
				table.sqlAlterStrings(
						dialect,
						metadata,
						tableInformation,
						getDefaultCatalogName( database, dialect ),
						getDefaultSchemaName( database, dialect )
				),
				formatter,
				options,
				targets
		);
	}

	private void applyIndexes(
			Table table,
			TableInformation tableInformation,
			Dialect dialect,
			Metadata metadata,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		final Exporter<Index> exporter = dialect.getIndexExporter();

		table.getIndexes().parallelStream().forEach( index -> {
			if ( !StringHelper.isEmpty( index.getName() ) ) {
				IndexInformation existingIndex = null;
				if ( tableInformation != null ) {
					existingIndex = findMatchingIndex( index, tableInformation );

				}
				if ( existingIndex == null ) {
					applySqlStrings(
							false,
							exporter.getSqlCreateStrings( index, metadata ),
							formatter,
							options,
							targets
					);
				}
			}
		} );
	}

	private IndexInformation findMatchingIndex(Index index, TableInformation tableInformation) {
		return tableInformation.getIndex( Identifier.toIdentifier( index.getName() ) );
	}

	private void applyUniqueKeys(
			Table table,
			TableInformation tableInfo,
			Dialect dialect,
			Metadata metadata,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		if ( uniqueConstraintStrategy == null ) {
			uniqueConstraintStrategy = determineUniqueConstraintSchemaUpdateStrategy( metadata );
		}

		if ( uniqueConstraintStrategy == UniqueConstraintSchemaUpdateStrategy.SKIP ) {
			return;
		}

		final Exporter<Constraint> exporter = dialect.getUniqueKeyExporter();

		table.getUniqueKeys().values().parallelStream().forEach( uniqueKey ->{
			// Skip if index already exists. Most of the time, this
			// won't work since most Dialects use Constraints. However,
			// keep it for the few that do use Indexes.
			IndexInformation indexInfo = null;
			if ( tableInfo != null && StringHelper.isNotEmpty( uniqueKey.getName() ) ) {
				indexInfo = tableInfo.getIndex( Identifier.toIdentifier( uniqueKey.getName() ) );
			}
			if ( indexInfo == null ) {

				if ( uniqueConstraintStrategy == UniqueConstraintSchemaUpdateStrategy.DROP_RECREATE_QUIETLY ) {
					applySqlStrings(
							true,
							exporter.getSqlDropStrings( uniqueKey, metadata ),
							formatter,
							options,
							targets
					);
				}

				applySqlStrings(
						true,
						exporter.getSqlCreateStrings( uniqueKey, metadata ),
						formatter,
						options,
						targets
				);
			}
		} );
	}

	private UniqueConstraintSchemaUpdateStrategy determineUniqueConstraintSchemaUpdateStrategy(Metadata metadata) {
		final ConfigurationService cfgService = ((MetadataImplementor) metadata).getMetadataBuildingOptions()
				.getServiceRegistry()
				.getService( ConfigurationService.class );

		return UniqueConstraintSchemaUpdateStrategy.interpret(
				cfgService.getSetting( UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY, StandardConverters.STRING )
		);
	}

	private void applyForeignKeys(
			Table table,
			TableInformation tableInformation,
			Dialect dialect,
			Metadata metadata,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		if ( !dialect.hasAlterTable() ) {
			return;
		}

		final Exporter<ForeignKey> exporter = dialect.getForeignKeyExporter();

		table.getForeignKeys().values().parallelStream().forEach( foreignKey -> {
			if ( foreignKey.isPhysicalConstraint() && foreignKey.isCreationEnabled() ) {
				ForeignKeyInformation existingForeignKey = null;
				if ( tableInformation != null ) {
					existingForeignKey = findMatchingForeignKey(
							foreignKey,
							tableInformation
					);
				}
				if ( existingForeignKey == null ) {
					applySqlStrings(
							false,
							exporter.getSqlCreateStrings( foreignKey, metadata ),
							formatter,
							options,
							targets
					);
				}
				// todo : shouldn't we just drop+recreate if FK exists?
				//		this follows the existing code from legacy SchemaUpdate which just skipped

				// in old SchemaUpdate code, this was the trigger to "create"
			}
		} );
	}

	private ForeignKeyInformation findMatchingForeignKey(ForeignKey foreignKey, TableInformation tableInformation) {
		if ( foreignKey.getName() == null ) {
			return null;
		}
		return tableInformation.getForeignKey( Identifier.toIdentifier( foreignKey.getName() ) );
	}

	private void checkExportIdentifier(Exportable exportable, Set<String> exportIdentifiers) {
		final String exportIdentifier = exportable.getExportIdentifier();
		if ( exportIdentifiers.contains( exportIdentifier ) ) {
			throw new SchemaManagementException(
					String.format(
							"Export identifier [%s] encountered more than once",
							exportIdentifier
					)
			);
		}
		exportIdentifiers.add( exportIdentifier );
	}

	private static void applySqlStrings(
			boolean quiet,
			String[] sqlStrings,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		if ( sqlStrings == null ) {
			return;
		}
		Arrays.asList( sqlStrings ).parallelStream().forEach( sqlString -> {
			applySqlString( quiet, sqlString, formatter, options, targets );
		} );
	}

	private static void applySqlString(
			boolean quiet,
			String sqlString,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		if ( StringHelper.isEmpty( sqlString ) ) {
			return;
		}
		Arrays.asList(targets).parallelStream().forEach( target -> {
			try {
				target.accept( formatter.format( sqlString ) );
			}
			catch (CommandAcceptanceException e) {
				if ( !quiet ) {
					options.getExceptionHandler().handleException( e );
				}
				// otherwise ignore the exception
			}
		} );
	}

	private static void applySqlStrings(
			boolean quiet,
			List<String> sqlStrings,
			Formatter formatter,
			ExecutionOptions options,
			GenerationTarget... targets) {
		if ( sqlStrings == null ) {
			return;
		}
		sqlStrings.parallelStream().forEach( sqlString -> {
			applySqlString( quiet, sqlString, formatter, options, targets );
		} );
	}

	private String getDefaultCatalogName(Database database, Dialect dialect) {
		final Identifier identifier = database.getDefaultNamespace().getPhysicalName().getCatalog();
		return identifier == null ? null : identifier.render( dialect );
	}

	private String getDefaultSchemaName(Database database, Dialect dialect) {
		final Identifier identifier = database.getDefaultNamespace().getPhysicalName().getSchema();
		return identifier == null ? null : identifier.render( dialect );
	}
}
