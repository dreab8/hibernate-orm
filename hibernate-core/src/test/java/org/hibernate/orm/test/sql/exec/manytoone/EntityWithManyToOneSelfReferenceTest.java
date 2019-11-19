/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.sql.exec.manytoone;

import java.util.List;

import org.hibernate.testing.orm.domain.gambit.EntityWithManyToOneSelfReference;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				EntityWithManyToOneSelfReference.class
		}
)
@ServiceRegistry
@SessionFactory(generateStatistics = true)
public class EntityWithManyToOneSelfReferenceTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		final EntityWithManyToOneSelfReference entity1 = new EntityWithManyToOneSelfReference(
				1,
				"first",
				Integer.MAX_VALUE
		);

		final EntityWithManyToOneSelfReference entity2 = new EntityWithManyToOneSelfReference(
				2,
				"second",
				Integer.MAX_VALUE,
				entity1
		);

		scope.inTransaction( session -> {
			session.save( entity1 );
			session.save( entity2 );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from EntityWithManyToOneSelfReference" ).executeUpdate();
				}
		);
	}

	@Test
	public void testHqlSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityWithManyToOneSelfReference queryResult = session.createQuery(
							"select e from EntityWithManyToOneSelfReference e where e.other.name = 'first'",
							EntityWithManyToOneSelfReference.class
					).uniqueResult();
					assertThat( queryResult.getName(), equalTo( "second" ) );
				}
		);

	}

	@Test
	@FailureExpected
	public void testGetEntity(SessionFactoryScope scope) {


		scope.inTransaction(
				session -> {
					final EntityWithManyToOneSelfReference loaded = session.get(
							EntityWithManyToOneSelfReference.class,
							2
					);
					assert loaded != null;
					assertThat( loaded.getName(), equalTo( "second" ) );
					assert loaded.getOther() != null;
					assertThat( loaded.getOther().getName(), equalTo( "first" ) );
				}
		);

		scope.inTransaction(
				session -> {
					final EntityWithManyToOneSelfReference loaded = session.get(
							EntityWithManyToOneSelfReference.class,
							1
					);
					assert loaded != null;
					assertThat( loaded.getName(), equalTo( "first" ) );
					assertThat( loaded.getOther(), nullValue() );
				}
		);

		scope.inTransaction(
				session -> {
					final List<EntityWithManyToOneSelfReference> list = session.byMultipleIds(
							EntityWithManyToOneSelfReference.class )
							.multiLoad( 1, 3 );
					assert list.size() == 1;
					final EntityWithManyToOneSelfReference loaded = list.get( 0 );
					assert loaded != null;
					assertThat( loaded.getName(), equalTo( "first" ) );
				}
		);

		scope.inTransaction(
				session -> {
					final List<EntityWithManyToOneSelfReference> list = session.byMultipleIds(
							EntityWithManyToOneSelfReference.class )
							.multiLoad( 2, 3 );
					assert list.size() == 1;
					final EntityWithManyToOneSelfReference loaded = list.get( 0 );
					assert loaded != null;
					assertThat( loaded.getName(), equalTo( "second" ) );
					assert loaded.getOther() != null;
					assertThat( loaded.getOther().getName(), equalTo( "first" ) );
				}
		);

		// todo (6.0) : the restriction here uses the wrong table alias...
		scope.inTransaction(
				session -> {
					final String value = session.createQuery(
							"select e.name from EntityWithManyToOneSelfReference e where e.other.name = 'first'",
							String.class
					).uniqueResult();
					assertThat( value, equalTo( "second" ) );
				}
		);


	}
}
