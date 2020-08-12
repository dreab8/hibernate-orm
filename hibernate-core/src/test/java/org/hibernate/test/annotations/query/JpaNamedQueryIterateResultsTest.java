/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.FlushModeType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.FlushMode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Andrea Boriero
 */
public class JpaNamedQueryIterateResultsTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected void addMappings(Map settings) {
		settings.put( AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, "500" );
		settings.put( AvailableSettings.MAX_FETCH_DEPTH, "1" );
		settings.put( AvailableSettings.BATCH_FETCH_STYLE, "PADDED" );
		settings.put( AvailableSettings.STATEMENT_FETCH_SIZE, "1000" );
		settings.put( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Address.class, User.class };
	}

	@Before
	public void setUp() {
		doInJPA( this::entityManagerFactory, entityManager -> {
					 for ( int i = 0; i < 10; i++ ) {
						 User user = new User();
						 user.setName( "User " + i );
						 user.setAddress( new ArrayList<Address>() );
						 for ( int i2 = 0; i2 < 4; i2++ ) {
							 Address address = new Address();
							 address.setAddress( "Address " + i2 );
							 address.setUser( user );
							 user.getAddress().add( address );
						 }
						 entityManager.persist( user );
					 }
				 }
		);
	}

	@After
	public void tearDown() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			entityManager.createQuery( "delete from Address" ).executeUpdate();
			entityManager.createQuery( "delete from User" ).executeUpdate();
				 }
		);
	}

	@Test
	public void testInClauseIsUsedWhileIteratingResults() {
		StatisticsImplementor statistics = entityManagerFactory().getStatistics();
		statistics.clear();
		doInJPA( this::entityManagerFactory, entityManager -> {
					 List<User> users = entityManager.createNamedQuery( User.FIND_ALL, User.class ).getResultList();

					 List<User> nativeQueryUsers = entityManager.createNativeQuery( "select u.* from USERS u", User.class )
							 .setFlushMode( FlushModeType.COMMIT )
							 .getResultList();
					 statistics.clear();
					 for ( User user : nativeQueryUsers ) {
						 user.getAddress().size();
					 }
					 assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
					 assertThat( entityManager.getFlushMode().name(), is( FlushMode.AUTO.name() ) );
				 }
		);
	}


	@Test
	public void testInClauseIsUsedWhileIteratingResults2() {
		StatisticsImplementor statistics = entityManagerFactory().getStatistics();
		statistics.clear();
		doInJPA( this::entityManagerFactory, entityManager -> {
					 List<User> users = entityManager.createNamedQuery( User.FIND_ALL, User.class ).getResultList();

					 List<User> nativeQueryUsers = entityManager.createNativeQuery( "select u.* from USERS u", User.class )
							 .getResultList();
					 statistics.clear();
					 for ( User user : nativeQueryUsers ) {
						 user.getAddress().size();
					 }
					 assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
				 }
		);
	}


	@Entity(name = "Address")
	public static class Address {
		@Id
		@GeneratedValue
		private Long id;

		private String address;

		@ManyToOne(optional = true, fetch = FetchType.LAZY)
		private User user;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public User getUser() {
			return user;
		}

		public void setUser(User user) {
			this.user = user;
		}
	}

	@NamedQueries(value = {
			@NamedQuery(
					name = User.FIND_ALL,
					query = "select u from User u join u.address"
			)
	})
	@Entity(name = "User")
	@Table(name = "USERS")
	public static class User {

		public static final String FIND_ALL = "User.findAll";

		@Id
		@GeneratedValue
		private Long id;

		@Column
		private String name;

		@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "user")
		private List<Address> address;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<Address> getAddress() {
			return address;
		}

		public void setAddress(List<Address> address) {
			this.address = address;
		}
	}
}
