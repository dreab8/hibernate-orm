/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import java.io.Serializable;
import java.sql.Types;

import org.hibernate.Incubating;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.query.sqm.CastType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for the SQL/JDBC side of a value mapping.
 * <p>
 * An instance of this type need not correspond directly to a SQL column type on
 * a particular database. Rather, a {@code JdbcType} defines how values are read
 * from and written to JDBC. Therefore, implementations of this interface map more
 * directly to the JDBC type codes defined by {@link Types} and {@link SqlTypes}.
 * <p>
 * A JDBC type may be selected when mapping an entity attribute using the
 * {@link org.hibernate.annotations.JdbcType} annotation, or, indirectly, using
 * the {@link org.hibernate.annotations.JdbcTypeCode} annotation.
 * <p>
 * Custom implementations should be registered with the
 * {@link org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry} at startup.
 * The built-in implementations are registered automatically.
 *
 * @author Steve Ebersole
 */
public interface JdbcType extends Serializable {
	/**
	 * A "friendly" name for use in logging
	 */
	default String getFriendlyName() {
		return Integer.toString( getDefaultSqlTypeCode() );
	}

	/**
	 * The {@linkplain SqlTypes JDBC type code} used when interacting with JDBC APIs.
	 * <p>
	 * For example, it's used when calling {@link java.sql.PreparedStatement#setNull(int, int)}.
	 *
	 * @return a JDBC type code
	 */
	int getJdbcTypeCode();

	/**
	 * A {@linkplain SqlTypes JDBC type code} that identifies the SQL column type to
	 * be used for schema generation.
	 * <p>
	 * This value is passed to {@link DdlTypeRegistry#getTypeName(int, Size)}
	 * to obtain the SQL column type.
	 *
	 * @return a JDBC type code
	 */
	default int getDefaultSqlTypeCode() {
		return getJdbcTypeCode();
	}

