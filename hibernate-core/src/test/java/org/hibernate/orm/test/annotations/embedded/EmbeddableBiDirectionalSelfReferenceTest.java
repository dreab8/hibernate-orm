/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.embedded;

import java.util.List;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hibernate.orm.test.annotations.embedded.EmbeddableBiDirectionalSelfReferenceTest.EntityTest;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				EntityTest.class
		}
)
@SessionFactory(statementInspectorClass = SQLStatementInspector.class)
public class EmbeddableBiDirectionalSelfReferenceTest {

	int expectedJoinCount;


	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		expectedJoinCount = scope.getSessionFactory().getMaximumFetchDepth();

		scope.inTransaction(
				session -> {
					EntityTest entity = new EntityTest( 1 );

					EmbeddableTest embeddable = new EmbeddableTest();
					embeddable.setEntity( entity );
					embeddable.setStringField( "Fab" );
					entity.setEmbeddedAttribute( embeddable );

					session.save( entity );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<EntityTest> results = session.createQuery( "from EntityTest" ).list();
					results.forEach(
							result -> session.delete( result )
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
					EmbeddableTest embeddedAttribute = entity.getEmbeddedAttribute();
					assertThat( embeddedAttribute, notNullValue() );
					assertThat( embeddedAttribute.getStringField(), is( "Fab" ) );
					assertSame( entity, embeddedAttribute.getEntity() );
					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", expectedJoinCount );
				}
		);
	}

	@Test
	public void testGet2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityTest entity = new EntityTest( 2 );

					EmbeddableTest embeddable = new EmbeddableTest();
					EntityTest entity2 = new EntityTest( 3 );
					embeddable.setEntity( entity2 );
					embeddable.setStringField( "Acme" );
					entity.setEmbeddedAttribute( embeddable );


					session.save( entity );
					session.save( entity2 );
				}
		);

		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					EntityTest entity = session.get( EntityTest.class, 2 );
					EmbeddableTest embeddedAttribute = entity.getEmbeddedAttribute();
					assertThat( embeddedAttribute, notNullValue() );
					assertThat( embeddedAttribute.getStringField(), is( "Acme" ) );
					assertThat( embeddedAttribute.getEntity(), notNullValue() );
					assertThat( embeddedAttribute.getEntity().getId(), is( 3 ) );
					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", expectedJoinCount );
				}
		);
	}

	@Entity(name = "EntityTest")
	public static class EntityTest {
		@Id
		private Integer id;

		@Embedded
		private EmbeddableTest embeddedAttribute;

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

		@ManyToOne
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
