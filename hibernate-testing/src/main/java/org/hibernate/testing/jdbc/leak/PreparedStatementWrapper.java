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
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * @author Andrea Boriero
 */
public class PreparedStatementWrapper extends StatementWrapper implements PreparedStatement {
	PreparedStatementWrapper(
			Statement statement,
			StatementInfo statementInfo,
			ResultSetInfo resultsetInfo) {
		super( statement, statementInfo, resultsetInfo );
	}

	private PreparedStatement getStatement() {
		return (PreparedStatement) statement;
	}

	@Override
	public ResultSet executeQuery() throws SQLException {
		return getStatement().executeQuery();
	}

	@Override
	public int executeUpdate() throws SQLException {
		return getStatement().executeUpdate();
	}

	@Override
	public void setNull(int parameterIndex, int sqlType) throws SQLException {
		getStatement().setNull( parameterIndex, sqlType );
	}

	@Override
	public void setBoolean(int parameterIndex, boolean x) throws SQLException {
		getStatement().setBoolean( parameterIndex, x );
	}

	@Override
	public void setByte(int parameterIndex, byte x) throws SQLException {
		getStatement().setByte( parameterIndex, x );
	}

	@Override
	public void setShort(int parameterIndex, short x) throws SQLException {
		getStatement().setShort( parameterIndex, x );
	}

	@Override
	public void setInt(int parameterIndex, int x) throws SQLException {
		getStatement().setInt( parameterIndex, x );
	}

	@Override
	public void setLong(int parameterIndex, long x) throws SQLException {
		getStatement().setLong( parameterIndex, x );
	}

	@Override
	public void setFloat(int parameterIndex, float x) throws SQLException {
		getStatement().setFloat( parameterIndex, x );
	}

	@Override
	public void setDouble(int parameterIndex, double x) throws SQLException {
		getStatement().setDouble( parameterIndex, x );
	}

	@Override
	public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
		getStatement().setBigDecimal( parameterIndex, x );
	}

	@Override
	public void setString(int parameterIndex, String x) throws SQLException {
		getStatement().setString( parameterIndex, x );
	}

	@Override
	public void setBytes(int parameterIndex, byte[] x) throws SQLException {
		getStatement().setBytes( parameterIndex, x );
	}

	@Override
	public void setDate(int parameterIndex, Date x) throws SQLException {
		getStatement().setDate( parameterIndex, x );
	}

	@Override
	public void setTime(int parameterIndex, Time x) throws SQLException {
		getStatement().setTime( parameterIndex, x );
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
		getStatement().setTimestamp( parameterIndex, x );
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
		getStatement().setAsciiStream( parameterIndex, x, length );
	}

	@Override
	public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
		getStatement().setUnicodeStream( parameterIndex, x, length );
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
		getStatement().setBinaryStream( parameterIndex, x, length );
	}

	@Override
	public void clearParameters() throws SQLException {
		getStatement().clearParameters();
	}

	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
		getStatement().setObject( parameterIndex, x, targetSqlType );
	}

	@Override
	public void setObject(int parameterIndex, Object x) throws SQLException {
		getStatement().setObject( parameterIndex, x );
	}

	@Override
	public boolean execute() throws SQLException {
		return getStatement().execute();
	}

	@Override
	public void addBatch() throws SQLException {
		getStatement().addBatch();
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
		getStatement().setCharacterStream( parameterIndex, reader, length );
	}

	@Override
	public void setRef(int parameterIndex, Ref x) throws SQLException {
		getStatement().setRef( parameterIndex, x );
	}

	@Override
	public void setBlob(int parameterIndex, Blob x) throws SQLException {
		getStatement().setBlob( parameterIndex, x );
	}

	@Override
	public void setClob(int parameterIndex, Clob x) throws SQLException {
		getStatement().setClob( parameterIndex, x );
	}

	@Override
	public void setArray(int parameterIndex, Array x) throws SQLException {
		getStatement().setArray( parameterIndex, x );
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return getStatement().getMetaData();
	}

	@Override
	public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
		getStatement().setDate( parameterIndex, x, cal );
	}

	@Override
	public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
		getStatement().setTime( parameterIndex, x, cal );
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
		getStatement().setTimestamp( parameterIndex, x, cal );
	}

	@Override
	public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
		getStatement().setNull( parameterIndex, sqlType, typeName );
	}

	@Override
	public void setURL(int parameterIndex, URL x) throws SQLException {
		getStatement().setURL( parameterIndex, x );
	}

	@Override
	public ParameterMetaData getParameterMetaData() throws SQLException {
		return getStatement().getParameterMetaData();
	}

	@Override
	public void setRowId(int parameterIndex, RowId x) throws SQLException {
		getStatement().setRowId( parameterIndex, x );
	}

	@Override
	public void setNString(int parameterIndex, String value) throws SQLException {
		getStatement().setNString( parameterIndex, value );
	}

	@Override
	public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
		getStatement().setNCharacterStream( parameterIndex, value, length );
	}

	@Override
	public void setNClob(int parameterIndex, NClob value) throws SQLException {
		getStatement().setNClob( parameterIndex, value );
	}

	@Override
	public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
		getStatement().setClob( parameterIndex, reader, length );
	}

	@Override
	public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
		getStatement().setBlob( parameterIndex, inputStream, length );
	}

	@Override
	public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
		getStatement().setNClob( parameterIndex, reader, length );
	}

	@Override
	public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
		getStatement().setSQLXML( parameterIndex, xmlObject );
	}

	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
		getStatement().setObject( parameterIndex, x, targetSqlType, scaleOrLength );
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
		getStatement().setAsciiStream( parameterIndex, x, length );
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
		getStatement().setBinaryStream( parameterIndex, x, length );
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
		getStatement().setCharacterStream( parameterIndex, reader, length );
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
		getStatement().setAsciiStream( parameterIndex, x );
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
		getStatement().setBinaryStream( parameterIndex, x );
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
		getStatement().setCharacterStream( parameterIndex, reader );
	}

	@Override
	public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
		getStatement().setNCharacterStream( parameterIndex, value );
	}

	@Override
	public void setClob(int parameterIndex, Reader reader) throws SQLException {
		getStatement().setClob( parameterIndex, reader );
	}

	@Override
	public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
		getStatement().setBlob( parameterIndex, inputStream );
	}

	@Override
	public void setNClob(int parameterIndex, Reader reader) throws SQLException {
		getStatement().setNClob( parameterIndex, reader );
	}
}