	default <T> BasicJavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer precision,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		// match legacy behavior
		return (BasicJavaType<T>) typeConfiguration.getJavaTypeRegistry().getDescriptor(
				JdbcTypeJavaClassMappings.INSTANCE.determineJavaClassForJdbcTypeCode( getDefaultSqlTypeCode() )
		);
	}

	/**
	 * Obtain a {@linkplain JdbcLiteralFormatter formatter} object capable of rendering
	 * values of the given {@linkplain JavaType Java type} as SQL literals of the type
	 * represented by this object.
	 */
	// todo (6.0) : move to {@link org.hibernate.metamodel.mapping.JdbcMapping}?
	default <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		return (appender, value, dialect, wrapperOptions) -> appender.appendSql( value.toString() );
	}

	/**
	 * Obtain a {@linkplain ValueBinder binder} object capable of binding values of the
	 * given {@linkplain JavaType Java type} to parameters of a JDBC
	 * {@link java.sql.PreparedStatement}.
	 *
	 * @param javaType The descriptor describing the types of Java values to be bound
	 *
	 * @return The appropriate binder.
	 */
	<X> ValueBinder<X> getBinder(JavaType<X> javaType);

	/**
	 * Obtain an {@linkplain ValueExtractor extractor} object capable of extracting
	 * values of the given {@linkplain JavaType Java type} from a JDBC
	 * {@link java.sql.ResultSet}.
	 *
	 * @param javaType The descriptor describing the types of Java values to be extracted
	 *
	 * @return The appropriate extractor
	 */
	<X> ValueExtractor<X> getExtractor(JavaType<X> javaType);

	/**
	 * The Java type class that is preferred by the binder or null.
	 */
	@Incubating
	default Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return null;
	}

	default boolean isInteger() {
		return isInteger( getJdbcTypeCode() );
	}

	static boolean isInteger(int typeCode) {
		switch ( typeCode ) {
			case Types.BIT:
			case Types.TINYINT:
			case Types.SMALLINT:
			case Types.INTEGER:
			case Types.BIGINT:
				return true;
		}
		return false;
	}

	default boolean isFloat() {
		return isFloat( getJdbcTypeCode() );
	}

	static boolean isFloat(int typeCode) {
		switch ( typeCode ) {
			case Types.FLOAT:
			case Types.REAL:
			case Types.DOUBLE:
				return true;
		}
		return false;
	}

	default boolean isDecimal() {
		return isDecimal( getJdbcTypeCode() );
	}

	static boolean isDecimal(int typeCode) {
		switch ( typeCode ) {
			case Types.DECIMAL:
			case Types.NUMERIC:
				return true;
		}
		return false;
	}

	default boolean isNumber() {
		return isNumber( getJdbcTypeCode() );
	}

	static boolean isNumber(int typeCode) {
		switch ( typeCode ) {
			case Types.BIT:
			case Types.TINYINT:
			case Types.SMALLINT:
			case Types.INTEGER:
			case Types.BIGINT:
			case Types.FLOAT:
			case Types.REAL:
			case Types.DOUBLE:
			case Types.DECIMAL:
			case Types.NUMERIC:
				return true;
		}
		return false;
	}

	default boolean isBinary() {
		return isBinary( getJdbcTypeCode() );
	}

	static boolean isBinary(int typeCode) {
		switch ( typeCode ) {
			case Types.BINARY:
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
			case Types.BLOB:
				return true;
		}
		return false;
	}

	default boolean isString() {
		return isString( getJdbcTypeCode() );
	}

	static boolean isString(int typeCode) {
		switch ( typeCode ) {
			case Types.CHAR:
			case Types.NCHAR:
			case Types.VARCHAR:
			case Types.NVARCHAR:
			case Types.LONGVARCHAR:
			case Types.LONGNVARCHAR:
			case Types.CLOB:
			case Types.NCLOB:
				return true;
		}
		return false;
	}

	default boolean isTemporal() {
		return isTemporal( getDefaultSqlTypeCode() );
	}

	static boolean isTemporal(int typeCode) {
		switch ( typeCode ) {
			case Types.DATE:
			case Types.TIME:
			case Types.TIMESTAMP:
			case Types.TIMESTAMP_WITH_TIMEZONE:
				return true;
		}
		return false;
	}

	default boolean isLob() {
		return isLob( getJdbcTypeCode() );
	}

	static boolean isLob(int jdbcTypeCode) {
		switch ( jdbcTypeCode ) {
			case SqlTypes.BLOB:
			case SqlTypes.CLOB:
			case SqlTypes.NCLOB: {
				return true;
			}
		}
		return false;
	}

	default boolean isInterval() {
		return isInterval( getDefaultSqlTypeCode() );
	}

	static boolean isInterval(int typeCode) {
		switch ( typeCode ) {
			case SqlTypes.INTERVAL_SECOND:
				return true;
		}
		return false;
	}

	default CastType getCastType() {
		return getCastType( getJdbcTypeCode() );
	}

	static CastType getCastType(int typeCode) {
		switch ( typeCode ) {
			case Types.INTEGER:
			case Types.TINYINT:
			case Types.SMALLINT:
				return CastType.INTEGER;
			case Types.BIGINT:
				return CastType.LONG;
			case Types.FLOAT:
			case Types.REAL:
				return CastType.FLOAT;
			case Types.DOUBLE:
				return CastType.DOUBLE;
			case Types.CHAR:
			case Types.NCHAR:
			case Types.VARCHAR:
			case Types.NVARCHAR:
			case Types.LONGVARCHAR:
			case Types.LONGNVARCHAR:
				return CastType.STRING;
			case Types.BOOLEAN:
				return CastType.BOOLEAN;
			case Types.DECIMAL:
			case Types.NUMERIC:
				return CastType.FIXED;
			case Types.DATE:
				return CastType.DATE;
			case Types.TIME:
				return CastType.TIME;
			case Types.TIMESTAMP:
				return CastType.TIMESTAMP;
			case Types.TIMESTAMP_WITH_TIMEZONE:
				return CastType.OFFSET_TIMESTAMP;
			case Types.NULL:
				return CastType.NULL;
			default:
				return CastType.OTHER;
		}
	}
}
