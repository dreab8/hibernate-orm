/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.crud.inheritance;

import java.math.BigDecimal;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * @author Andrea Boriero
 */
public class TablePerClassTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { DebitAccount.class, CreditAccount.class };
	}

	@Test
	public void testPersistDebitAccount() {
		DebitAccount debitAccount = new DebitAccount();
		inTransaction(
				session -> {
					debitAccount.setOwner( "Fab" );
					session.save( debitAccount );
				}
		);

		inTransaction(
				session -> {
					DebitAccount account = session.get(
							DebitAccount.class,
							debitAccount.getId()
					);
					assertThat( account, notNullValue() );
				}
		);
	}

	@Test
	public void testPersistAccounts() {
		DebitAccount debitAccount = new DebitAccount();
		inTransaction(
				session -> {
					debitAccount.setOwner( "Fab" );
					debitAccount.setOverdraftFee( new BigDecimal( 10 ) );
					session.save( debitAccount );
				}
		);

		CreditAccount creditAccount = new CreditAccount();
		inTransaction(
				session -> {
					creditAccount.setOwner( "And" );
					session.save( creditAccount );
				}
		);

		inTransaction(
				session -> {
					MappedSuperclassTest.DebitAccount account = session.get(
							MappedSuperclassTest.DebitAccount.class,
							debitAccount.getId()
					);
					assertThat( account, notNullValue() );
				}
		);

		inTransaction(
				session -> {
					MappedSuperclassTest.CreditAccount account = session.get(
							MappedSuperclassTest.CreditAccount.class,
							creditAccount.getId()
					);
					assertThat( account, notNullValue() );
				}
		);
	}

	@Entity(name = "Account")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static class Account {

		@Id
		@GeneratedValue
		private Long id;

		public Long getId() {
			return id;
		}

		private String owner;

		private BigDecimal balance;

		private BigDecimal interestRate;

		public String getOwner() {
			return owner;
		}

		public void setOwner(String owner) {
			this.owner = owner;
		}

		public BigDecimal getBalance() {
			return balance;
		}

		public void setBalance(BigDecimal balance) {
			this.balance = balance;
		}

		public BigDecimal getInterestRate() {
			return interestRate;
		}

		public void setInterestRate(BigDecimal interestRate) {
			this.interestRate = interestRate;
		}
	}

	@Entity(name = "DebitAccount")
	public static class DebitAccount extends Account {

		private BigDecimal overdraftFee;


		public BigDecimal getOverdraftFee() {
			return overdraftFee;
		}

		public void setOverdraftFee(BigDecimal overdraftFee) {
			this.overdraftFee = overdraftFee;
		}
	}

	@Entity(name = "CreditAccount")
	public static class CreditAccount extends Account {

		private BigDecimal creditLimit;

		public BigDecimal getCreditLimit() {
			return creditLimit;
		}

		public void setCreditLimit(BigDecimal creditLimit) {
			this.creditLimit = creditLimit;
		}
	}
}
