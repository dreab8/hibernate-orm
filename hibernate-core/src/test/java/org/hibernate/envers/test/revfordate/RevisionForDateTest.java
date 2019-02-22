/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.revfordate;

import java.util.Date;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.exception.RevisionDoesNotExistException;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;

import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class RevisionForDateTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private long timestamp1;
	private long timestamp2;
	private long timestamp3;
	private long timestamp4;
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() throws Exception {
		inTransactions(
				// Revision 1
				entityManager -> {
					timestamp1 = getTimestamp();
					StrTestEntity entity = new StrTestEntity( "x" );
					entityManager.persist( entity );

					this.id = entity.getId();
				},

				// Revision 2
				entityManager -> {
					timestamp2 = getTimestamp();
					StrTestEntity entity = entityManager.find( StrTestEntity.class, id );
					entity.setStr( "y" );
				},

				// Revision 3
				entityManager -> {
					timestamp3 = getTimestamp();
					StrTestEntity entity = entityManager.find( StrTestEntity.class, id );
					entity.setStr( "z" );
				},

				// No revision
				entityManager -> {
					timestamp4 = getTimestamp();
				}
		);
	}

	@DynamicTest(expected = RevisionDoesNotExistException.class)
	public void testNoRevisionsExitPriorToTimestamp1() {
		getAuditReader().getRevisionNumberForDate( new Date( timestamp1 ) );
	}
	
	@DynamicTest(expected = RevisionDoesNotExistException.class)
	public void testNoRevisionsExistForTimestamp1UsingFind() {
		getAuditReader().find( StrTestEntity.class, id, new Date( timestamp1 ) );
	}

	@DynamicTest
	public void testTimestamps() {
		assertThat( getAuditReader().getRevisionNumberForDate( new Date( timestamp2 ) ).intValue(), equalTo( 1 ) );
		assertThat( getAuditReader().getRevisionNumberForDate( new Date( timestamp3 ) ).intValue(), equalTo( 2 ) );
		assertThat( getAuditReader().getRevisionNumberForDate( new Date( timestamp4 ) ).intValue(), equalTo( 3 ) );
	}
	
	@DynamicTest
	public void testEntitiesForTimestamps() {
		assertThat( getAuditReader().find( StrTestEntity.class, id, new Date( timestamp2 ) ).getStr(), equalTo( "x" ) );
		assertThat( getAuditReader().find( StrTestEntity.class, id, new Date( timestamp3 ) ).getStr(), equalTo( "y" ) );
		assertThat( getAuditReader().find( StrTestEntity.class, id, new Date( timestamp4 ) ).getStr(), equalTo( "z" ) );
	}

	@DynamicTest
	public void testDatesForRevisions() {
		final AuditReader reader = getAuditReader();
		assertThat( reader.getRevisionNumberForDate( reader.getRevisionDate( 1 ) ).intValue(), equalTo( 1 ) );
		assertThat( reader.getRevisionNumberForDate( reader.getRevisionDate( 2 ) ).intValue(), equalTo( 2 ) );
		assertThat( reader.getRevisionNumberForDate( reader.getRevisionDate( 3 ) ).intValue(), equalTo( 3 ) );
	}

	@DynamicTest
	public void testRevisionsForDates() {
		final AuditReader reader = getAuditReader();

		final Number revisionForTimestamp2 = reader.getRevisionNumberForDate( new Date( timestamp2 ) );
		assertThat( reader.getRevisionDate( revisionForTimestamp2 ).getTime(), lessThanOrEqualTo( timestamp2 ) );
		assertThat( reader.getRevisionDate( revisionForTimestamp2.intValue() + 1 ).getTime(), greaterThan( timestamp2 ) );

		final Number revisionForTimestamp3 = reader.getRevisionNumberForDate( new Date( timestamp3 ) );
		assertThat( reader.getRevisionDate( revisionForTimestamp3 ).getTime(), lessThanOrEqualTo( timestamp3 ) );
		assertThat( reader.getRevisionDate( revisionForTimestamp3.intValue() + 1 ).getTime(), greaterThan( timestamp3 ) );

		final Number revisionForTimestamp4 = reader.getRevisionNumberForDate( new Date( timestamp4 ) );
		assertThat( reader.getRevisionDate( revisionForTimestamp4 ).getTime(), lessThanOrEqualTo( timestamp4 ) );
	}

	private long getTimestamp() {
		long timestamp = System.currentTimeMillis();
		try {
			// This is used to guarantee that there is a gap between the timestamps assigned
			// to each unique revision to guarantee that the test queries return expected hits.
			Thread.sleep( 100 );
		}
		catch ( InterruptedException e ) {
			throw new RuntimeException( "Failed to wait for defined time slice", e );
		}
		return timestamp;
	}
}
