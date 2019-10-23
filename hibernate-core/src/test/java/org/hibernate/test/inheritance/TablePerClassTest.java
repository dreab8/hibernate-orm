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
package org.hibernate.test.inheritance;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Andrea Boriero
 */
public class TablePerClassTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Customer.class,
				DomesticCustomer.class,
				ForeignCustomer.class
		};
	}

	@Test
	public void testIt() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			final List<Customer> results = entityManager.createQuery(
					"select c from Customer c",
					Customer.class
			).getResultList();

			assertThat( results.size(), is( 2 ) );

			for ( Customer result : results ) {
				if ( result.getId() == 1 ) {
					assertThat( result, instanceOf( DomesticCustomer.class ) );
					final DomesticCustomer customer = (DomesticCustomer) result;
					assertThat( customer.getName(), is( "domestic" ) );
					assertThat( ( customer ).getTaxId(), is( "123" ) );
				}
				else {
					assertThat( result.getId(), is( 2 ) );
					final ForeignCustomer customer = (ForeignCustomer) result;
					assertThat( customer.getName(), is( "foreign" ) );
					assertThat( ( customer ).getVat(), is( "987" ) );
				}
			}
		} );
	}

	@Before
	public void createTestData() {
		doInJPA( this::entityManagerFactory, entityManager -> {
					 entityManager.persist( new DomesticCustomer( 1, "domestic", "123" ) );
					 entityManager.persist( new ForeignCustomer( 2, "foreign", "987" ) );
				 }
		);
	}

	@After
	public void cleanupTestData() {
		doInJPA( this::entityManagerFactory, entityManager -> {
					 entityManager.createQuery( "from DomesticCustomer", DomesticCustomer.class ).getResultList().forEach(
							 cust -> entityManager.remove( cust )
					 );
					 entityManager.createQuery( "from ForeignCustomer", ForeignCustomer.class ).getResultList().forEach(
							 cust -> entityManager.remove( cust )
					 );
				 }
		);
	}

	@Entity(name = "Customer")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static class Customer {
		private Integer id;
		private String name;

		public Customer() {
		}

		public Customer(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
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
	}

	@Entity(name = "DomesticCustomer")
	public static class DomesticCustomer extends Customer {
		private String taxId;

		public DomesticCustomer() {
		}

		public DomesticCustomer(Integer id, String name, String taxId) {
			super( id, name );
			this.taxId = taxId;
		}

		public String getTaxId() {
			return taxId;
		}

		public void setTaxId(String taxId) {
			this.taxId = taxId;
		}
	}

	@Entity(name = "ForeignCustomer")
	public static class ForeignCustomer extends Customer {
		private String vat;

		public ForeignCustomer() {
		}

		public ForeignCustomer(Integer id, String name, String vat) {
			super( id, name );
			this.vat = vat;
		}

		public String getVat() {
			return vat;
		}

		public void setVat(String vat) {
			this.vat = vat;
		}
	}
}
