/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * Centralizes access to the standard set of basic {@link Type types}.
 * <p/>
 * Type mappings can be adjusted per {@link org.hibernate.SessionFactory}.  These adjusted mappings can be accessed
 * from the {@link org.hibernate.TypeHelper} instance obtained via {@link org.hibernate.SessionFactory#getTypeHelper()}
 *
 * @see BasicTypeRegistry
 * @see org.hibernate.TypeHelper
 * @see org.hibernate.SessionFactory#getTypeHelper()
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
@SuppressWarnings( {"UnusedDeclaration"})
public final class StandardBasicTypes {
	private StandardBasicTypes() {
	}

	private static final Set<SqlTypeDescriptor> SQL_TYPE_DESCRIPTORS = new HashSet<SqlTypeDescriptor>();

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#BIT BIT}.
	 *
	 * @see BooleanTypeImpl
	 */
	public static final BooleanTypeImpl BOOLEAN = BooleanTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#INTEGER INTEGER}.
	 *
	 * @see NumericBooleanTypeImpl
	 */
	public static final NumericBooleanTypeImpl NUMERIC_BOOLEAN = NumericBooleanTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#CHAR CHAR(1)} (using 'T'/'F').
	 *
	 * @see TrueFalseTypeImpl
	 */
	public static final TrueFalseTypeImpl TRUE_FALSE = TrueFalseTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Boolean} to JDBC {@link java.sql.Types#CHAR CHAR(1)} (using 'Y'/'N').
	 *
	 * @see YesNoTypeImpl
	 */
	public static final YesNoTypeImpl YES_NO = YesNoTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Byte} to JDBC {@link java.sql.Types#TINYINT TINYINT}.
	 */
	public static final ByteTypeImpl BYTE = ByteTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Short} to JDBC {@link java.sql.Types#SMALLINT SMALLINT}.
	 *
	 * @see ShortTypeImpl
	 */
	public static final ShortTypeImpl SHORT = ShortTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Integer} to JDBC {@link java.sql.Types#INTEGER INTEGER}.
	 *
	 * @see IntegerTypeImpl
	 */
	public static final IntegerTypeImpl INTEGER = IntegerTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Long} to JDBC {@link java.sql.Types#BIGINT BIGINT}.
	 *
	 * @see LongTypeImpl
	 */
	public static final LongTypeImpl LONG = LongTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Float} to JDBC {@link java.sql.Types#FLOAT FLOAT}.
	 *
	 * @see FloatTypeImpl
	 */
	public static final FloatTypeImpl FLOAT = FloatTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Double} to JDBC {@link java.sql.Types#DOUBLE DOUBLE}.
	 *
	 * @see DoubleTypeImpl
	 */
	public static final DoubleTypeImpl DOUBLE = DoubleTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.math.BigInteger} to JDBC {@link java.sql.Types#NUMERIC NUMERIC}.
	 *
	 * @see BigIntegerTypeImpl
	 */
	public static final BigIntegerTypeImpl BIG_INTEGER = BigIntegerTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.math.BigDecimal} to JDBC {@link java.sql.Types#NUMERIC NUMERIC}.
	 *
	 * @see BigDecimalTypeImpl
	 */
	public static final BigDecimalTypeImpl BIG_DECIMAL = BigDecimalTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Character} to JDBC {@link java.sql.Types#CHAR CHAR(1)}.
	 *
	 * @see CharacterTypeImpl
	 */
	public static final CharacterTypeImpl CHARACTER = CharacterTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see StringTypeImpl
	 */
	public static final StringTypeImpl STRING = StringTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#NVARCHAR NVARCHAR}
	 */
	public static final StringNVarcharTypeImpl NSTRING = StringNVarcharTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.net.URL} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see UrlTypeImpl
	 */
	public static final UrlTypeImpl URL = UrlTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.util.Date} ({@link java.sql.Time}) to JDBC
	 * {@link java.sql.Types#TIME TIME}.
	 *
	 * @see TimeTypeImpl
	 */
	public static final TimeTypeImpl TIME = TimeTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.util.Date} ({@link java.sql.Date}) to JDBC
	 * {@link java.sql.Types#DATE DATE}.
	 *
	 * @see TimeTypeImpl
	 */
	public static final DateTypeImpl DATE = DateTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.util.Date} ({@link java.sql.Timestamp}) to JDBC
	 * {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 *
	 * @see TimeTypeImpl
	 */
	public static final TimestampTypeImpl TIMESTAMP = TimestampTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.util.Calendar} to JDBC
	 * {@link java.sql.Types#TIMESTAMP TIMESTAMP}.
	 *
	 * @see CalendarTypeImpl
	 */
	public static final CalendarTypeImpl CALENDAR = CalendarTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.util.Calendar} to JDBC
	 * {@link java.sql.Types#DATE DATE}.
	 *
	 * @see CalendarDateTypeImpl
	 */
	public static final CalendarDateTypeImpl CALENDAR_DATE = CalendarDateTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Class} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see ClassTypeImpl
	 */
	public static final ClassTypeImpl CLASS = ClassTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.util.Locale} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see LocaleTypeImpl
	 */
	public static final LocaleTypeImpl LOCALE = LocaleTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.util.Currency} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see CurrencyTypeImpl
	 */
	public static final CurrencyTypeImpl CURRENCY = CurrencyTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.util.TimeZone} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see TimeZoneTypeImpl
	 */
	public static final TimeZoneTypeImpl TIMEZONE = TimeZoneTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.util.UUID} to JDBC {@link java.sql.Types#BINARY BINARY}.
	 *
	 * @see UUIDBinaryTypeImpl
	 */
	public static final UUIDBinaryTypeImpl UUID_BINARY = UUIDBinaryTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.util.UUID} to JDBC {@link java.sql.Types#CHAR CHAR}.
	 *
	 * @see UUIDCharTypeImpl
	 */
	public static final UUIDCharTypeImpl UUID_CHAR = UUIDCharTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#VARBINARY VARBINARY}.
	 *
	 * @see BinaryTypeImpl
	 */
	public static final BinaryTypeImpl BINARY = BinaryTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Byte Byte[]} to JDBC {@link java.sql.Types#VARBINARY VARBINARY}.
	 *
	 * @see WrapperBinaryTypeImpl
	 */
	public static final WrapperBinaryTypeImpl WRAPPER_BINARY = WrapperBinaryTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#VARBINARY VARBINARY},
	 * specifically for entity versions/timestamps.
	 *
	 * @see RowVersionTypeImpl
	 */
	public static final RowVersionTypeImpl ROW_VERSION = RowVersionTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#LONGVARBINARY LONGVARBINARY}.
	 *
	 * @see ImageTypeImpl
	 * @see #MATERIALIZED_BLOB
	 */
	public static final ImageTypeImpl IMAGE = ImageTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.sql.Blob} to JDBC {@link java.sql.Types#BLOB BLOB}.
	 *
	 * @see BlobTypeImpl
	 * @see #MATERIALIZED_BLOB
	 */
	public static final BlobTypeImpl BLOB = BlobTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@code byte[]} to JDBC {@link java.sql.Types#BLOB BLOB}.
	 *
	 * @see MaterializedBlobTypeImpl
	 * @see #MATERIALIZED_BLOB
	 * @see #IMAGE
	 */
	public static final MaterializedBlobTypeImpl MATERIALIZED_BLOB = MaterializedBlobTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@code char[]} to JDBC {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see CharArrayTypeImpl
	 */
	public static final CharArrayTypeImpl CHAR_ARRAY = CharArrayTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link Character Character[]} to JDBC
	 * {@link java.sql.Types#VARCHAR VARCHAR}.
	 *
	 * @see CharacterArrayTypeImpl
	 */
	public static final CharacterArrayTypeImpl CHARACTER_ARRAY = CharacterArrayTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#LONGVARCHAR LONGVARCHAR}.
	 * <p/>
	 * Similar to a {@link #MATERIALIZED_CLOB}
	 *
	 * @see TextTypeImpl
	 */
	public static final TextTypeImpl TEXT = TextTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#LONGNVARCHAR LONGNVARCHAR}.
	 * <p/>
	 * Similar to a {@link #MATERIALIZED_NCLOB}
	 *
	 * @see NTextTypeImpl
	 */
	public static final NTextTypeImpl NTEXT = NTextTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.sql.Clob} to JDBC {@link java.sql.Types#CLOB CLOB}.
	 *
	 * @see ClobTypeImpl
	 * @see #MATERIALIZED_CLOB
	 */
	public static final ClobTypeImpl CLOB = ClobTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.sql.NClob} to JDBC {@link java.sql.Types#NCLOB NCLOB}.
	 *
	 * @see NClobTypeImpl
	 * @see #MATERIALIZED_NCLOB
	 */
	public static final NClobTypeImpl NCLOB = NClobTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#CLOB CLOB}.
	 *
	 * @see MaterializedClobTypeImpl
	 * @see #MATERIALIZED_CLOB
	 * @see #TEXT
	 */
	public static final MaterializedClobTypeImpl MATERIALIZED_CLOB = MaterializedClobTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link String} to JDBC {@link java.sql.Types#NCLOB NCLOB}.
	 *
	 * @see MaterializedNClobTypeImpl
	 * @see #MATERIALIZED_CLOB
	 * @see #NTEXT
	 */
	public static final MaterializedNClobTypeImpl MATERIALIZED_NCLOB = MaterializedNClobTypeImpl.INSTANCE;

	/**
	 * The standard Hibernate type for mapping {@link java.io.Serializable} to JDBC {@link java.sql.Types#VARBINARY VARBINARY}.
	 * <p/>
	 * See especially the discussion wrt {@link ClassLoader} determination on {@link SerializableTypeImpl}
	 *
	 * @see SerializableTypeImpl
	 */
	public static final SerializableTypeImpl SERIALIZABLE = SerializableTypeImpl.INSTANCE;
}
