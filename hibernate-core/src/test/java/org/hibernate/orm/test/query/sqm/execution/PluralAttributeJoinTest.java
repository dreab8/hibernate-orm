package org.hibernate.orm.test.query.sqm.execution;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;

/**
 * @author Andrea Boriero
 */
public class PluralAttributeJoinTest extends SessionFactoryBasedFunctionalTest {

	@Entity( name = "Person" )
	public static class Person {
		@Id
		public Integer id;
		public String name;

		@OneToMany
		List<Address> addressList;
	}

	@Entity(name = "Address")
	public static class Address {
		@Id
		public Integer id;
		public String street;
	}

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( Person.class );
		metadataSources.addAnnotatedClass( Address.class );
	}

	@Test
	public void testSelectId() {
		sessionFactoryScope().inTransaction(
				session -> {
					final List result = session.createQuery( "select p.id from Person p join p.addressList" ).list();
					assertThat( result, hasSize( 1 ) );
					final Object value = result.get( 0 );
					assertThat( value, instanceOf( Integer.class ) );
					assertThat( value, is( 1 ) );
				}
		);
	}
}

