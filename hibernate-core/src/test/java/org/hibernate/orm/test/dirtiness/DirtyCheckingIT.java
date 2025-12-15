/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dirtiness;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;


@DomainModel(
		annotatedClasses = {
				DirtyCheckingIT.Fruit.class
		}
)
@SessionFactory(
		useCollectingStatementInspector = true
)
public class DirtyCheckingIT {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.persist( new Fruit().setId( 5 ).setName( "Apple" ) )
		);
	}


	@Test
	public void testDirtyCheck(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector)scope.getStatementInspector();

		scope.inTransaction(
				session -> {
					Fruit fruit = session.find( Fruit.class, 5 );
					statementInspector.clear();
					assertThat( fruit ).hasFieldOrPropertyWithValue( "name", "Apple" );
				}
		);
		assertThat(statementInspector.getSqlQueries()).isEmpty();
	}

	@Entity(name= "Fruit")
	public static class Fruit {
		@Id
		private int id;

		// Dirty checking should not be confused by this initialization.
		private String name = "Banana";

		public int getId() {
			return id;
		}

		public Fruit setId(final int id) {
			this.id = id;
			return this;
		}

		public String getName() {
			return name;
		}

		public Fruit setName(final String name) {
			this.name = name;
			return this;
		}
	}
}
