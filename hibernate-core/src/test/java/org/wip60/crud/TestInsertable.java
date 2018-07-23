/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.wip60.crud;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class TestInsertable extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Price.class };
	}

	@Test
	public void testIt() {
		Price price = new Price( 1, "first", 12 );
		doInHibernate(
				this::sessionFactory,
				session -> {
					session.save( price );
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					session.update( price );
					price.setInitalPrice( 20 );
					price.setDescription( "first item" );
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					assertThat( session.get( Price.class, price.getId() ).getInitalPrice(), is( 12 ) );
					assertThat( session.get( Price.class, price.getId() ).getDescription(), is( "first item" ) );
				}
		);
	}

	@Entity
	public static class Price {
		@Id
		private Integer id;

		private String description;

		public Price() {
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}

		public Price(Integer id, String description, Integer initalPrice) {
			this.id = id;
			this.description = description;
			this.initalPrice = initalPrice;
		}

		@Column(updatable = false)
		private Integer initalPrice;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Integer getInitalPrice() {
			return initalPrice;
		}

		public void setInitalPrice(Integer initalPrice) {
			this.initalPrice = initalPrice;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}
}

