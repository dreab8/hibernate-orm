/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.execution;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.hibernate.orm.test.query.sqm.BaseSqmUnitTest;
import org.hibernate.orm.test.query.sqm.produce.domain.ConstructedLookupListItem;
import org.hibernate.orm.test.query.sqm.produce.domain.NestedCtorLookupListItem;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.TestingUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class DynamicInstantiationSmokeTests extends BaseSqmUnitTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				EntityOfBasics.class,
		};
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@BeforeEach
	public void createData() {
		inTransaction(
				session -> {
					final EntityOfBasics entity = new EntityOfBasics();
					entity.setId( 1 );
					entity.setTheString( "Some name" );
					entity.setGender( EntityOfBasics.Gender.MALE );
					entity.setTheInt( -1 );
					session.save( entity );
				}
		);
	}

	@AfterEach
	public void dropData() {
		inTransaction(
				session -> session.doWork(
						connection -> {
							final Statement statement = connection.createStatement();
							try {
								statement.execute( "delete from EntityOfBasics" );
							}
							finally {
								try {
									statement.close();
								}
								catch (SQLException ignore) {
								}
							}
						}
				)
		);
	}

	@Test
	public void testSimpleDynamicInstantiation() {
		inTransaction(
				session -> {
					final List results = session.createQuery(
							"select new org.hibernate.orm.test.query.sqm.produce.domain.ConstructedLookupListItem( e.id, e.theString ) from EntityOfBasics e"
					).list();

					assertThat( results, hasSize(1) );
					final ConstructedLookupListItem item = (ConstructedLookupListItem) results.get( 0 );
					assertThat( item.getId(), is( 1 ) );
					assertThat( item.getDisplayValue(), is( "Some name" ) );
				}
		);
	}

	@Test
	public void testSimpleInjectedInstantiation() {
		inTransaction(
				session -> session.createQuery(
						"select new org.hibernate.orm.test.query.sqm.produce.domain.InjectedLookupListItem( e.id as id, e.theString as displayValue ) from EntityOfBasics e"
				).list()
		);
	}

	@Test
	public void testMultipleDynamicInstantiations() {
		inTransaction(
				session -> {
					final List results = session.createQuery(
							"select new org.hibernate.orm.test.query.sqm.produce.domain.ConstructedLookupListItem( e.id, e.theString ), " +
									"new org.hibernate.orm.test.query.sqm.produce.domain.ConstructedLookupListItem( e.id, e.theString ) " +
									"from EntityOfBasics e"
					).list();

					assertThat( results, hasSize(1) );

					final Object[] row = (Object[]) results.get( 0 );

					{
						// first item
						final ConstructedLookupListItem item = (ConstructedLookupListItem) row[0];
						assertThat( item.getId(), is( 1 ) );
						assertThat( item.getDisplayValue(), is( "Some name" ) );
					}

					{
						// second item
						final ConstructedLookupListItem item = (ConstructedLookupListItem) row[1];
						assertThat( item.getId(), is( 1 ) );
						assertThat( item.getDisplayValue(), is( "Some name" ) );
					}
				}
		);
	}

	@Test
	public void testMixedAttributeAndDynamicInstantiation() {
		inTransaction(
				session -> {
					final List results = session.createQuery(
							"select new org.hibernate.orm.test.query.sqm.produce.domain.ConstructedLookupListItem( e.id, e.theString ), e.theInteger from EntityOfBasics e"
					).list();

					assertThat( results, hasSize(1) );
					final Object[] result = TestingUtil.cast(
							results.get( 0 ),
							Object[].class
					);

					final ConstructedLookupListItem item = (ConstructedLookupListItem) result[0];
					assertThat( item.getId(), is( 1 ) );
					assertThat( item.getDisplayValue(), is( "Some name" ) );

					assertThat( result[1], is( nullValue() ) );
				}
		);
	}

	@Test
	public void testSimpleDynamicListInstantiation() {
		inTransaction(
				session -> {
					final List results = session.createQuery(
							"select new list( e.id, e.theString ) from EntityOfBasics e"
					).list();

					assertThat( results, hasSize( 1 ) );

					final List row = TestingUtil.cast( results.get( 0 ), List.class );
					assertThat( row, hasSize( 2 ) );
					assertThat( row.get( 0 ), is( 1 ) );
					assertThat( row.get( 1 ), is( "Some name" ) );
				}
		);
	}

	@Test
	public void testSimpleDynamicMapInstantiation() {
		inTransaction(
				session -> {
					final List results = session.createQuery(
							"select new map( e.id as id, e.theString as ts ) from EntityOfBasics e"
					).list();

					assertThat( results, hasSize( 1 ) );

					final Map row = TestingUtil.cast( results.get( 0 ), Map.class );
					assertThat( row.get( "id" ), is( 1 ) );
					assertThat( row.get( "ts" ), is( "Some name" ) );
				}
		);
	}

	@Test
	public void testNestedDynamicInstantiation() {
		inTransaction(
				session -> {
					final List results = session.createQuery(
							"select new org.hibernate.orm.test.query.sqm.produce.domain.NestedCtorLookupListItem(" +
									" e.id, " +
									" e.theString, " +
									" new org.hibernate.orm.test.query.sqm.produce.domain.ConstructedLookupListItem( e.id, e.theString )" +
									" ) " +
									" from EntityOfBasics e"
					).list();

					assertThat( results, hasSize( 1 ) );

					final NestedCtorLookupListItem item = TestingUtil.cast(
							results.get( 0 ),
							NestedCtorLookupListItem.class
					);

					assertThat( item.getId(), is( 1 ) );
					assertThat( item.getDisplayValue(), is( "Some name" ) );
					assertThat( item.getNested().getId(), is( 1 ) );
					assertThat( item.getNested().getDisplayValue(), is( "Some name" ) );
				}
		);
	}

	@Test
	public void testNestedMixedDynamicInstantiation() {
		inTransaction(
				session -> {
					final List results = session.createQuery(
							"select new org.hibernate.orm.test.query.sqm.produce.domain.NestedCtorLookupListItem(" +
									" e.id, " +
									" e.theString, " +
									" new org.hibernate.orm.test.query.sqm.produce.domain.InjectedLookupListItem( e.id as id, e.theString as displayValue )" +
									" ) " +
									" from EntityOfBasics e"
					).list();

					assertThat( results, hasSize( 1 ) );

					final NestedCtorLookupListItem item = TestingUtil.cast(
							results.get( 0 ),
							NestedCtorLookupListItem.class
					);

					assertThat( item.getId(), is( 1 ) );
					assertThat( item.getDisplayValue(), is( "Some name" ) );
					assertThat( item.getNested().getId(), is( 1 ) );
					assertThat( item.getNested().getDisplayValue(), is( "Some name" ) );
				}
		);
	}
}
