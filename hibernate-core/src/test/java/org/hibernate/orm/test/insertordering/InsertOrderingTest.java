/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.insertordering;

import java.util.Iterator;
import java.util.function.Supplier;

import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.batch.internal.Batch2BuilderInitiator;
import org.hibernate.engine.jdbc.batch.internal.Batch2Impl;
import org.hibernate.engine.jdbc.batch.spi.Batch2;
import org.hibernate.engine.jdbc.batch.spi.Batch2Builder;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.group.PreparedStatementGroup;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/insertordering/Mapping.hbm.xml"
)
@SessionFactory
@ServiceRegistry(
		settings = {@Setting( name = Environment.ORDER_INSERTS, value = "true"),
				@Setting( name = Environment.STATEMENT_BATCH_SIZE, value = "10"),
				@Setting( name = Batch2BuilderInitiator.BUILDER, value = "org.hibernate.orm.test.insertordering.InsertOrderingTest$StatsBatchBuilder" )
		}
)
public class InsertOrderingTest {

	@Test
	public void testBatchOrdering(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					int iterations = 12;
					for ( int i = 0; i < iterations; i++ ) {
						User user = new User( "user-" + i );
						Group group = new Group( "group-" + i );
						session.save( user );
						session.save( group );
						user.addMembership( group );
					}
					StatsBatch.reset();
				}
		);


		// 1 for first 10 User (1)
		// 1 for final 2 User (2)
		// 1 for first 10 Group (3)
		// 1 for last 2 Group (4)
		// 1 for first 10 Membership (5)
		// 1 for last 2 Membership (6)
		assertEquals( 6, StatsBatch.numberOfBatches );

		scope.inTransaction(
				session -> {
					Iterator users = session.createQuery(
							"from User u left join fetch u.memberships m left join fetch m.group" ).list().iterator();
					while ( users.hasNext() ) {
						session.delete( users.next() );
					}
				}
		);
	}

	public static class StatsBatchBuilder implements Batch2Builder {
		@Override
		public Batch2 buildBatch(
				BatchKey key,
				Integer batchSize,
				Supplier<PreparedStatementGroup> statementGroupSupplier,
				JdbcCoordinator jdbcCoordinator) {
			return new StatsBatch( key, batchSize, statementGroupSupplier.get(), jdbcCoordinator );
		}
	}

	public static class StatsBatch extends Batch2Impl {
		private static int numberOfBatches = -1;

		public StatsBatch(
				BatchKey key,
				int batchSize,
				PreparedStatementGroup statementGroup,
				JdbcCoordinator jdbcCoordinator) {
			super( key, statementGroup, batchSize, jdbcCoordinator );
		}

		static void reset() {
			numberOfBatches = -1;
		}

		@Override
		protected void performExecution() {
			super.performExecution();
			if ( numberOfBatches < 0 ) {
				numberOfBatches = 0;
			}
			numberOfBatches++;
		}
	}
}
