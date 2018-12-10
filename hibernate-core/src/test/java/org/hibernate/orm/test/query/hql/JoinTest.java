/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.hql;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hamcrest.CoreMatchers;
import org.hibernate.boot.MetadataSources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Chris Cranford
 */
public class JoinTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );

		metadataSources.addAnnotatedClass( AuditEntity.class );
		metadataSources.addAnnotatedClass( AuditRevisionEntity.class );
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@BeforeEach
	public void setUp() {
		sessionFactoryScope().inTransaction(
				session -> {
					final AuditRevisionEntity revEntity = new AuditRevisionEntity();
					revEntity.setId( 1 );
					revEntity.setTimestamp( Instant.now().getEpochSecond() );

					final AuditEntityId entityId = new AuditEntityId();
					entityId.setId( 1 );
					entityId.setRev( revEntity );

					final AuditEntity entity = new AuditEntity();
					entity.setOriginalId( entityId );
					entity.setData( "test" );

					session.save( revEntity );
					session.save( entity );
				}
		);

		sessionFactoryScope().inTransaction(
				session -> {
					final AuditRevisionEntity revEntity = new AuditRevisionEntity();
					revEntity.setId( 2 );
					revEntity.setTimestamp( Instant.now().getEpochSecond() );

					final AuditEntityId entityId = new AuditEntityId();
					entityId.setId( 2 );
					entityId.setRev( revEntity );

					final AuditEntity entity = new AuditEntity();
					entity.setOriginalId( entityId );
					entity.setData( "test" );

					session.save( revEntity );
					session.save( entity );
				}
		);
	}

	@AfterEach
	public void tearDown() {
		sessionFactoryScope().inTransaction(
				session -> {
					session.createQuery( "FROM AuditEntity", AuditEntity.class ).list()
							.forEach( e -> {
								final AuditRevisionEntity revEntity = e.getOriginalId().getRev();
								session.delete( e );
								session.delete( revEntity );
							} );
				}
		);
	}


	@Test
	public void testImplicitJoins() {
		StringBuilder hql = new StringBuilder();
		hql.append( "select e.originalId.id " );
		hql.append( "from " + AuditEntity.class.getName() + " e " );
		hql.append( "inner join " + AuditEntity.class.getName() + " s " );
		hql.append( "on e.originalId.id = s.originalId.id " );
		hql.append( "and e.originalId.rev.id = s.originalId.rev.id " );
		hql.append( "where e.originalId.id = :id" );

		sessionFactoryScope().inTransaction(
				session -> {
					List results = session.createQuery( hql.toString() ).setParameter( "id", 1 ).getResultList();
					assertThat( results.size(), CoreMatchers.is( 1 ) );
				}
		);
	}

	// Represents the actual audited entity
	@Entity(name = "AuditEntity")
	public static class AuditEntity {
		@EmbeddedId
		private AuditEntityId originalId;
		private String data;

		public AuditEntityId getOriginalId() {
			return originalId;
		}

		public void setOriginalId(AuditEntityId originalId) {
			this.originalId = originalId;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}

	// Represents the audit-entity composite-id
	@Embeddable
	public static class AuditEntityId implements Serializable {
		private Integer id;
		@ManyToOne
		private AuditRevisionEntity rev;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public AuditRevisionEntity getRev() {
			return rev;
		}

		public void setRev(AuditRevisionEntity rev) {
			this.rev = rev;
		}
	}

	// Represents the REVINFO revision entity.
	@Entity(name = "AuditRevisionEntity")
	public static class AuditRevisionEntity {
		@Id
		private Integer id;
		private Long timestamp;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Long getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(Long timestamp) {
			this.timestamp = timestamp;
		}
	}
}
