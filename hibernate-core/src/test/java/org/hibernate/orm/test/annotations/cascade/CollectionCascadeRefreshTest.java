/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.Hibernate;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				CollectionCascadeRefreshTest.Parent.class,
				CollectionCascadeRefreshTest.EntityA.class,
				CollectionCascadeRefreshTest.EntityB.class,
		}
)
@SessionFactory(statementInspectorClass = SQLStatementInspector.class)
public class CollectionCascadeRefreshTest {

	private static final Long PARENT_ID = 1L;
	private static final Long ENTITY_A_1_ID = 1L;
	private static final String ENTITY_A_1_NAME = "A 1";
	private static final Long ENTITY_A_2_ID = 2L;
	private static final String ENTITY_A_2_NAME = "A 2";
	private static final Long ENTITY_A_3_ID = 3L;
	private static final String ENTITY_A_3_NAME = "A 3";
	private static final Long ENTITY_B_ID = 4L;
	private static final String ENTITY_B_NAME = "B 1";


	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityB entityB = new EntityB( ENTITY_B_ID, ENTITY_B_NAME );

					EntityA entityA1 = new EntityA( ENTITY_A_1_ID, ENTITY_A_1_NAME, entityB );
					EntityA entityA2 = new EntityA( ENTITY_A_2_ID, ENTITY_A_2_NAME );
					EntityA entityA3 = new EntityA( ENTITY_A_3_ID, ENTITY_A_3_NAME );

					Parent parent = new Parent( PARENT_ID, "parent1" );
					parent.addEntityA( entityA1 );
					parent.addEntityA( entityA2 );
					parent.addEntityA( entityA3 );

					session.persist( parent );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from EntityA" ).executeUpdate();
					session.createMutationQuery( "delete from EntityB" ).executeUpdate();
					session.createMutationQuery( "delete from Parent" ).executeUpdate();
				}
		);
	}

	@Test
	public void testCascadeRefreshWithUninitializedCollection(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		scope.inTransaction(
				session -> {
					EntityA child1 = session.find( EntityA.class, ENTITY_A_1_ID );
					child1.setName( "new A 1" );

					EntityB entityB = child1.getEntityB();
					entityB.setName( "new B 1" );

					Parent parent = session.find( Parent.class, PARENT_ID );
					assertThat( Hibernate.isInitialized( parent.getEntityAS() ) ).isFalse();
					statementInspector.clear();

					session.refresh( parent );

					assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );

					assertThat( Hibernate.isInitialized( parent.getEntityAS() ) )
							.as( "refresh should have initialized the children collection " )
							.isTrue();
					parent.getEntityAS().forEach( child -> {
						if ( child.getId() == ENTITY_A_1_ID ) {
							assertThat( child.getName() ).isEqualTo( ENTITY_A_1_NAME );
							assertThat( child.getEntityB().getName() ).isEqualTo( ENTITY_B_NAME );
						}
					} );
				}
		);
	}

	@Test
	public void testCascadeRefreshWithInitializedCollection(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		scope.inTransaction(
				session -> {
					EntityA child1 = session.find( EntityA.class, ENTITY_A_1_ID );
					child1.setName( "new A 1" );

					EntityB entityB = child1.getEntityB();
					entityB.setName( "new B 1" );

					Parent parent = session.find( Parent.class, PARENT_ID );
					Hibernate.initialize( parent.getEntityAS());
					assertThat( Hibernate.isInitialized( parent.getEntityAS() ) ).isTrue();
					statementInspector.clear();

					session.refresh( parent );

					assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );

					assertThat( Hibernate.isInitialized( parent.getEntityAS() ) )
							.as( "refresh should have initialized the children collection " )
							.isTrue();

					parent.getEntityAS().forEach( child -> {
						if ( child.getId() == ENTITY_A_1_ID ) {
							assertThat( child.getName() ).isEqualTo( ENTITY_A_1_NAME );
							assertThat( child.getEntityB().getName() ).isEqualTo( ENTITY_B_NAME );
						}
					} );
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private Long id;

		private String name;

		@OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, mappedBy = "parent")
		List<EntityA> entityAS = new ArrayList<>();

		public Parent() {
		}

		public Parent(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public List<EntityA> getEntityAS() {
			return entityAS;
		}

		public void addEntityA(EntityA entityA) {
			entityAS.add( entityA );
			entityA.parent = this;
		}

	}

	@Entity(name = "EntityA")
	public static class EntityA {
		@Id
		private Long id;

		private String name;

		@ManyToOne
		private Parent parent;

		@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
		private EntityB entityB;

		public EntityA() {
		}

		public EntityA(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public EntityA(Long id, String name, EntityB entityB) {
			this.id = id;
			this.name = name;
			this.entityB = entityB;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		public EntityB getEntityB() {
			return entityB;
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB {
		@Id
		private Long id;

		private String name;


		public EntityB() {
		}

		public EntityB(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
