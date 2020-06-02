/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.embedded;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				EmbeddableBidirectionalCircularityTest.EntityTest.class,
				EmbeddableBidirectionalCircularityTest.EntityTest2.class
		}
)
@SessionFactory(statementInspectorClass = SQLStatementInspector.class)
public class EmbeddableBidirectionalCircularityTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityTest entity = new EntityTest( 1 );
					EntityTest2 entityTest2 = new EntityTest2( 2 );

					EmbeddableTest embeddable = new EmbeddableTest();
					embeddable.setEntity( entity );
					embeddable.setStringField( "Fab" );

					entityTest2.setEmbeddedAttribute( embeddable );

					entity.setEntity2( entityTest2 );
					session.save( entity );
					session.save( entityTest2 );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from EntityTest", EntityTest.class ).list().forEach(
							entityTest -> {
								session.delete( entityTest );
							}
					);

					session.createQuery( "from EntityTest2", EntityTest2.class ).list().forEach(
							entityTest -> {
								session.delete( entityTest );
							}
					);
				}
		);
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					EntityTest entity = session.get( EntityTest.class, 1 );
					EntityTest2 entity2 = entity.getEntity2();
					assertSame( entity2.getEmbeddedAttribute().getEntity(), entity );
					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 2 );
				}
		);
	}

	@Entity(name = "EntityTest")
	public static class EntityTest {
		@Id
		private Integer id;

		@ManyToOne
		private EntityTest2 entity2;

		public EntityTest() {
		}

		public EntityTest(int id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EntityTest2 getEntity2() {
			return entity2;
		}

		public void setEntity2(EntityTest2 entity2) {
			this.entity2 = entity2;
		}
	}

	@Entity(name = "EntityTest2")
	public static class EntityTest2 {
		@Id
		private Integer id;

		@Embedded
		private EmbeddableTest embeddedAttribute;

		public EntityTest2() {
		}

		public EntityTest2(int id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EmbeddableTest getEmbeddedAttribute() {
			return embeddedAttribute;
		}

		public void setEmbeddedAttribute(EmbeddableTest embeddedAttribute) {
			this.embeddedAttribute = embeddedAttribute;
		}
	}

	@Embeddable
	public static class EmbeddableTest {
		private String stringField;

		@OneToOne(mappedBy = "entity2")
		private EntityTest entity;

		public String getStringField() {
			return stringField;
		}

		public void setStringField(String stringField) {
			this.stringField = stringField;
		}

		public EntityTest getEntity() {
			return entity;
		}

		public void setEntity(EntityTest entity) {
			this.entity = entity;
		}
	}
}
