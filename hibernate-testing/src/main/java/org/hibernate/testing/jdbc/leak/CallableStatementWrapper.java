/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.jdbc.leak;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 * @author Andrea Boriero
 */
public class CallableStatementWrapper extends PreparedStatementWrapper implements CallableStatement {
	CallableStatementWrapper(
			Statement statement,
			StatementInfo statementInfo,
			ResultSetInfo resultsetInfo) {
		super( statement, statementInfo, resultsetInfo );
	}

	private CallableStatement getStatement() {
		return (CallableStatement) statement;
	}

	@Override
	public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException {
		getStatement().registerOutParameter( parameterIndex, sqlType );
	}

	@Override
	public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws SQLException {
		getStatement().registerOutParameter( parameterIndex, sqlType, scale );
	}

	@Override
	public boolean wasNull() throws SQLException {
		return getStatement().wasNull();
	}

	@Override
	public String getString(int parameterIndex) throws SQLException {
		return getStatement().getString( parameterIndex );
	}

	@Override
	public boolean getBoolean(int parameterIndex) throws SQLException {
		return getStatement().getBoolean( parameterIndex );
	}

	@Override
	public byte getByte(int parameterIndex) throws SQLException {
		return getStatement().getByte( parameterIndex );
	}

	@Override
	public short getShort(int parameterIndex) throws SQLException {
		return getStatement().getShort( parameterIndex );
	}

	@Override
	public int getInt(int parameterIndex) throws SQLException {
		return getStatement().getInt( parameterIndex );
	}

	@Override
	public long getLong(int parameterIndex) throws SQLException {
		return getStatement().getLong( parameterIndex );
	}

	@Override
	public float getFloat(int parameterIndex) throws SQLException {
		return getStatement().getFloat( parameterIndex );
	}

	@Override
	public double getDouble(int parameterIndex) throws SQLException {
		return getStatement().getDouble( parameterIndex );
	}

	@Override
	public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException {
		return getStatement().getBigDecimal( parameterIndex, scale );
	}

	@Override
	public byte[] getBytes(int parameterIndex) throws SQLException {
		return getStatement().getBytes( parameterIndex );
	}

	@Override
	public Date getDate(int parameterIndex) throws SQLException {
		return getStatement().getDate( parameterIndex );
	}

	@Override
	public Time getTime(int parameterIndex) throws SQLException {
		return getStatement().getTime( parameterIndex );
	}

	@Override
	public Timestamp getTimestamp(int parameterIndex) throws SQLException {
		return getStatement().getTimestamp( parameterIndex );
	}

	@Override
	public Object getObject(int parameterIndex) throws SQLException {
		return getStatement().getObject( parameterIndex );
	}

