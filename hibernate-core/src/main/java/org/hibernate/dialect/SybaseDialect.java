/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.CastType;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.TrimSpec;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.sql.SqmTranslator;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.query.sqm.sql.StandardSqmTranslatorFactory;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.descriptor.jdbc.BlobTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.ClobTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.NClobTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeDescriptorRegistry;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import javax.persistence.TemporalType;


/**
 * Superclass for all Sybase dialects.
 *
 * @author Brett Meyer
 */
public class SybaseDialect extends AbstractTransactSQLDialect {

	private final int version;
	protected final boolean jtdsDriver;

	//All Sybase dialects share an IN list size limit.
	private static final int PARAM_LIST_SIZE_LIMIT = 250000;

	public SybaseDialect(){
		this( 1100, false );
	}

	public SybaseDialect(DialectResolutionInfo info){
		this(
				info.getDatabaseMajorVersion() * 100 + info.getDatabaseMinorVersion() * 10,
				info.getDriverName() != null && info.getDriverName().contains( "jTDS" )
		);
	}

	public SybaseDialect(int version, boolean jtdsDriver) {
		super();
		this.version = version;
		this.jtdsDriver = jtdsDriver;
		//Sybase ASE didn't introduce bigint until version 15.0
		registerColumnType( Types.BIGINT, "numeric(19,0)" );
	}

	@Override
	public JdbcTypeDescriptor resolveSqlTypeDescriptor(
			int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeDescriptorRegistry jdbcTypeDescriptorRegistry) {
		switch ( jdbcTypeCode ) {
			case Types.NUMERIC:
			case Types.DECIMAL:
				if ( precision == 19 && scale == 0 ) {
					return jdbcTypeDescriptorRegistry.getDescriptor( Types.BIGINT );
				}
		}
		return super.resolveSqlTypeDescriptor( jdbcTypeCode, precision, scale, jdbcTypeDescriptorRegistry );
	}

	@Override
	public SqmTranslatorFactory getSqmTranslatorFactory() {
		return new StandardSqmTranslatorFactory() {
			@Override
			public SqmTranslator<SelectStatement> createSelectTranslator(
					SqmSelectStatement<?> sqmSelectStatement,
					QueryOptions queryOptions,
					DomainParameterXref domainParameterXref,
					QueryParameterBindings domainParameterBindings,
					LoadQueryInfluencers loadQueryInfluencers,
					SqlAstCreationContext creationContext) {
				return new SybaseSqmToSqlAstConverter<>(
						sqmSelectStatement,
						queryOptions,
						domainParameterXref,
						domainParameterBindings,
						loadQueryInfluencers,
						creationContext
				);
			}
		};
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new SybaseSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public boolean supportsNullPrecedence() {
		return false;
	}

	@Override
	public int getInExpressionCountLimit() {
		return PARAM_LIST_SIZE_LIMIT;
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(typeContributions, serviceRegistry);

		if ( jtdsDriver ) {
			typeContributions.getTypeConfiguration().getJdbcTypeDescriptorRegistry().addDescriptor(
					Types.NCLOB,
					ClobTypeDescriptor.CLOB_BINDING
			);
			typeContributions.getTypeConfiguration().getJdbcTypeDescriptorRegistry().addDescriptor(
					Types.NVARCHAR,
					ClobTypeDescriptor.CLOB_BINDING
			);
			typeContributions.contributeJdbcTypeDescriptor( ClobTypeDescriptor.CLOB_BINDING );
		}
	}

	@Override
	public NationalizationSupport getNationalizationSupport() {
		// At least the jTDS driver doesn't support this
		return jtdsDriver ? NationalizationSupport.IMPLICIT : super.getNationalizationSupport();
	}

	@Override
	protected JdbcTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
		switch (sqlCode) {
		case Types.BLOB:
			return BlobTypeDescriptor.PRIMITIVE_ARRAY_BINDING;
		case Types.CLOB:
			// Some Sybase drivers cannot support getClob.  See HHH-7889
			// The jTDS driver doesn't support the JDBC4 signatures using 'long length' for stream bindings
			return jtdsDriver ? ClobTypeDescriptor.CLOB_BINDING : ClobTypeDescriptor.STREAM_BINDING_EXTRACTING;
		case Types.NCLOB:
			// The jTDS driver doesn't support the JDBC4 signatures using 'long length' for stream bindings
			if ( jtdsDriver ) {
				return NClobTypeDescriptor.NCLOB_BINDING;
			}
		case Types.NVARCHAR:
			// The jTDS driver doesn't support the JDBC4 setNString method
			if ( jtdsDriver ) {
				return NClobTypeDescriptor.NCLOB_BINDING;
			}
		default:
			return super.getSqlTypeDescriptorOverride( sqlCode );
		}
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry(queryEngine);

		//this doesn't work 100% on earlier versions of Sybase
		//which were missing the third parameter in charindex()
		//TODO: we could emulate it with substring() like in Postgres
		CommonFunctionFactory.locate_charindex( queryEngine );

		CommonFunctionFactory.replace_strReplace( queryEngine );
		CommonFunctionFactory.everyAny_sumCaseCase( queryEngine );
	}

	@Override
	public String getNullColumnString() {
		return " null";
	}

	@Override
	public boolean canCreateSchema() {
		// As far as I can tell, it does not
		return false;
	}

	@Override
	public String getCurrentSchemaCommand() {
		return "select db_name()";
	}

	@Override
	public String castPattern(CastType from, CastType to) {
		if ( to == CastType.STRING ) {
			switch ( from ) {
				case DATE:
					return "str_replace(convert(varchar, ?1, 102), '.', '-')";
				case TIME:
					return "convert(varchar, ?1, 108)";
				case TIMESTAMP:
					return "str_replace(convert(varchar, ?1, 23), 'T', ' ')";
			}
		}
		return super.castPattern( from, to );
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			case WEEK: return "calweekofyear"; //the ISO week number I think
			default: return super.translateExtractField(unit);
		}
	}

	@Override
	public String extractPattern(TemporalUnit unit) {
		//TODO!!
		return "datepart(?1, ?2)";
	}

	@Override
	public boolean supportsFractionalTimestampArithmetic() {
		return false;
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType) {
		//TODO!!
		return "dateadd(?1, ?2, ?3)";
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		//TODO!!
		return "datediff(?1, ?2, ?3)";
	}

	@Override
	public String trimPattern(TrimSpec specification, char character) {
		return super.trimPattern(specification, character)
				.replace("replace", "str_replace");
	}

	@Override
	public String translateDatetimeFormat(String format) {
		throw new NotYetImplementedFor6Exception( "format() function not supported on Sybase");
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

}
