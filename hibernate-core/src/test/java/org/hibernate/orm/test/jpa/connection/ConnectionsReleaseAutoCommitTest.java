/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.connection;

import java.sql.Connection;
import java.sql.SQLException;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-12197")
@RequiresDialect(H2Dialect.class)
@Jpa(
		annotatedClasses = { ConnectionsReleaseAutoCommitTest.Thing.class },
		integrationSettings = @Setting(name = AvailableSettings.CONNECTION_PROVIDER, value = "org.hibernate.orm.test.jpa.connection.ConnectionProviderDecorator")
)
public class ConnectionsReleaseAutoCommitTest {

	private Connection connection;

	@Test
	public void testConnectionAcquisitionCount(EntityManagerFactoryScope scope) throws SQLException {
		ConnectionProviderDecorator connectionProvider = getConnectionProvider( scope );
		connectionProvider.clear();

		scope.inTransaction( entityManager -> {
			assertEquals( 1, connectionProvider.getConnectionCount() );
			Thing thing = new Thing();
			thing.setId( 1 );
			entityManager.persist( thing );
			assertEquals( 1, connectionProvider.getConnectionCount() );
		} );

		assertEquals( 1, connectionProvider.getConnectionCount() );
		verify( connectionProvider.connection, times( 1 ) ).close();
	}

	private ConnectionProviderDecorator getConnectionProvider(EntityManagerFactoryScope scope) {
		return (ConnectionProviderDecorator) ( (SessionFactoryImplementor) ( scope
				.getEntityManagerFactory() ) ).getServiceRegistry().getService( ConnectionProvider.class );
	}

	@Entity(name = "Thing")
	@Table(name = "Thing")
	public static class Thing {
		@Id
		public Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

}
