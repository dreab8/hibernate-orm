/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package benchmark;


import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

/**
 * @author Andrea Boriero
 */
@State(Scope.Thread)
public class BenchmarkTestBaseSetUp {

	private SessionFactoryImplementor sessionFactory;
	private Metadata metadata;


	@Setup(Level.Trial)
	public void before() {

		final StandardServiceRegistryBuilder ssrBuilder = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" );
		applySettings( ssrBuilder );
		final StandardServiceRegistry ssr = ssrBuilder.build();
		try {
			metadata = buildMetadata( ssr );
			sessionFactory = (SessionFactoryImplementor) metadata.buildSessionFactory();
		}
		catch (Exception e) {
			StandardServiceRegistryBuilder.destroy( ssr );
			SchemaManagementToolCoordinator.ActionGrouping actions = SchemaManagementToolCoordinator.ActionGrouping.interpret(
					ssrBuilder.getSettings() );
			dropDatabase();
			throw e;
		}
		setUp();
	}

	private MetadataImplementor buildMetadata(StandardServiceRegistry ssr) {
		MetadataSources metadataSources = new MetadataSources( ssr );
		applyMetadataSources( metadataSources );
		return (MetadataImplementor) metadataSources.buildMetadata();
	}

	protected void applyMetadataSources(MetadataSources metadataSources) {
	}


	protected void applySettings(StandardServiceRegistryBuilder builder) {
	}

	private void dropDatabase() {
		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().build();
//		try {
//			final DatabaseModel databaseModel = Helper.buildDatabaseModel( buildMetadata( ssr ) );
//			new SchemaExport( databaseModel, ssr ).drop( EnumSet.of( TargetType.DATABASE ) );
//		}
//		finally {
		StandardServiceRegistryBuilder.destroy( ssr );
//		}
	}

	@TearDown(Level.Trial)
	public void after() {
		try {
			tearDown();
			dropDatabase();
			sessionFactory.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void setUp() {
	}

	protected void tearDown() {

	}

	public Object inTransaction(Function<Session, Object> sessionFactoryConsumer) {
		Object result;
		try (Session session = sessionFactory.openSession()) {
			session.getTransaction().begin();
			result = sessionFactoryConsumer.apply( session );
			session.getTransaction().commit();
		}
		return result;
	}

	public void inTransaction(Consumer<Session> sessionFactoryConsumer) {
		try (Session session = sessionFactory.openSession()) {
			session.getTransaction().begin();
			sessionFactoryConsumer.accept( session );
			session.getTransaction().commit();
		}
	}
}
