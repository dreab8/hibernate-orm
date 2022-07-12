/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.jpa.transaction.batch;

import java.sql.SQLException;
import java.util.function.Supplier;

import org.hibernate.engine.jdbc.batch.internal.Batch2BuilderImpl;
import org.hibernate.engine.jdbc.batch.spi.Batch2;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchObserver;
import org.hibernate.engine.jdbc.group.PreparedStatementGroup;
import org.hibernate.engine.jdbc.mutation.spi.ParameterBinderImplementor;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;

import org.hibernate.testing.orm.junit.SettingProvider;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractBatchingTest {

	protected static Batch2Wrapper batchWrapper;
	protected static boolean wasReleaseCalled;
	protected static int numberOfStatementsBeforeRelease = -1;
	protected static int numberOfStatementsAfterRelease = -1;

	public static class Batch2BuilderSettingProvider implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return Batch2BuilderLocal.class.getName();
		}
	}

	public static class Batch2BuilderLocal extends Batch2BuilderImpl {
		private final boolean throwError;

		public Batch2BuilderLocal() {
			this( false );
		}

		public Batch2BuilderLocal(boolean throwError) {
			super( 50 );
			this.throwError = throwError;
		}

		@Override
		public Batch2 buildBatch(
				BatchKey key,
				Integer batchSize,
				Supplier<PreparedStatementGroup> statementGroupSupplier,
				JdbcCoordinator jdbcCoordinator) {
			batchWrapper = new Batch2Wrapper(
					throwError,
					super.buildBatch( key, batchSize, statementGroupSupplier, jdbcCoordinator ),
					jdbcCoordinator
			);
			return batchWrapper;
		}
	}

	public static class ErrorBatch2BuilderSettingProvider implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return ErrorBatch2BuilderLocal.class.getName();
		}
	}

	public static class ErrorBatch2BuilderLocal extends Batch2BuilderLocal {
		public ErrorBatch2BuilderLocal() {
			super( true );
		}
	}

	public static class Batch2Wrapper implements Batch2 {
		private final boolean throwError;
		private final Batch2 wrapped;
		private final JdbcCoordinator jdbcCoordinator;

		private int numberOfBatches;
		private int numberOfSuccessfulBatches;

		public Batch2Wrapper(boolean throwError, Batch2 wrapped, JdbcCoordinator jdbcCoordinator) {
			this.throwError = throwError;
			this.wrapped = wrapped;
			this.jdbcCoordinator = jdbcCoordinator;
		}

		public int getNumberOfBatches() {
			return numberOfBatches;
		}

		public int getNumberOfSuccessfulBatches() {
			return numberOfSuccessfulBatches;
		}

		@Override
		public BatchKey getKey() {
			return wrapped.getKey();
		}

		@Override
		public void addObserver(BatchObserver observer) {
			wrapped.addObserver( observer );
		}

		@Override
		public PreparedStatementGroup getStatementGroup() {
			return wrapped.getStatementGroup();
		}

		@Override
		public void addToBatch(ParameterBinderImplementor parameterBinder) {
			numberOfBatches++;
			wrapped.addToBatch( parameterBinder );
			numberOfStatementsBeforeRelease = wrapped.getStatementGroup().getNumberOfStatements();

			if ( throwError  ) {
				// Implementations really should call abortBatch() before throwing an exception.
				// Purposely skipping the call to abortBatch() to ensure that Hibernate works properly when
				// a legacy implementation does not call abortBatch().
				final JdbcServices jdbcServices = jdbcCoordinator.getJdbcSessionOwner()
						.getJdbcSessionContext()
						.getServiceRegistry()
						.getService( JdbcServices.class );
				throw jdbcServices.getSqlExceptionHelper().convert(
						new SQLException( "fake SQLException" ),
						"could not perform addBatch",
						wrapped.getStatementGroup().getSqlGroup().getSingleTableMutation().getSqlString()
				);
			}

			numberOfSuccessfulBatches++;
		}

		@Override
		public void execute() {
			wrapped.execute();
		}

		@Override
		public void release() {
			wasReleaseCalled = true;
			wrapped.release();
			numberOfStatementsAfterRelease = wrapped.getStatementGroup().getNumberOfStatements();
		}
	}
}