	@Override
	public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
		return getStatement().getBigDecimal( parameterIndex );
	}

	@Override
	public Object getObject(int parameterIndex, Map<String, Class<?>> map) throws SQLException {
		return getStatement().getObject( parameterIndex, map );
	}

	@Override
	public Ref getRef(int parameterIndex) throws SQLException {
		return getStatement().getRef( parameterIndex );
	}

	@Override
	public Blob getBlob(int parameterIndex) throws SQLException {
		return getStatement().getBlob( parameterIndex );
	}

	@Override
	public Clob getClob(int parameterIndex) throws SQLException {
		return getStatement().getClob( parameterIndex );
	}

	@Override
	public Array getArray(int parameterIndex) throws SQLException {
		return getStatement().getArray( parameterIndex );
	}

	@Override
	public Date getDate(int parameterIndex, Calendar cal) throws SQLException {
		return getStatement().getDate( parameterIndex, cal );
	}

	@Override
	public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
		return getStatement().getTime( parameterIndex, cal );
	}

	@Override
	public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException {
		return getStatement().getTimestamp( parameterIndex, cal );
	}

	@Override
	public void registerOutParameter(int parameterIndex, int sqlType, String typeName) throws SQLException {
		getStatement().registerOutParameter( parameterIndex, sqlType, typeName );
	}

	@Override
	public void registerOutParameter(String parameterName, int sqlType) throws SQLException {
		getStatement().registerOutParameter( parameterName, sqlType );
	}

	@Override
	public void registerOutParameter(String parameterName, int sqlType, int scale) throws SQLException {
		getStatement().registerOutParameter( parameterName, sqlType, scale );
	}

	@Override
	public void registerOutParameter(String parameterName, int sqlType, String typeName) throws SQLException {
		getStatement().registerOutParameter( parameterName, sqlType, typeName );
	}

	@Override
	public URL getURL(int parameterIndex) throws SQLException {
		return getStatement().getURL( parameterIndex );
	}

	@Override
	public void setURL(String parameterName, URL val) throws SQLException {
		getStatement().setURL( parameterName, val );
	}

	@Override
	public void setNull(String parameterName, int sqlType) throws SQLException {
		getStatement().setNull( parameterName, sqlType );
	}

	@Override
	public void setBoolean(String parameterName, boolean x) throws SQLException {
		getStatement().setBoolean( parameterName, x );
	}

	@Override
	public void setByte(String parameterName, byte x) throws SQLException {
		getStatement().setByte( parameterName, x );
	}

	@Override
	public void setShort(String parameterName, short x) throws SQLException {
		getStatement().setShort( parameterName, x );
	}

	@Override
	public void setInt(String parameterName, int x) throws SQLException {
		getStatement().setInt( parameterName, x );
	}

	@Override
	public void setLong(String parameterName, long x) throws SQLException {
		getStatement().setLong( parameterName, x );
	}

	@Override
	public void setFloat(String parameterName, float x) throws SQLException {
		getStatement().setFloat( parameterName, x );
	}

	@Override
	public void setDouble(String parameterName, double x) throws SQLException {
		getStatement().setDouble( parameterName, x );
	}

	@Override
	public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException {
		getStatement().setBigDecimal( parameterName, x );
	}

	@Override
	public void setString(String parameterName, String x) throws SQLException {
		getStatement().setString( parameterName, x );
	}

	@Override
	public void setBytes(String parameterName, byte[] x) throws SQLException {
		getStatement().setBytes( parameterName, x );
	}

	@Override
	public void setDate(String parameterName, Date x) throws SQLException {
		getStatement().setDate( parameterName, x );
	}

	@Override
	public void setTime(String parameterName, Time x) throws SQLException {
		getStatement().setTime( parameterName, x );
	}

	@Override
	public void setTimestamp(String parameterName, Timestamp x) throws SQLException {
		getStatement().setTimestamp( parameterName, x );
	}

	@Override
	public void setAsciiStream(String parameterName, InputStream x, int length) throws SQLException {
		getStatement().setAsciiStream( parameterName, x, length );
	}

	@Override
	public void setBinaryStream(String parameterName, InputStream x, int length) throws SQLException {
		getStatement().setBinaryStream( parameterName, x, length );
	}

	@Override
	public void setObject(String parameterName, Object x, int targetSqlType, int scale) throws SQLException {
		getStatement().setObject( parameterName, x, targetSqlType, scale );
	}

	@Override
	public void setObject(String parameterName, Object x, int targetSqlType) throws SQLException {
		getStatement().setObject( parameterName, x, targetSqlType );
	}

	@Override
	public void setObject(String parameterName, Object x) throws SQLException {
		getStatement().setObject( parameterName, x );
	}

	@Override
	public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException {
		getStatement().setCharacterStream( parameterName, reader, length );
	}

	@Override
	public void setDate(String parameterName, Date x, Calendar cal) throws SQLException {
		getStatement().setDate( parameterName, x, cal );
	}

	@Override
	public void setTime(String parameterName, Time x, Calendar cal) throws SQLException {
		getStatement().setTime( parameterName, x, cal );
	}

	@Override
	public void setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException {
		getStatement().setTimestamp( parameterName, x, cal );
	}

	@Override
	public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {
		getStatement().setNull( parameterName, sqlType, typeName );
	}

	@Override
	public String getString(String parameterName) throws SQLException {
		return getStatement().getString( parameterName );
	}

	@Override
	public boolean getBoolean(String parameterName) throws SQLException {
		return getStatement().getBoolean( parameterName );
	}

	@Override
	public byte getByte(String parameterName) throws SQLException {
		return getStatement().getByte( parameterName );
	}

	@Override
	public short getShort(String parameterName) throws SQLException {
		return getStatement().getShort( parameterName );
	}

	@Override
	public int getInt(String parameterName) throws SQLException {
		return getStatement().getInt( parameterName );
	}

	@Override
	public long getLong(String parameterName) throws SQLException {
		return getStatement().getLong( parameterName );
	}

	@Override
	public float getFloat(String parameterName) throws SQLException {
		return getStatement().getFloat( parameterName );
	}

	@Override
	public double getDouble(String parameterName) throws SQLException {
		return getStatement().getDouble( parameterName );
	}

	@Override
	public byte[] getBytes(String parameterName) throws SQLException {
		return getStatement().getBytes( parameterName );
	}

	@Override
	public Date getDate(String parameterName) throws SQLException {
		return getStatement().getDate( parameterName );
	}

	@Override
	public Time getTime(String parameterName) throws SQLException {
		return getStatement().getTime( parameterName );
	}

	@Override
	public Timestamp getTimestamp(String parameterName) throws SQLException {
		return getStatement().getTimestamp( parameterName );
	}

	@Override
	public Object getObject(String parameterName) throws SQLException {
		return getStatement().getObject( parameterName );
	}

	@Override
	public BigDecimal getBigDecimal(String parameterName) throws SQLException {
		return getStatement().getBigDecimal( parameterName );
	}

	@Override
	public Object getObject(String parameterName, Map<String, Class<?>> map) throws SQLException {
		return getStatement().getObject( parameterName, map );
	}

	@Override
	public Ref getRef(String parameterName) throws SQLException {
		return getStatement().getRef( parameterName );
	}

	@Override
	public Blob getBlob(String parameterName) throws SQLException {
		return getStatement().getBlob( parameterName );
	}

	@Override
	public Clob getClob(String parameterName) throws SQLException {
		return getStatement().getClob( parameterName );
	}

	@Override
	public Array getArray(String parameterName) throws SQLException {
		return getStatement().getArray( parameterName );
	}

	@Override
	public Date getDate(String parameterName, Calendar cal) throws SQLException {
		return getStatement().getDate( parameterName, cal );
	}

	@Override
	public Time getTime(String parameterName, Calendar cal) throws SQLException {
		return getStatement().getTime( parameterName, cal );
	}

	@Override
	public Timestamp getTimestamp(String parameterName, Calendar cal) throws SQLException {
		return getStatement().getTimestamp( parameterName, cal );
	}

	@Override
	public URL getURL(String parameterName) throws SQLException {
		return getStatement().getURL( parameterName );
	}

	@Override
	public RowId getRowId(int parameterIndex) throws SQLException {
		return getStatement().getRowId( parameterIndex );
	}

	@Override
	public RowId getRowId(String parameterName) throws SQLException {
		return getStatement().getRowId( parameterName );
	}

	@Override
	public void setRowId(String parameterName, RowId x) throws SQLException {
		getStatement().setRowId( parameterName, x );
	}

	@Override
	public void setNString(String parameterName, String value) throws SQLException {
		getStatement().setNString( parameterName, value );
	}

	@Override
	public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {
		getStatement().setNCharacterStream( parameterName, value, length );
	}

	@Override
	public void setNClob(String parameterName, NClob value) throws SQLException {
		getStatement().setNClob( parameterName, value );
	}

	@Override
	public void setClob(String parameterName, Reader reader, long length) throws SQLException {
		getStatement().setClob( parameterName, reader, length );
	}

	@Override
	public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {
		getStatement().setBlob( parameterName, inputStream, length );
	}

	@Override
	public void setNClob(String parameterName, Reader reader, long length) throws SQLException {
		getStatement().setNClob( parameterName, reader, length );
	}

	@Override
	public NClob getNClob(int parameterIndex) throws SQLException {
		return getStatement().getNClob( parameterIndex );
	}

	@Override
	public NClob getNClob(String parameterName) throws SQLException {
		return getStatement().getNClob( parameterName );
	}

	@Override
	public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
		getStatement().setSQLXML( parameterName, xmlObject );
	}

	@Override
	public SQLXML getSQLXML(int parameterIndex) throws SQLException {
		return getStatement().getSQLXML( parameterIndex );
	}

	@Override
	public SQLXML getSQLXML(String parameterName) throws SQLException {
		return getStatement().getSQLXML( parameterName );
	}

	@Override
	public String getNString(int parameterIndex) throws SQLException {
		return getStatement().getNString( parameterIndex );
	}

	@Override
	public String getNString(String parameterName) throws SQLException {
		return getStatement().getNString( parameterName );
	}

	@Override
	public Reader getNCharacterStream(int parameterIndex) throws SQLException {
		return getStatement().getNCharacterStream( parameterIndex );
	}

	@Override
	public Reader getNCharacterStream(String parameterName) throws SQLException {
		return getStatement().getNCharacterStream( parameterName );
	}

	@Override
	public Reader getCharacterStream(int parameterIndex) throws SQLException {
		return getStatement().getCharacterStream( parameterIndex );
	}

	@Override
	public Reader getCharacterStream(String parameterName) throws SQLException {
		return getStatement().getCharacterStream( parameterName );
	}

	@Override
	public void setBlob(String parameterName, Blob x) throws SQLException {
		getStatement().setBlob( parameterName, x );
	}

	@Override
	public void setClob(String parameterName, Clob x) throws SQLException {
		getStatement().setClob( parameterName, x );
	}

	@Override
	public void setAsciiStream(String parameterName, InputStream x, long length) throws SQLException {
		getStatement().setAsciiStream( parameterName, x, length );
	}

	@Override
	public void setBinaryStream(String parameterName, InputStream x, long length) throws SQLException {
		getStatement().setBinaryStream( parameterName, x, length );
	}

	@Override
	public void setCharacterStream(String parameterName, Reader reader, long length) throws SQLException {
		getStatement().setCharacterStream( parameterName, reader, length );
	}

	@Override
	public void setAsciiStream(String parameterName, InputStream x) throws SQLException {
		getStatement().setAsciiStream( parameterName, x );
	}

	@Override
	public void setBinaryStream(String parameterName, InputStream x) throws SQLException {
		getStatement().setBinaryStream( parameterName, x );
	}

	@Override
	public void setCharacterStream(String parameterName, Reader reader) throws SQLException {
		getStatement().setCharacterStream( parameterName, reader );
	}

	@Override
	public void setNCharacterStream(String parameterName, Reader value) throws SQLException {
		getStatement().setNCharacterStream( parameterName, value );
	}

	@Override
	public void setClob(String parameterName, Reader reader) throws SQLException {
		getStatement().setClob( parameterName, reader );
	}

	@Override
	public void setBlob(String parameterName, InputStream inputStream) throws SQLException {
		getStatement().setBlob( parameterName, inputStream );
	}

	@Override
	public void setNClob(String parameterName, Reader reader) throws SQLException {
		getStatement().setNClob( parameterName, reader );
	}

	@Override
	public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
		return getStatement().getObject( parameterIndex, type );
	}

	@Override
	public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
		return getStatement().getObject( parameterName, type );
	}
}
