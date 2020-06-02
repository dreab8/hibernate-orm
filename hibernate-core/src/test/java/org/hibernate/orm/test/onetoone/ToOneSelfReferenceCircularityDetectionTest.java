package org.hibernate.orm.test.onetoone;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Andrea Boriero
 */
@DomainModel(annotatedClasses = {
		ToOneSelfReferenceCircularityDetectionTest.EntityTest.class
})
@SessionFactory(statementInspectorClass = SQLStatementInspector.class)
public class ToOneSelfReferenceCircularityDetectionTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityTest entity = new EntityTest( 1, "e1" );
					EntityTest entity2 = new EntityTest( 2, "e2" );
					EntityTest entity3 = new EntityTest( 3, "e3" );

					entity2.setEntity( entity3 );
					entity.setEntity( entity2 );
					session.save( entity3 );
					session.save( entity2 );
					session.save( entity );
				}
		);
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
//				/*
//				select
//					e1_0.id,
//					e2_0.id,
//					e2_0.name,
//					e1_0.name
//				from
//					EntityTest as e1_0
//				left outer join
//					EntityTest as e2_0
//						on e1_0.entity_id = e2_0.id
//				where
//					e1_0.id = ?
//				 */
				session -> {
					final EntityTest entity = session.get( EntityTest.class, 1 );
					assertThat( entity.getName(), is( "e1" ) );

					final EntityTest entity2 = entity.getEntity();
					assertThat( entity2, notNullValue() );
					assertThat( entity2.getName(), is( "e2" ) );

					final EntityTest entity3 = entity2.getEntity();
					assertThat( entity3, notNullValue() );
					assertThat( entity3.getName(), is( "e3" ) );

					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", scope.getSessionFactory().getMaximumFetchDepth() );
				}
		);
	}

	@Entity(name = "EntityTest")
	public static class EntityTest {
		@Id
		private Integer id;

		private String name;

		@ManyToOne
		private EntityTest entity;

		public EntityTest() {
		}

		public EntityTest(int id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public EntityTest getEntity() {
			return entity;
		}

		public void setEntity(EntityTest entity) {
			this.entity = entity;
		}
	}
}
