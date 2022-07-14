/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.insert;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.spi.ParameterBinderImplementor;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.MutationStatementPreparer;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.pretty.MessageHelper;

/**
 * Abstract InsertGeneratedIdentifierDelegate implementation where the
 * underlying strategy requires a subsequent select after the insert
 * to determine the generated identifier.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSelectingDelegate implements InsertGeneratedIdentifierDelegate {
	private final PostInsertIdentityPersister persister;

	protected AbstractSelectingDelegate(PostInsertIdentityPersister persister) {
		this.persister = persister;
	}

	/**
	 * Get the SQL statement to be used to retrieve generated key values.
	 *
	 * @return The SQL command string
	 */
	protected abstract String getSelectSQL();

	protected void bindParameters(
			Object entity,
			PreparedStatement ps,
			SharedSessionContractImplementor session) throws SQLException {
	}

	/**
	 * Extract the generated key value from the given result set
	 * from execution of {@link #getSelectSQL()}.
	 *
	 */
	protected abstract Object extractGeneratedValue(
			Object entity,
			ResultSet rs,
			SharedSessionContractImplementor session) throws SQLException;

	@Override
	public PreparedStatement prepareStatement(String insertSql, SharedSessionContractImplementor session) {
		final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
		final MutationStatementPreparer statementPreparer = jdbcCoordinator.getMutationStatementPreparer();
		return statementPreparer.prepareStatement( insertSql, PreparedStatement.NO_GENERATED_KEYS );
	}

	@Override
	public Object performInsert(
			PreparedStatementDetails insertStatementDetails,
			ParameterBinderImplementor parameterBinder,
			Object entity,
			SharedSessionContractImplementor session) {
		final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
		final JdbcServices jdbcServices = session.getJdbcServices();

		final SqlStatementLogger sqlStatementLogger = jdbcServices.getSqlStatementLogger();
		sqlStatementLogger.logStatement( insertStatementDetails.getTableMutation().getSqlString() );
		parameterBinder.beforeStatement( insertStatementDetails.getTableMutation().getTableName(), session );

		final PreparedStatement insertStatement = insertStatementDetails.getStatement();
		jdbcCoordinator.getResultSetReturn().executeUpdate( insertStatement );

		// the insert is complete, select the generated id...

		final String idSelectSql = getSelectSQL();
		final PreparedStatement idSelect = jdbcCoordinator
				.getStatementPreparer()
				.prepareStatement( idSelectSql );

		try {
			bindParameters( entity, idSelect, session );

			final ResultSet rs = session.getJdbcCoordinator().getResultSetReturn().extract( idSelect );
			try {
				return extractGeneratedValue( entity, rs, session );
			}
			catch (SQLException e) {
				throw jdbcServices.getSqlExceptionHelper().convert(
						e,
						"Unable to execute post-insert id selection query",
						idSelectSql
				);
			}
			finally {
				session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( idSelect );
				session.getJdbcCoordinator().afterStatementExecution();
			}
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					"Unable to bind parameters for post-insert id selection query",
					idSelectSql
			);
		}
	}

	@Override
	public final Object performInsert(
			String insertSQL,
			SharedSessionContractImplementor session,
			Binder binder) {
		try {
			// prepare and execute the insert
			PreparedStatement insert = session
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( insertSQL, PreparedStatement.NO_GENERATED_KEYS );
			try {
				binder.bindValues( insert );
				session.getJdbcCoordinator().getResultSetReturn().executeUpdate( insert );
			}
			finally {
				session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( insert );
				session.getJdbcCoordinator().afterStatementExecution();
			}
		}
		catch (SQLException sqle) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not insert: " + MessageHelper.infoString( persister ),
					insertSQL
			);
		}

		final String selectSQL = getSelectSQL();

		try {
			//fetch the generated id in a separate query
			PreparedStatement idSelect = session
					.getJdbcCoordinator()
					.getStatementPreparer()
					.prepareStatement( selectSQL, false );
			try {
				bindParameters( binder.getEntity(), idSelect, session );
				ResultSet rs = session.getJdbcCoordinator().getResultSetReturn().extract( idSelect );
				try {
					return extractGeneratedValue( binder.getEntity(), rs, session );
				}
				finally {
					session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( rs, idSelect );
				}
			}
			finally {
				session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( idSelect );
				session.getJdbcCoordinator().afterStatementExecution();
			}

		}
		catch (SQLException sqle) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					sqle,
					"could not retrieve generated id after insert: " + MessageHelper.infoString( persister ),
					insertSQL
			);
		}
	}

}
