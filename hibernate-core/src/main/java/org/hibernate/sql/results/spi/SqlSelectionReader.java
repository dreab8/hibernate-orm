/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * A low-level reader for extracting JDBC results.  We always extract "basic" values
 * via this contract; various other contracts may consume those basic vales into compositions
 * like an entity, embeddable or collection.
 *
 * SqlSelectionReader generally "wraps" a {@link org.hibernate.type.descriptor.spi.ValueExtractor}
 * and dispatches its calls to that wrapped ValueExtractor.  The SqlSelectionReader result
 * is generally used to populate the "current row JDBC values" array
 * ({@link RowProcessingState#getJdbcValue})
 *
 * @author Steve Ebersole
 */
public interface SqlSelectionReader<T> {

	// todo (6.0) : rename these methods for consistency
	//		- atm its a mix between read and extract.  pick one

	/**
	 * Read a value from the underlying JDBC ResultSet
	 *
	 * @param resultSet The JDBC ResultSet from which to extract a value
	 * @param persistenceContext The current persistence context
	 * @param sqlSelection Description of the JDBC value to be extracted
	 *
	 * @return The extracted value
	 *
	 * @throws SQLException Exceptions from the underlying JDBC objects are simply re-thrown.
	 */
	T read(
			ResultSet resultSet,
			SharedSessionContractImplementor persistenceContext,
			SqlSelection sqlSelection) throws SQLException;


	/**
	 * Extract the value of an INOUT/OUT parameter from the JDBC CallableStatement *by position*
	 *
	 * @param statement The CallableStatement from which to extract the parameter value.
	 * @param jdbcParameterIndex The index of the registered INOUT/OUT parameter
	 * @param persistenceContext The current persistence context
	 *
	 * @return The extracted value
	 *
	 * @throws SQLException Exceptions from the underlying JDBC objects are simply re-thrown.
	 */
	T extractParameterValue(
			CallableStatement statement,
			SharedSessionContractImplementor persistenceContext,
			int jdbcParameterIndex) throws SQLException;

	/**
	 * Extract the value of an INOUT/OUT parameter from the JDBC CallableStatement *by name*
	 *
	 * @param statement The CallableStatement from which to extract the parameter value.
	 * @param jdbcParameterName The parameter name.
	 * @param persistenceContext The current persistence context
	 *
	 * @return The extracted value.
	 *
	 * @throws SQLException Exceptions from the underlying JDBC objects are simply re-thrown.
	 */
	T extractParameterValue(
			CallableStatement statement,
			SharedSessionContractImplementor persistenceContext,
			String jdbcParameterName) throws SQLException;
}
