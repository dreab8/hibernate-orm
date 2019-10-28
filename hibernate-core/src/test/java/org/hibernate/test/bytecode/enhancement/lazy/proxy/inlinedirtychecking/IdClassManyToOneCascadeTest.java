/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext({ DirtyCheckEnhancementContext.class, NoDirtyCheckEnhancementContext.class })
public class IdClassManyToOneCascadeTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, "true" );
		ssrb.applySetting( AvailableSettings.FORMAT_SQL, "false" );
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		super.configureSessionFactoryBuilder( sfb );
		sfb.applyStatisticsSupport( true );
		sfb.applySecondLevelCacheSupport( false );
		sfb.applyQueryCacheSupport( false );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( SomeEntity.class );
		sources.addAnnotatedClass( ReferencedEntity.class );
	}

	@Test
	@FailureExpected(jiraKey = "HHH-12251")
	public void testMergeCascadesToManyToOne() {

		inTransaction( session -> {
			ReferencedEntity referencedEntity = new ReferencedEntity();
			referencedEntity.setId( 42L );

			SomeEntity someEntity = new SomeEntity();
			someEntity.setId( 23L );
			someEntity.setReferencedEntity( referencedEntity );

			session.merge( someEntity );

			assertTrue( session.contains( referencedEntity ) );
		} );
	}

	@Test
	public void testPersistCascadesToManyToOne() {

		inTransaction( session -> {
			ReferencedEntity referencedEntity = new ReferencedEntity();
			referencedEntity.setId( 42L );

			SomeEntity someEntity = new SomeEntity();
			someEntity.setId( 23L );
			someEntity.setReferencedEntity( referencedEntity );

			session.persist( someEntity );

			assertTrue( session.contains( referencedEntity ) );
		} );
	}

	@Entity(name = "SomeEntity")
	@IdClass(SomeEntityPK.class)
	public static class SomeEntity {

		@Id
		private long id;

		@Id
		@ManyToOne
		private ReferencedEntity referencedEntity;

		public ReferencedEntity getReferencedEntity() {
			return referencedEntity;
		}

		public void setReferencedEntity(ReferencedEntity referencedEntity) {
			this.referencedEntity = referencedEntity;
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}
	}

	public static class SomeEntityPK implements Serializable {

		private Long id;
		private Long referencedEntity;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getReferencedEntity() {
			return referencedEntity;
		}

		public void setReferencedEntity(Long referencedEntity) {
			this.referencedEntity = referencedEntity;
		}
	}

	@Entity(name = "ReferencedEntity")
	public static class ReferencedEntity {

		@Id
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}
