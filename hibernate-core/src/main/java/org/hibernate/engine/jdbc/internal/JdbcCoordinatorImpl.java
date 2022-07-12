/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.TransactionException;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.Batch2;
import org.hibernate.engine.jdbc.batch.spi.Batch2Builder;
import org.hibernate.engine.jdbc.batch.spi.BatchBuilder;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.group.PreparedStatementGroup;
import org.hibernate.engine.jdbc.spi.InvalidatableWrapper;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.JdbcWrapper;
import org.hibernate.engine.jdbc.spi.MutationStatementPreparer;
import org.hibernate.engine.jdbc.spi.ResultSetReturn;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.StatementPreparer;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jdbc.WorkExecutor;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.hibernate.resource.jdbc.ResourceRegistry;
import org.hibernate.resource.jdbc.internal.LogicalConnectionManagedImpl;
import org.hibernate.resource.jdbc.internal.LogicalConnectionProvidedImpl;
import org.hibernate.resource.jdbc.internal.ResourceRegistryStandardImpl;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransaction;

/**
 * Standard Hibernate implementation of {@link JdbcCoordinator}
 * <p/>
 * IMPL NOTE : Custom serialization handling!
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 * @author Sanne Grinovero
 */
public class JdbcCoordinatorImpl implements JdbcCoordinator {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( JdbcCoordinatorImpl.class );

	private transient final LogicalConnectionImplementor logicalConnection;
	private transient final JdbcSessionOwner owner;

	private transient final JdbcServices jdbcServices;

	private transient Batch currentBatch;
	private transient Batch2 currentBatch2;

	private transient long transactionTimeOutInstant = -1;

	private Statement lastQuery;
	private final boolean isUserSuppliedConnection;

	/**
	 * If true, manually (and temporarily) circumvent aggressive release processing.
	 */
	private boolean releasesEnabled = true;

	/**
	 * Constructs a JdbcCoordinatorImpl
	 *
	 * @param userSuppliedConnection The user supplied connection (may be null)
	 */
	public JdbcCoordinatorImpl(
			Connection userSuppliedConnection,
			JdbcSessionOwner owner,
			JdbcServices jdbcServices) {
		this.isUserSuppliedConnection = userSuppliedConnection != null;

		final ResourceRegistry resourceRegistry = new ResourceRegistryStandardImpl(
				owner.getJdbcSessionContext().getObserver()
		);
		if ( isUserSuppliedConnection ) {
			this.logicalConnection = new LogicalConnectionProvidedImpl( userSuppliedConnection, resourceRegistry );
		}
		else {
			this.logicalConnection = new LogicalConnectionManagedImpl(
					owner.getJdbcConnectionAccess(),
					owner.getJdbcSessionContext(),
					resourceRegistry,
					jdbcServices
			);
		}
		this.owner = owner;
		this.jdbcServices = jdbcServices;
	}

	private JdbcCoordinatorImpl(
			LogicalConnectionImplementor logicalConnection,
			boolean isUserSuppliedConnection,
			JdbcSessionOwner owner) {
		this.logicalConnection = logicalConnection;
		this.isUserSuppliedConnection = isUserSuppliedConnection;
		this.owner = owner;
		this.jdbcServices = owner.getJdbcSessionContext()
				.getServiceRegistry()
				.getService( JdbcServices.class );
	}

	@Override
	public LogicalConnectionImplementor getLogicalConnection() {
		return logicalConnection;
	}

	protected SessionFactoryImplementor sessionFactory() {
		return this.owner.getJdbcSessionContext().getSessionFactory();
	}

	protected BatchBuilder batchBuilder() {
		return sessionFactory().getServiceRegistry().getService( BatchBuilder.class );
	}

	/**
	 * Access to the SqlExceptionHelper
	 *
	 * @return The SqlExceptionHelper
	 */
	public SqlExceptionHelper sqlExceptionHelper() {
		return jdbcServices.getSqlExceptionHelper();
	}

	private int flushDepth;

	@Override
	public void flushBeginning() {
		if ( flushDepth == 0 ) {
			releasesEnabled = false;
		}
		flushDepth++;
	}

