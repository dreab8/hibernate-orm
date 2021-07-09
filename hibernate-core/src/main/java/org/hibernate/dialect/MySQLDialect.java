/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.LockOptions;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.query.NullOrdering;
import org.hibernate.query.NullPrecedence;
import org.hibernate.PessimisticLockException;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.hint.IndexQueryHintHandler;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.MySQLIdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitLimitHandler;
import org.hibernate.dialect.sequence.NoSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.unique.MySQLUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.SqlExpressable;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.internal.idtable.AfterUseAction;
import org.hibernate.query.sqm.mutation.internal.idtable.IdTable;
import org.hibernate.query.sqm.mutation.internal.idtable.LocalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.internal.idtable.TempIdTableExporter;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeDescriptorRegistry;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import javax.persistence.TemporalType;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;

/**
 * An SQL dialect for MySQL (prior to 5.x).
 *
 * @author Gavin King
 */
public class MySQLDialect extends Dialect {

	private final UniqueDelegate uniqueDelegate;
	private final MySQLStorageEngine storageEngine;
	private final int version;
	private final int characterSetBytesPerCharacter;
	private final SizeStrategy sizeStrategy;

	public MySQLDialect(DialectResolutionInfo info) {
		this(
				info.getDatabaseMajorVersion() * 100 + info.getDatabaseMinorVersion() * 10,
				getCharacterSetBytesPerCharacter( info.unwrap( DatabaseMetaData.class ) )
		);
	}

	public MySQLDialect() {
		this( 500 );
	}

	public MySQLDialect(int version) {
		// Let's be conservative and assume people use a 4 byte character set
		this( version, 4 );
	}

	public MySQLDialect(int version, int characterSetBytesPerCharacter) {
		super();
		this.version = version;
		this.characterSetBytesPerCharacter = characterSetBytesPerCharacter;

		String storageEngine = Environment.getProperties().getProperty( Environment.STORAGE_ENGINE );
		if (storageEngine == null) {
			storageEngine = System.getProperty( Environment.STORAGE_ENGINE );
		}
		if (storageEngine == null) {
			this.storageEngine = getDefaultMySQLStorageEngine();
		}
		else if( "innodb".equalsIgnoreCase( storageEngine ) ) {
			this.storageEngine = InnoDBStorageEngine.INSTANCE;
		}
		else if( "myisam".equalsIgnoreCase( storageEngine ) ) {
			this.storageEngine = MyISAMStorageEngine.INSTANCE;
		}
		else {
			throw new UnsupportedOperationException( "The " + storageEngine + " storage engine is not supported!" );
		}

		registerColumnType( Types.BOOLEAN, "bit" ); // HHH-6935: Don't use "boolean" i.e. tinyint(1) due to JDBC ResultSetMetaData

		registerColumnType( Types.NUMERIC, "decimal($p,$s)" ); //it's just a synonym

		if ( getMySQLVersion() < 570) {
			registerColumnType( Types.TIMESTAMP, "datetime" );
			registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "timestamp" );
		}
		else {
			// Since 5.7 we can explicitly specify a fractional second
			// precision for the timestamp-like types
			registerColumnType(Types.TIMESTAMP, "datetime($p)");
			registerColumnType(Types.TIMESTAMP_WITH_TIMEZONE, "timestamp($p)");
		}

		// max length for VARCHAR changed in 5.0.3
		final int maxVarcharLen = getMaxVarcharLen();

		registerColumnType( Types.VARCHAR, maxVarcharLen, "varchar($l)" );
		registerColumnType( Types.VARBINARY, maxVarcharLen, "varbinary($l)" );

		final int maxTinyLobLen = 255;
		final int maxLobLen = 65_535;
		final int maxMediumLobLen = 16_777_215;
		final long maxLongLobLen = 4_294_967_295L;

		registerColumnType( Types.VARCHAR, maxLongLobLen, "longtext" );
		registerColumnType( Types.VARCHAR, maxMediumLobLen, "mediumtext" );
		if ( maxVarcharLen < maxLobLen ) {
			registerColumnType( Types.VARCHAR, maxLobLen, "text" );
		}

