/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jdbc.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.resource.jdbc.ResourceRegistry;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class BasicConnectionTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testExceptionHandling() {
		Session session = openSession();
		SessionImplementor sessionImpl = (SessionImplementor) session;
		boolean caught = false;
		try {
			PreparedStatement ps = sessionImpl.getJdbcCoordinator().getStatementPreparer()
					.prepareStatement( "select count(*) from NON_EXISTENT" );
			sessionImpl.getJdbcCoordinator().getResultSetReturn().execute( ps );
		}
		catch ( JDBCException ok ) {
			caught = true;
		}
		finally {
			session.close();
		}

		assertTrue( "The connection did not throw a JDBCException as expected", caught );
	}

	@Test
	public void testBasicJdbcUsage() throws JDBCException {
		Session session = openSession();
		SessionImplementor sessionImpl = (SessionImplementor) session;
		JdbcCoordinator jdbcCoord = sessionImpl.getJdbcCoordinator();

		final ResourceRegistry resourceRegistry = getResourceRegistry( jdbcCoord );
		PreparedStatement ps1 = null;
		PreparedStatement ps2 = null;
		ResultSet firstResultSet = null;
		ResultSet seconResultSet = null;
		try {
			Statement statement = jdbcCoord.getStatementPreparer().createStatement();
			String dropSql = getDialect().getDropTableString( "SANDBOX_JDBC_TST" );
			try {
				jdbcCoord.getResultSetReturn().execute( statement, dropSql );
			}
			catch ( Exception e ) {
				// ignore if the DB doesn't support "if exists" and the table doesn't exist
			}
			jdbcCoord.getResultSetReturn().execute( statement,
					"create table SANDBOX_JDBC_TST ( ID integer, NAME varchar(100) )" );
			assertTrue( resourceRegistry.hasRegisteredResources() );
			assertTrue( jdbcCoord.getLogicalConnection().isPhysicallyConnected() );
			resourceRegistry.release( statement );
			assertFalse( resourceRegistry.hasRegisteredResources() );
			assertTrue( jdbcCoord.getLogicalConnection().isPhysicallyConnected() ); // after_transaction specified

			ps1 = jdbcCoord.getStatementPreparer().prepareStatement(
					"insert into SANDBOX_JDBC_TST( ID, NAME ) values ( ?, ? )" );
			ps1.setLong( 1, 1 );
			ps1.setString( 2, "name" );
			firstResultSet = jdbcCoord.getResultSetReturn().execute( ps1 );

			ps2 = jdbcCoord.getStatementPreparer().prepareStatement( "select * from SANDBOX_JDBC_TST" );
			seconResultSet = jdbcCoord.getResultSetReturn().extract( ps2 );

			assertTrue( resourceRegistry.hasRegisteredResources() );
		}
		catch ( SQLException e ) {
			fail( "incorrect exception type : sqlexception" );
		}
		finally {
			try {
				session.doWork( connection -> {
					try(Statement stmnt = connection.createStatement()){
						stmnt.execute( getDialect().getDropTableString( "SANDBOX_JDBC_TST" ) );
					}
				} );
			}
			finally {
				if(firstResultSet != null){
					resourceRegistry.release( firstResultSet );
				}
				if(seconResultSet != null){
					resourceRegistry.release( seconResultSet );
				}
				if(ps1 != null){
					try {
						ps1.close();
					}
					catch (SQLException e) {
					}
				}
				if(ps2 != null){
					try {
						ps2.close();
					}
					catch (SQLException e) {
					}
				}
				resourceRegistry.releaseResources();
				session.close();
			}
		}

		assertFalse( resourceRegistry.hasRegisteredResources() );
	}

	private ResourceRegistry getResourceRegistry(JdbcCoordinator jdbcCoord) {
		return jdbcCoord.getResourceRegistry();
	}
}
