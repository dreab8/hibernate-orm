/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.wip60.crud;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Andrea Boriero
 */
public class EmbeddableWithColumnInsertableFalseTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	@Before
	public void setUp() {
		doInHibernate( this::sessionFactory, session -> {
			Name name = new Name( "Fabiana", "Fab" );
			Person person = new Person( 1, name, 33 );
			session.save( person );
		} );
	}

	@After
	public void tearDown() {
		doInHibernate( this::sessionFactory, session -> {
			session.createQuery( "delete from Person" ).executeUpdate();
		} );
	}

	@Test
	public void testSaving() {
		doInHibernate( this::sessionFactory, session -> {

			Person person = session.get( Person.class, 1 );
			assertThat( person.name.secondName, nullValue() );
			assertThat( person.name.firstName, is( "Fabiana" ) );
		} );
	}

	@Test
	public void testUpdating() {
		doInHibernate( this::sessionFactory, session -> {
			Person person = session.get( Person.class, 1 );
			person.setAge( 34 );
			person.getName().setSecondName( "Fabi" );
			person.getName().setFirstName( "Fabi" );
		} );

		doInHibernate( this::sessionFactory, session -> {
			Person person = session.get( Person.class, 1 );
			assertThat( person.name.secondName, nullValue() );
			assertThat( person.name.firstName, is( "Fabi" ) );
		} );
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		private Integer id;

		private Name name;

		private Integer age;

		public Person() {
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}

		public Person(Integer id, Name name, Integer age) {
			this.id = id;
			this.name = name;
			this.age = age;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Name getName() {
			return name;
		}

		public void setName(Name name) {
			this.name = name;
		}

		public Integer getAge() {
			return age;
		}

		public void setAge(Integer age) {
			this.age = age;
		}
	}


	@Embeddable
	public static class Name {
		private String firstName;
		@Column(insertable = false, updatable = false)
		private String secondName;

		public Name() {
		}

		public Name(String firstName, String secondName) {
			this.firstName = firstName;
			this.secondName = secondName;
		}

		public String getFirstName() {
			return firstName;
		}

		public String getSecondName() {
			return secondName;
		}

		public void setSecondName(String secondName) {
			this.secondName = secondName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}
	}

}