		registerColumnType( Types.VARBINARY, maxLongLobLen, "longblob" );
		registerColumnType( Types.VARBINARY, maxMediumLobLen, "mediumblob" );
		if ( maxVarcharLen < maxLobLen ) {
			registerColumnType( Types.VARBINARY, maxLobLen, "blob" );
		}

		registerColumnType( Types.BLOB, maxLongLobLen, "longblob" );
		registerColumnType( Types.BLOB, maxMediumLobLen, "mediumblob" );
		registerColumnType( Types.BLOB, maxLobLen, "blob" );
		registerColumnType( Types.BLOB, maxTinyLobLen, "tinyblob" );

		registerColumnType( Types.CLOB, maxLongLobLen, "longtext" );
		registerColumnType( Types.CLOB, maxMediumLobLen, "mediumtext" );
		registerColumnType( Types.CLOB, maxLobLen, "text" );
		registerColumnType( Types.CLOB, maxTinyLobLen, "tinytext" );

		registerColumnType( Types.NCLOB, "longtext" );
		registerColumnType( Types.NCLOB, maxLongLobLen, "longtext" );
		registerColumnType( Types.NCLOB, maxMediumLobLen, "mediumtext" );
		registerColumnType( Types.NCLOB, maxLobLen, "text" );
		registerColumnType( Types.NCLOB, maxTinyLobLen, "tinytext" );

		if ( getMySQLVersion() >= 570) {
			// MySQL 5.7 brings JSON native support with a dedicated datatype
			// https://dev.mysql.com/doc/refman/5.7/en/json.html
			registerColumnType(Types.JAVA_OBJECT, "json");
		}

		registerKeyword( "key" );

		getDefaultProperties().setProperty( Environment.MAX_FETCH_DEPTH, "2" );
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );

		uniqueDelegate = new MySQLUniqueDelegate( this );
		sizeStrategy = new SizeStrategyImpl() {
			@Override
			public Size resolveSize(
					JdbcTypeDescriptor jdbcType,
					JavaTypeDescriptor javaType,
					Integer precision,
					Integer scale,
					Long length) {
				final int jdbcTypeCode = jdbcType.getJdbcType();
				switch ( jdbcTypeCode ) {
					case Types.BIT:
						// MySQL allows BIT with a length up to 64
						if ( length != null ) {
							return Size.length( Math.min( Math.max( length, 1 ), 64 ) );
						}
				}
				return super.resolveSize( jdbcType, javaType, precision, scale, length );
			}
		};
	}

	protected static int getCharacterSetBytesPerCharacter(DatabaseMetaData databaseMetaData) {
		if ( databaseMetaData != null ) {
			try (java.sql.Statement s = databaseMetaData.getConnection().createStatement() ) {
				final ResultSet rs = s.executeQuery( "SELECT @@character_set_database" );
				if ( rs.next() ) {
					final String characterSet = rs.getString( 1 );
					final int collationIndex = characterSet.indexOf( '_' );
					// According to https://dev.mysql.com/doc/refman/8.0/en/charset-charsets.html
					switch ( collationIndex == -1 ? characterSet : characterSet.substring( 0, collationIndex ) ) {
						case "utf16":
						case "utf16le":
						case "utf32":
						case "utf8mb4":
						case "gb18030":
							return 4;
						case "utf8":
						case "utf8mb3":
						case "eucjpms":
						case "ujis":
							return 3;
						case "ucs2":
						case "cp932":
						case "big5":
						case "euckr":
						case "gb2312":
						case "gbk":
						case "sjis":
							return 2;
						default:
							return 1;
					}
				}
			}
			catch (SQLException ex) {
				// Ignore
			}
		}
		return 4;
	}

	protected int getMaxVarcharLen() {
		if ( getMySQLVersion() < 500 ) {
			return 255;
		}
		else {
			switch ( characterSetBytesPerCharacter ) {
				case 1:
					return 65_535;
				case 2:
					return 32_767;
				case 3:
					return 21_844;
				case 4:
				default:
					return 16_383;
			}
		}
	}

	@Override
	public String getNullColumnString(String columnType) {
		// Good job MySQL https://dev.mysql.com/doc/refman/8.0/en/timestamp-initialization.html
		// If the explicit_defaults_for_timestamp system variable is enabled, TIMESTAMP columns permit NULL values only if declared with the NULL attribute.
		if ( columnType.regionMatches( true, 0, "timestamp", 0, "timestamp".length() ) ) {
			return " null";
		}
		return super.getNullColumnString( columnType );
	}

	@Override
	public int getVersion() {
		return version;
	}

	public int getMySQLVersion() {
		return version;
	}

	@Override
	public SizeStrategy getSizeStrategy() {
		return sizeStrategy;
	}

	@Override
	public long getDefaultLobLength() {
		//max length for mediumblob or mediumtext
		return 16_777_215;
	}

	@Override
	public JdbcTypeDescriptor resolveSqlTypeDescriptor(
			int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeDescriptorRegistry jdbcTypeDescriptorRegistry) {
		if ( jdbcTypeCode == Types.BIT ) {
			return jdbcTypeDescriptorRegistry.getDescriptor( Types.BOOLEAN );
		}
		return super.resolveSqlTypeDescriptor( jdbcTypeCode, precision, scale, jdbcTypeDescriptorRegistry );
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BIT;
	}

