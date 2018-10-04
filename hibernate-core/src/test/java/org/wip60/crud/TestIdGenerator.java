/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.wip60.crud;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Andrea Boriero
 */
public class TestIdGenerator extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SimpleEntity.class };
	}

	@Test
	public void testInsert() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					SimpleEntity entity = new SimpleEntity( "Fab" );
					session.save( entity );
				}
		);

	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity {
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Id
		Long id;

		String StringProperty;

		public SimpleEntity() {
		}

		public SimpleEntity(String stringProperty) {
			StringProperty = stringProperty;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getStringProperty() {
			return StringProperty;
		}

		public void setStringProperty(String stringProperty) {
			StringProperty = stringProperty;
		}
	}
}