	@Override
	public void flushEnding() {
		flushDepth--;
		if ( flushDepth < 0 ) {
			throw new HibernateException( "Mismatched flush handling" );
		}
		if ( flushDepth == 0 ) {
			releasesEnabled = true;
		}
		
		afterStatementExecution();
	}

	@Override
	public Connection close() {
		LOG.tracev( "Closing JDBC container [{0}]", this );
		Connection connection;
		try {
			if ( currentBatch != null ) {
				LOG.closingUnreleasedBatch();
				currentBatch.release();
			}
			if ( currentBatch2 != null ) {
				LOG.closingUnreleasedBatch();
				currentBatch2.release();
			}
		}
		finally {
			connection = logicalConnection.close();
		}
		return connection;
	}

	@Override
	public Batch getBatch(BatchKey key) {
		if ( currentBatch != null ) {
			if ( currentBatch.getKey().equals( key ) ) {
				return currentBatch;
			}
			else {
				currentBatch.execute();
				currentBatch.release();
			}
		}
		currentBatch = batchBuilder().buildBatch( key, this );
		return currentBatch;
	}

	@Override
	public Batch2 getBatch2(
			BatchKey key,
			Integer batchSizeToUse,
			Supplier<PreparedStatementGroup> statementGroupSupplier) {
		if ( currentBatch2 != null ) {
			if ( currentBatch2.getKey().equals( key ) ) {
				return currentBatch2;
			}
			else {
				currentBatch2.execute();
				currentBatch2.release();
			}
		}

		final Batch2Builder batchBuilder = sessionFactory().getServiceRegistry().getService( Batch2Builder.class );
		currentBatch2 = batchBuilder.buildBatch( key, batchSizeToUse, statementGroupSupplier, this );

		return currentBatch2;
	}

	@Override
	public void executeBatch() {
		if ( currentBatch != null ) {
			currentBatch.execute();
			// needed?
			currentBatch.release();
		}

		if ( currentBatch2 != null ) {
			currentBatch2.execute();
			currentBatch2.release();
		}
	}

	@Override
	public void abortBatch() {
		if ( currentBatch != null ) {
			currentBatch.release();
		}

		if ( currentBatch2 != null ) {
			currentBatch2.release();
		}
	}

	private transient StatementPreparer statementPreparer;

	@Override
	public StatementPreparer getStatementPreparer() {
		if ( statementPreparer == null ) {
			statementPreparer = new StatementPreparerImpl( this, jdbcServices );
		}
		return statementPreparer;
	}

	private transient MutationStatementPreparer mutationStatementPreparer;

	@Override
	public MutationStatementPreparer getMutationStatementPreparer() {
		if ( mutationStatementPreparer == null ) {
			mutationStatementPreparer = new MutationStatementPreparerImpl( this, jdbcServices );
		}
		return mutationStatementPreparer;
	}

	private transient ResultSetReturn resultSetExtractor;

	@Override
	public ResultSetReturn getResultSetReturn() {
		if ( resultSetExtractor == null ) {
			resultSetExtractor = new ResultSetReturnImpl( this, jdbcServices );
		}
		return resultSetExtractor;
	}

	@Override
	public void setTransactionTimeOut(int seconds) {
		transactionTimeOutInstant = System.currentTimeMillis() + ( seconds * 1000L );
	}

	@Override
	public void flushBeforeTransactionCompletion() {
		getJdbcSessionOwner().flushBeforeTransactionCompletion();
	}

	@Override
	public int determineRemainingTransactionTimeOutPeriod() {
		if ( transactionTimeOutInstant < 0 ) {
			return -1;
		}
		final int secondsRemaining = (int) ((transactionTimeOutInstant - System.currentTimeMillis()) / 1000);
		if ( secondsRemaining <= 0 ) {
			throw new TransactionException( "transaction timeout expired" );
		}
		return secondsRemaining;
	}