//	@Override
//	public int getDefaultDecimalPrecision() {
//		//this is the maximum, but I guess it's too high
//		return 65;
//	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		CommonFunctionFactory.soundex( queryEngine );
		CommonFunctionFactory.radians( queryEngine );
		CommonFunctionFactory.degrees( queryEngine );
		CommonFunctionFactory.cot( queryEngine );
		CommonFunctionFactory.log( queryEngine );
		CommonFunctionFactory.log2( queryEngine );
		CommonFunctionFactory.log10( queryEngine );
		CommonFunctionFactory.pi( queryEngine );
		CommonFunctionFactory.trim2( queryEngine );
		CommonFunctionFactory.octetLength( queryEngine );
		CommonFunctionFactory.reverse( queryEngine );
		CommonFunctionFactory.space( queryEngine );
		CommonFunctionFactory.repeat( queryEngine );
		CommonFunctionFactory.pad_space( queryEngine );
		CommonFunctionFactory.md5( queryEngine );
		CommonFunctionFactory.yearMonthDay( queryEngine );
		CommonFunctionFactory.hourMinuteSecond( queryEngine );
		CommonFunctionFactory.dayofweekmonthyear( queryEngine );
		CommonFunctionFactory.weekQuarter( queryEngine );
		CommonFunctionFactory.daynameMonthname( queryEngine );
		CommonFunctionFactory.lastDay( queryEngine );
		CommonFunctionFactory.date( queryEngine );
		CommonFunctionFactory.timestamp( queryEngine );
		time( queryEngine );

		CommonFunctionFactory.utcDateTimeTimestamp( queryEngine );
		CommonFunctionFactory.rand( queryEngine );
		CommonFunctionFactory.crc32( queryEngine );
		CommonFunctionFactory.sha1( queryEngine );
		CommonFunctionFactory.sha2( queryEngine );
		CommonFunctionFactory.sha( queryEngine );
		CommonFunctionFactory.bitLength( queryEngine );
		CommonFunctionFactory.octetLength( queryEngine );
		CommonFunctionFactory.ascii( queryEngine );
		CommonFunctionFactory.chr_char( queryEngine );
		CommonFunctionFactory.instr( queryEngine );
		CommonFunctionFactory.substr( queryEngine );
		//also natively supports ANSI-style substring()
		CommonFunctionFactory.position( queryEngine );
		CommonFunctionFactory.nowCurdateCurtime( queryEngine );
		CommonFunctionFactory.truncate( queryEngine );
		CommonFunctionFactory.insert( queryEngine );
		CommonFunctionFactory.bitandorxornot_operator( queryEngine );
		CommonFunctionFactory.bitAndOr( queryEngine );
		CommonFunctionFactory.stddev( queryEngine );
		CommonFunctionFactory.stddevPopSamp( queryEngine );
		CommonFunctionFactory.variance( queryEngine );
		CommonFunctionFactory.varPopSamp( queryEngine );
		CommonFunctionFactory.datediff( queryEngine );
		CommonFunctionFactory.adddateSubdateAddtimeSubtime( queryEngine );
		CommonFunctionFactory.format_dateFormat( queryEngine );
		CommonFunctionFactory.makedateMaketime( queryEngine );

		if ( getMySQLVersion() < 570 ) {
			CommonFunctionFactory.sysdateParens( queryEngine );
		}
		else {
			// MySQL timestamp type defaults to precision 0 (seconds) but
			// we want the standard default precision of 6 (microseconds)
			CommonFunctionFactory.sysdateExplicitMicros( queryEngine );
		}
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new MySQLSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	private void time(QueryEngine queryEngine) {
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "time" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardBasicTypes.STRING )
				.register();
	}

	@Override
	public int getFloatPrecision() {
		//according to MySQL docs, this is
		//the maximum precision for 4 bytes
		return 23;
	}

	/**
	 * MySQL 5.7 precision defaults to seconds, but microseconds is better
	 */
	@Override
	public String currentTimestamp() {
		return getMySQLVersion() < 570 ? super.currentTimestamp() : "current_timestamp(6)";
	}

	/**
	 * {@code microsecond} is the smallest unit for
	 * {@code timestampadd()} and {@code timestampdiff()},
	 * and the highest precision for a {@code timestamp}.
	 */
	@Override
	public long getFractionalSecondPrecisionInNanos() {
		return 1_000; //microseconds
	}

	/**
	 * MySQL supports a limited list of temporal fields in the
	 * extract() function, but we can emulate some of them by
	 * using the appropriate named functions instead of
	 * extract().
	 *
	 * Thus, the additional supported fields are
	 * {@link TemporalUnit#DAY_OF_YEAR},
	 * {@link TemporalUnit#DAY_OF_MONTH},
	 * {@link TemporalUnit#DAY_OF_YEAR}.
	 *
	 * In addition, the field {@link TemporalUnit#SECOND} is
	 * redefined to include microseconds.
	 */
	@Override
	public String extractPattern(TemporalUnit unit) {
		switch (unit) {
			case SECOND:
				return "(second(?2)+microsecond(?2)/1e6)";
			case WEEK:
				return "weekofyear(?2)"; //same as week(?2,3), the ISO week
			case DAY_OF_WEEK:
				return "dayofweek(?2)";
			case DAY_OF_MONTH:
				return "dayofmonth(?2)";
			case DAY_OF_YEAR:
				return "dayofyear(?2)";
			//TODO: case WEEK_YEAR: yearweek(?2, 3)/100
			default:
				return "?1(?2)";
		}
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType) {
		switch (unit) {
			case NANOSECOND:
				return "timestampadd(microsecond, (?2)/1e3, ?3)";
			case NATIVE:
				return "timestampadd(microsecond, ?2, ?3)";
			default:
				return "timestampadd(?1, ?2, ?3)";
		}
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		switch (unit) {
			case NANOSECOND:
				return "timestampdiff(microsecond, ?2, ?3)*1e3";
			case NATIVE:
				return "timestampdiff(microsecond, ?2, ?3)";
			default:
				return "timestampdiff(?1, ?2, ?3)";
		}
	}

	/**
	 * @see <a href="https://dev.mysql.com/worklog/task/?id=7019">MySQL 5.7 work log</a>
	 * @return true for MySQL 5.7 and above
	 */
	@Override
	public boolean supportsRowValueConstructorSyntaxInInList() {
		return getMySQLVersion() >= 570;
	}

	@Override
	public boolean supportsUnionAll() {
		return getMySQLVersion() >= 500;
	}

	@Override
	public boolean supportsSelectAliasInGroupByClause() {
		return true;
	}

	@Override
	public boolean supportsColumnCheck() {
		return false;
	}

	@Override
	public String getQueryHintString(String query, String hints) {
		return getMySQLVersion() < 500
				? super.getQueryHintString( query, hints )
				: IndexQueryHintHandler.INSTANCE.addQueryHints( query, hints );
	}

	/**
	 * No support for sequences.
	 */
	@Override
	public SequenceSupport getSequenceSupport() {
		return NoSequenceSupport.INSTANCE;
	}

	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return getMySQLVersion() < 500 ? super.getViolatedConstraintNameExtractor() : EXTRACTOR;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				switch ( Integer.parseInt( JdbcExceptionHelper.extractSqlState( sqle ) ) ) {
					case 23000:
						return extractUsingTemplate( " for key '", "'", sqle.getMessage() );
					default:
						return null;
				}
			} );

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		final String cols = String.join( ", ", foreignKey );
		final String referencedCols = String.join( ", ", primaryKey );
		return String.format(
				" add constraint %s foreign key (%s) references %s (%s)",
				constraintName,
				cols,
				referencedTable,
				referencedCols
		);
	}

	@Override
	public String getDropForeignKeyString() {
		return " drop foreign key ";
	}

	@Override
	public LimitHandler getLimitHandler() {
		//also supports LIMIT n OFFSET m
		return LimitLimitHandler.INSTANCE;
	}

	@Override
	public String getFromDual() {
		return "from dual";
	}

	@Override
	public char closeQuote() {
		return '`';
	}

	@Override
	public char openQuote() {
		return '`';
	}

	@Override
	public boolean canCreateCatalog() {
		return true;
	}

	@Override
	public String[] getCreateCatalogCommand(String catalogName) {
		return new String[] { "create database " + catalogName };
	}

	@Override
	public String[] getDropCatalogCommand(String catalogName) {
		return new String[] { "drop database " + catalogName };
	}

	@Override
	public boolean canCreateSchema() {
		return false;
	}

	@Override
	public String[] getCreateSchemaCommand(String schemaName) {
		throw new UnsupportedOperationException( "MySQL does not support dropping creating/dropping schemas in the JDBC sense" );
	}

	@Override
	public String[] getDropSchemaCommand(String schemaName) {
		throw new UnsupportedOperationException( "MySQL does not support dropping creating/dropping schemas in the JDBC sense" );
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}

	@Override
	public String getSelectGUIDString() {
		return "select uuid()";
	}

	@Override
	public String getTableComment(String comment) {
		return " comment='" + comment + "'";
	}

	@Override
	public String getColumnComment(String comment) {
		return " comment '" + comment + "'";
	}

	@Override
	public NullOrdering getNullOrdering() {
		return NullOrdering.SMALLEST;
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {

		return new LocalTemporaryTableStrategy(
				new IdTable( rootEntityDescriptor, basename -> "HT_" + basename, this ),
				() -> new TempIdTableExporter( true, this::getTypeName ) {
					@Override
					protected String getCreateCommand() {
						return "create temporary table if not exists";
					}

					@Override
					protected String getDropCommand() {
						return "drop temporary table";
					}
				},
				AfterUseAction.DROP,
				TempTableDdlTransactionHandling.NONE,
				runtimeModelCreationContext.getSessionFactory()
		);
	}

	@Override
	public String getCastTypeName(SqlExpressable type, Long length, Integer precision, Integer scale) {
		switch ( type.getJdbcMapping().getJdbcTypeDescriptor().getJdbcTypeCode() ) {
			case Types.INTEGER:
			case Types.BIGINT:
			case Types.SMALLINT:
			case Types.TINYINT:
				//MySQL doesn't let you cast to INTEGER/BIGINT/TINYINT
				return "signed";
			case Types.BIT:
				//special case for casting to Boolean
				return "unsigned";
			case Types.FLOAT:
			case Types.DOUBLE:
			case Types.REAL:
				//MySQL doesn't let you cast to DOUBLE/FLOAT
				//but don't just return 'decimal' because
				//the default scale is 0 (no decimal places)
				return String.format(
						"decimal(%d, %d)",
						precision == null ? type.getJdbcMapping().getJavaTypeDescriptor().getDefaultSqlPrecision(this) : precision,
						scale == null ? type.getJdbcMapping().getJavaTypeDescriptor().getDefaultSqlScale() : scale
				);
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
				//MySQL doesn't let you cast to BLOB/TINYBLOB/LONGBLOB
				//we could just return 'binary' here but that would be
				//inconsistent with other Dialects which need a length
				return String.format(
						"binary(%d)",
						length == null ? type.getJdbcMapping().getJavaTypeDescriptor().getDefaultSqlLength(this) : length
				);
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
				//MySQL doesn't let you cast to TEXT/LONGTEXT
				//we could just return 'char' here but that would be
				//inconsistent with other Dialects which need a length
				return String.format(
						"char(%d)",
						length == null ? type.getJdbcMapping().getJavaTypeDescriptor().getDefaultSqlLength(this) : length
				);
			default:
				return super.getCastTypeName( type, length, precision, scale );
		}
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select now()";
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		boolean isResultSet = ps.execute();
		while ( !isResultSet && ps.getUpdateCount() != -1 ) {
			isResultSet = ps.getMoreResults();
		}
		return ps.getResultSet();
	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	public boolean supportsRowValueConstructorSyntax() {
		return true;
	}

	@Override
	public boolean supportsNullPrecedence() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInSet() {
		return false;
	}

	@Override
	public String renderOrderByElement(String expression, String collation, String order, NullPrecedence nulls) {
		final StringBuilder orderByElement = new StringBuilder();
		if ( nulls != NullPrecedence.NONE ) {
			// Workaround for NULLS FIRST / LAST support.
			orderByElement.append( "case when " ).append( expression ).append( " is null then " );
			if ( nulls == NullPrecedence.FIRST ) {
				orderByElement.append( "0 else 1" );
			}
			else {
				orderByElement.append( "1 else 0" );
			}
			orderByElement.append( " end, " );
		}
		// Nulls precedence has already been handled so passing NONE value.
		orderByElement.append( super.renderOrderByElement( expression, collation, order, NullPrecedence.NONE ) );
		return orderByElement.toString();
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public boolean areStringComparisonsCaseInsensitive() {
		return true;
	}

	@Override
	public boolean supportsLobValueChangePropagation() {
		// note: at least my local MySQL 5.1 install shows this not working...
		return false;
	}

	@Override
	public boolean supportsSubqueryOnMutatingTable() {
		return false;
	}

	@Override
	public boolean supportsLockTimeouts() {
		// yes, we do handle "lock timeout" conditions in the exception conversion delegate,
		// but that's a hardcoded lock timeout period across the whole entire MySQL database.
		// MySQL does not support specifying lock timeouts as part of the SQL statement, which is really
		// what this meta method is asking.
		return false;
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			switch ( sqlException.getErrorCode() ) {
				case 1205:
				case 3572:
					return new PessimisticLockException( message, sqlException, sql );
				case 1207:
				case 1206:
					return new LockAcquisitionException( message, sqlException, sql );
			}

			final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );
			if ( sqlState != null ) {
				switch ( sqlState ) {
					case "41000":
						return new LockTimeoutException( message, sqlException, sql );
					case "40001":
						return new LockAcquisitionException( message, sqlException, sql );
				}
			}

			return null;
		};
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData)
			throws SQLException {

		if ( dbMetaData == null ) {
			builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.MIXED );
			builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		}

		return super.buildIdentifierHelper( builder, dbMetaData );
	}

	@Override
	public String getNotExpression(String expression) {
		return "not (" + expression + ")";
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new MySQLIdentityColumnSupport();
	}

	@Override
	public boolean isJdbcLogWarningsEnabledByDefault() {
		return false;
	}

	@Override
	public boolean supportsCascadeDelete() {
		return storageEngine.supportsCascadeDelete();
	}

	@Override
	public String getTableTypeString() {
		String engineKeyword = getMySQLVersion() < 500 ? "type" : "engine";
		return storageEngine.getTableTypeString( engineKeyword );
	}

	@Override
	public boolean hasSelfReferentialForeignKeyBug() {
		return storageEngine.hasSelfReferentialForeignKeyBug();
	}

	@Override
	public boolean dropConstraints() {
		return storageEngine.dropConstraints();
	}

	protected MySQLStorageEngine getDefaultMySQLStorageEngine() {
		return getMySQLVersion() < 550 ? MyISAMStorageEngine.INSTANCE : InnoDBStorageEngine.INSTANCE;
	}

	@Override
	protected String escapeLiteral(String literal) {
		return super.escapeLiteral( literal ).replace("\\", "\\\\");
	}

	@Override
	public String translateDatetimeFormat(String format) {
		return datetimeFormat( format ).result();
	}

	public static Replacer datetimeFormat(String format) {
		return new Replacer( format, "'", "" )
				.replace("%", "%%")

				//year
				.replace("yyyy", "%Y")
				.replace("yyy", "%Y")
				.replace("yy", "%y")
				.replace("y", "%Y")

				//month of year
				.replace("MMMM", "%M")
				.replace("MMM", "%b")
				.replace("MM", "%m")
				.replace("M", "%c")

				//week of year
				.replace("ww", "%v")
				.replace("w", "%v")
				//year for week
				.replace("YYYY", "%x")
				.replace("YYY", "%x")
				.replace("YY", "%x")
				.replace("Y", "%x")

				//week of month
				//????

				//day of week
				.replace("EEEE", "%W")
				.replace("EEE", "%a")
				.replace("ee", "%w")
				.replace("e", "%w")

				//day of month
				.replace("dd", "%d")
				.replace("d", "%e")

				//day of year
				.replace("DDD", "%j")
				.replace("DD", "%j")
				.replace("D", "%j")

				//am pm
				.replace("aa", "%p")
				.replace("a", "%p")

				//hour
				.replace("hh", "%I")
				.replace("HH", "%H")
				.replace("h", "%l")
				.replace("H", "%k")

				//minute
				.replace("mm", "%i")
				.replace("m", "%i")

				//second
				.replace("ss", "%S")
				.replace("s", "%S")

				//fractional seconds
				.replace("SSSSSS", "%f")
				.replace("SSSSS", "%f")
				.replace("SSSS", "%f")
				.replace("SSS", "%f")
				.replace("SS", "%f")
				.replace("S", "%f");
	}

	private String withTimeout(String lockString, int timeout) {
		switch (timeout) {
			case LockOptions.NO_WAIT:
				return supportsNoWait() ? lockString + " nowait" : lockString;
			case LockOptions.SKIP_LOCKED:
				return supportsSkipLocked() ? lockString + " skip locked" : lockString;
			case LockOptions.WAIT_FOREVER:
				return lockString;
			default:
				return supportsWait() ? lockString + " wait " + timeout : lockString;
		}
	}

	@Override
	public String getWriteLockString(int timeout) {
		return withTimeout( getForUpdateString(), timeout );
	}

	@Override
	public String getWriteLockString(String aliases, int timeout) {
		return withTimeout( getForUpdateString(aliases), timeout );
	}

	@Override
	public String getReadLockString(int timeout) {
		return withTimeout( supportsForShare() ? " for share" : " lock in share mode", timeout );
	}

	@Override
	public String getReadLockString(String aliases, int timeout) {
		if ( supportsAliasLocks() && supportsForShare() ) {
			return withTimeout(" for share of " + aliases, timeout );
		}
		else {
			// fall back to locking all aliases
			return getReadLockString( timeout );
		}
	}

	@Override
	public String getForUpdateSkipLockedString() {
		return supportsSkipLocked()
				? " for update skip locked"
				: getForUpdateString();
	}

	@Override
	public String getForUpdateSkipLockedString(String aliases) {
		return supportsSkipLocked() && supportsAliasLocks()
				? getForUpdateString( aliases ) + " skip locked"
				// fall back to skip locking all aliases
				: getForUpdateSkipLockedString();
	}

	@Override
	public String getForUpdateNowaitString() {
		return supportsNoWait()
				? " for update nowait"
				: getForUpdateString();
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		return supportsNoWait() && supportsAliasLocks()
				? getForUpdateString( aliases ) + " nowait"
				// fall back to nowait locking all aliases
				: getForUpdateNowaitString();
	}

	@Override
	public String getForUpdateString(String aliases) {
		return supportsAliasLocks()
				? " for update of " + aliases
				// fall back to locking all aliases
				: getForUpdateString();
	}

	@Override
	public boolean supportsOffsetInSubquery() {
		return true;
	}

	@Override
	public boolean supportsWindowFunctions() {
		return getMySQLVersion() >= 802;
	}

	@Override
	public boolean supportsSkipLocked() {
		return getMySQLVersion() >= 800;
	}

	@Override
	public boolean supportsNoWait() {
		return getMySQLVersion() >= 800;
	}

	@Override
	public boolean supportsWait() {
		//only supported on MariaDB
		return false;
	}

	@Override
	public RowLockStrategy getWriteRowLockStrategy() {
		return supportsAliasLocks() ? RowLockStrategy.TABLE : RowLockStrategy.NONE;
	}

	boolean supportsForShare() {
		return getMySQLVersion() >= 800;
	}

	boolean supportsAliasLocks() {
		return getMySQLVersion() >= 800;
	}

	@Override
	public GroupBySummarizationRenderingStrategy getGroupBySummarizationRenderingStrategy() {
		return GroupBySummarizationRenderingStrategy.CLAUSE;
	}
}
