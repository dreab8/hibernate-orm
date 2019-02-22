/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.collections;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.Audited;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * Tests an entity mapping using an entity as a map-key and map-value.
 *
 * This only fails on {@code DefaultAuditStrategy} because the {@code ValidityAuditStrategy} does
 * not make use of the related-id data of the middle table like the default audit strategy.
 *
 * This test verifies both strategies work, but the failure is only applicable for the default strategy.
 *
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11892")
public class EntityMapTest extends EnversEntityManagerFactoryBasedFunctionalTest {

	private A a;
	private B b1;
	private B b2;
	private C c1;
	private C c2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { A.class, B.class, C.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		// todo (6.0) - Maxmimum fetch depth handling seems to be problematic with ValidityAuditStrategy.
		//		At line 92, this test would fail without the following configuration property because the
		//		navigableReferenceStack depth is 7 which exceeds the maximumDepth default of 5.
		//		This lead to the sqlSelections not being resolved and therefore a select-clause that had
		//		absolutely no selectables; thus a SQL syntax exception.
		//

		// todo (6.0) - This should be fixed in ORM and this requirement of maximum-fetch depth removed.
		settings.put( AvailableSettings.MAX_FETCH_DEPTH, 10 );
	}

	@MappedSuperclass
	public static abstract class AbstractEntity {
		@Id
		@GeneratedValue
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			AbstractEntity that = (AbstractEntity) o;

			return id != null ? id.equals( that.id ) : that.id == null;
		}

		@Override
		public int hashCode() {
			return id != null ? id.hashCode() : 0;
		}
	}

	@Entity(name = "A")
	@Audited
	public static class A extends AbstractEntity {
		@ElementCollection
		private Map<B, C> map = new HashMap<>();

		public Map<B, C> getMap() {
			return map;
		}

		public void setMap(Map<B, C> map) {
			this.map = map;
		}
	}

	@Entity(name = "B")
	@Audited
	public static class B extends AbstractEntity {

	}

	@Entity(name = "C")
	@Audited
	public static class C extends AbstractEntity {

	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		// add b/c key-pair to the map and save a entity.
		doInJPA( this::entityManagerFactory, entityManager -> {
			final A a = new A();

			final B b = new B();
			final C c = new C();
			entityManager.persist( b );
			entityManager.persist( c );

			a.getMap().put( b, c );
			entityManager.persist( a );

			this.a = a;
			this.b1 = b;
			this.c1 = c;
		} );

		// add a new b/c key-pair to the map
		doInJPA( this::entityManagerFactory, entityManager -> {
			final A a = entityManager.find( A.class, this.a.getId() );

			final B b = new B();
			final C c = new C();
			entityManager.persist( b );
			entityManager.persist( c );

			a.getMap().put( b, c );
			entityManager.merge( a );

			this.b2 = b;
			this.c2 = c;
		} );

		// Remove b1 from the map
		doInJPA( this::entityManagerFactory, entityManager -> {
			final A a = entityManager.find( A.class, this.a.getId() );
			a.getMap().remove( this.b1 );
			entityManager.merge( a );
		} );
	}

	@DynamicTest
	public void testRevisionHistory() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			assertEquals( Arrays.asList( 1, 2, 3 ), getAuditReader().getRevisions( A.class, a.getId() ) );

			assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( B.class, b1.getId() ) );
			assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( C.class, c1.getId() ) );

			assertEquals( Arrays.asList( 2 ), getAuditReader().getRevisions( B.class, b2.getId() ) );
			assertEquals( Arrays.asList( 2 ), getAuditReader().getRevisions( C.class, c2.getId() ) );
		} );
	}

	@DynamicTest
	public void testRevision1() {
		final A rev1 = getAuditReader().find( A.class, this.a.getId(), 1 );
		assertThat( rev1.getMap().entrySet(), CollectionMatchers.hasSize( 1 ) );
		assertThat( rev1.getMap(), hasEntry( this.b1, this.c1 ) );
	}

	@DynamicTest
	public void testRevision2() {
		final A rev2 = getAuditReader().find( A.class, this.a.getId(), 2 );
		assertThat( rev2.getMap().entrySet(), CollectionMatchers.hasSize( 2 ) );
		assertThat( rev2.getMap(), hasEntry( this.b1, this.c1 ) );
		assertThat( rev2.getMap(), hasEntry( this.b2, this.c2 ) );
	}

	@DynamicTest
	public void testRevision3() {
		final A rev3 = getAuditReader().find( A.class, this.a.getId(), 3 );
		assertThat( rev3.getMap().entrySet(), CollectionMatchers.hasSize( 1 ) );
		assertThat( rev3.getMap(), hasEntry( this.b2, this.c2 ) );
	}

}