	@Override
	public void afterStatementExecution() {
		final ConnectionReleaseMode connectionReleaseMode = getLogicalConnection().getConnectionHandlingMode().getReleaseMode();
		LOG.tracev( "Starting after statement execution processing [{0}]", connectionReleaseMode );
		if ( connectionReleaseMode == ConnectionReleaseMode.AFTER_STATEMENT ) {
			if ( ! releasesEnabled ) {
				LOG.debug( "Skipping aggressive release due to manual disabling" );
				return;
			}
			if ( hasRegisteredResources() ) {
				LOG.debug( "Skipping aggressive release due to registered resources" );
				return;
			}
			getLogicalConnection().afterStatement();
		}
	}

	@Override
	public void afterTransaction() {
		transactionTimeOutInstant = -1;
		ConnectionReleaseMode connectionReleaseMode = getLogicalConnection().getConnectionHandlingMode().getReleaseMode();
		if ( connectionReleaseMode == ConnectionReleaseMode.AFTER_STATEMENT ||
				connectionReleaseMode == ConnectionReleaseMode.AFTER_TRANSACTION ||
				connectionReleaseMode == ConnectionReleaseMode.BEFORE_TRANSACTION_COMPLETION ) {
			this.logicalConnection.afterTransaction();
		}
	}

	private void releaseResources() {
		getLogicalConnection().getResourceRegistry().releaseResources();
	}

	private boolean hasRegisteredResources() {
		return getLogicalConnection().getResourceRegistry().hasRegisteredResources();
	}

	private ConnectionReleaseMode determineConnectionReleaseMode(
			JdbcConnectionAccess jdbcConnectionAccess,
			boolean isUserSuppliedConnection,
			ConnectionReleaseMode connectionReleaseMode) {
		if ( isUserSuppliedConnection ) {
			return ConnectionReleaseMode.ON_CLOSE;
		}
		else if ( connectionReleaseMode == ConnectionReleaseMode.AFTER_STATEMENT &&
				! jdbcConnectionAccess.supportsAggressiveRelease() ) {
			LOG.debug( "Connection provider reports to not support aggressive release; overriding" );
			return ConnectionReleaseMode.AFTER_TRANSACTION;
		}
		else {
			return connectionReleaseMode;
		}
	}
	@Override
	public <T> T coordinateWork(WorkExecutorVisitable<T> work) {
		final Connection connection = getLogicalConnection().getPhysicalConnection();
		try {
			final T result = work.accept( new WorkExecutor<>(), connection );
			afterStatementExecution();
			return result;
		}
		catch ( SQLException e ) {
			throw sqlExceptionHelper().convert( e, "error executing work" );
		}
	}

