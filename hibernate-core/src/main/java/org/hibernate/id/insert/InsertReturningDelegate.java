/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.id.insert;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.id.PostInsertIdentityPersister;

/**
 * Delegate for dealing with IDENTITY columns where the dialect supports returning
 * the generated IDENTITY value directly from the insert statement.
 *
 * @see org.hibernate.id.IdentityGenerator
 * @see IdentityColumnSupport#supportsInsertSelectIdentity()
 */
public class InsertReturningDelegate extends AbstractReturningDelegate {
	private final PostInsertIdentityPersister persister;
	private final Dialect dialect;

	public InsertReturningDelegate(PostInsertIdentityPersister persister, Dialect dialect) {
		super( persister );
		this.persister = persister;
		this.dialect = dialect;
	}

	@Override
	public IdentifierGeneratingInsert prepareIdentifierGeneratingInsert(SqlStringGenerationContext context) {
		InsertSelectIdentityInsert insert = new InsertSelectIdentityInsert( dialect );
		insert.addIdentityColumn( persister.getRootTableKeyColumnNames()[ 0 ] );
		return insert;
	}

	@Override
	protected Object executeAndExtract(
			String insertSql,
			PreparedStatement insertStatement,
			SharedSessionContractImplementor session) {
		final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
		final JdbcServices jdbcServices = session.getJdbcServices();

		final ResultSet rs = jdbcCoordinator.getResultSetReturn().execute( insertStatement );

		try {
			return IdentifierGeneratorHelper.getGeneratedIdentity(
					rs,
					persister.getRootTableKeyColumnNames()[ 0 ],
					persister.getIdentifierType(),
					jdbcServices.getJdbcEnvironment().getDialect()
			);
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					"Unable to extract generated key(s) from generated-keys ResultSet",
					insertSql
			);
		}
		finally {
			jdbcCoordinator
					.getLogicalConnection()
					.getResourceRegistry()
					.release( rs, insertStatement );
		}
	}

	@Override
	public String prepareIdentifierGeneratingInsert(String insertSQL) {
		return dialect.getIdentityColumnSupport().appendIdentitySelectToInsert( insertSQL );
	}

	@Override
	public PreparedStatement prepareStatement(String insertSql, SharedSessionContractImplementor session) {
		return session
				.getJdbcCoordinator()
				.getStatementPreparer()
				.prepareStatement( insertSql, PreparedStatement.NO_GENERATED_KEYS );
	}
}