	@Override
	public boolean isReadyForSerialization() {
		return this.isUserSuppliedConnection
				? ! getLogicalConnection().isPhysicallyConnected()
				: ! hasRegisteredResources();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void registerLastQuery(Statement statement) {
		LOG.tracev( "Registering last query statement [{0}]", statement );
		if ( statement instanceof JdbcWrapper ) {
			final JdbcWrapper<Statement> wrapper = (JdbcWrapper<Statement>) statement;
			registerLastQuery( wrapper.getWrappedObject() );
			return;
		}
		lastQuery = statement;
	}

	@Override
	public void cancelLastQuery() {
		try {
			if ( lastQuery != null ) {
				lastQuery.cancel();
			}
		}
		catch (SQLException sqle) {
			SqlExceptionHelper sqlExceptionHelper = jdbcServices.getSqlExceptionHelper();
			//Should always be non-null, but to make sure as the implementation is lazy:
			if ( sqlExceptionHelper == null ) {
				sqlExceptionHelper = new SqlExceptionHelper( false );
			}
			throw sqlExceptionHelper.convert( sqle, "Cannot cancel query" );
		}
		finally {
			lastQuery = null;
		}
	}

	@Override
	public void enableReleases() {
		releasesEnabled = true;
	}
	
	@Override
	public void disableReleases() {
		releasesEnabled = false;
	}

	@SuppressWarnings("unchecked")
	protected void close(Statement statement) {
		LOG.tracev( "Closing prepared statement [{0}]", statement );
		
		// Important for Statement caching -- some DBs (especially Sybase) log warnings on every Statement under
		// certain situations.
		sqlExceptionHelper().logAndClearWarnings( statement );

		if ( statement instanceof InvalidatableWrapper ) {
			final InvalidatableWrapper<Statement> wrapper = (InvalidatableWrapper<Statement>) statement;
			close( wrapper.getWrappedObject() );
			wrapper.invalidate();
			return;
		}

		try {
			// if we are unable to "clean" the prepared statement,
			// we do not close it
			try {
				if ( statement.getMaxRows() != 0 ) {
					statement.setMaxRows( 0 );
				}
				if ( statement.getQueryTimeout() != 0 ) {
					statement.setQueryTimeout( 0 );
				}
			}
			catch( SQLException sqle ) {
				// there was a problem "cleaning" the prepared statement
				if ( LOG.isDebugEnabled() ) {
					LOG.debugf( "Exception clearing maxRows/queryTimeout [%s]", sqle.getMessage() );
				}
				// EARLY EXIT!!!
				return;
			}
			statement.close();
			if ( lastQuery == statement ) {
				lastQuery = null;
			}
		}
		catch ( Exception e ) {
			LOG.debugf( "Unable to release JDBC statement [%s]", e.getMessage() );
		}
	}


	protected void close(ResultSet resultSet) {
		LOG.tracev( "Closing result set [{0}]", resultSet );

		if ( resultSet instanceof InvalidatableWrapper ) {
			@SuppressWarnings("unchecked")
			final InvalidatableWrapper<ResultSet> wrapper = (InvalidatableWrapper<ResultSet>) resultSet;
			close( wrapper.getWrappedObject() );
			wrapper.invalidate();
			return;
		}

		try {
			resultSet.close();
		}
		catch ( Exception e ) {
			// try to handle general errors more elegantly
			LOG.debugf( "Unable to release JDBC result set [%s]", e.getMessage() );
		}
	}

	@Override
	public boolean isActive() {
		return !sessionFactory().isClosed();
	}

	@Override
	public void afterTransactionBegin() {
		owner.afterTransactionBegin();
	}

	@Override
	public void beforeTransactionCompletion() {
		this.owner.beforeTransactionCompletion();
		this.logicalConnection.beforeTransactionCompletion();
	}

	@Override
	public void afterTransactionCompletion(boolean successful, boolean delayed) {
		afterTransaction();
		owner.afterTransactionCompletion( successful, delayed );
	}

	@Override
	public JdbcSessionOwner getJdbcSessionOwner() {
		return this.owner;
	}

	@Override
	public JdbcResourceTransaction getResourceLocalTransaction() {
		return logicalConnection.getPhysicalJdbcTransaction();
	}

	/**
	 * JDK serialization hook
	 *
	 * @param oos The stream into which to write our state
	 *
	 * @throws IOException Trouble accessing the stream
	 */
	@Override
	public void serialize(ObjectOutputStream oos) throws IOException {
		if ( ! isReadyForSerialization() ) {
			throw new HibernateException( "Cannot serialize Session while connected" );
		}
		oos.writeBoolean( isUserSuppliedConnection );
		logicalConnection.serialize( oos );
	}

	/**
	 * JDK deserialization hook
	 *
	 * @param ois The stream into which to write our state
	 * @param owner The Jdbc Session owner which owns the JdbcCoordinatorImpl to be deserialized.
	 *
	 * @return The deserialized JdbcCoordinatorImpl
	 *
	 * @throws IOException Trouble accessing the stream
	 * @throws ClassNotFoundException Trouble reading the stream
	 */
	public static JdbcCoordinatorImpl deserialize(
			ObjectInputStream ois,
			JdbcSessionOwner owner) throws IOException, ClassNotFoundException {
		final boolean isUserSuppliedConnection = ois.readBoolean();
		LogicalConnectionImplementor logicalConnection;
		if ( isUserSuppliedConnection ) {
			logicalConnection = LogicalConnectionProvidedImpl.deserialize( ois );
		}
		else {
			logicalConnection = LogicalConnectionManagedImpl.deserialize(
					ois,
					owner.getJdbcConnectionAccess(),
					owner.getJdbcSessionContext()
			);
		}
		return new JdbcCoordinatorImpl( logicalConnection, isUserSuppliedConnection, owner );
	}
}
