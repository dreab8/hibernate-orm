/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.crud.onetoone;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andrea Boriero
 */
@Disabled( "issues with non-aggregated composite-id" )
public class OneToOneWithDerivedIdentityTest extends SessionFactoryBasedFunctionalTest {

	private static final Integer PERSON_ID = 1;

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
				PersonInfo.class
		};
	}

	@Test
	public void testGet() {
		inTransaction(
				session -> {

					Person p = new Person();
					p.setId( 0 );
					p.setName( "Alfio" );
					PersonInfo pi = new PersonInfo();
					pi.setId( p );
					pi.setInfo( "Some information" );
					session.persist( p );
					session.persist( pi );

				} );

		inTransaction(
				session -> {
					Person person = session.get( Person.class, PERSON_ID );
					assertEquals( person.getName(), "Alfio" );
				} );
	}

	@Entity
	public static class Person {
		@Id
		private Integer id;

		@Basic
		private String name;

		@OneToOne(mappedBy = "id")
		private PersonInfo personInfo;

		public Integer getId() {
			return this.id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public PersonInfo getPersonInfo() {
			return this.personInfo;
		}

		public void setPersonInfo(PersonInfo personInfo) {
			this.personInfo = personInfo;
		}
	}

	@Entity
	public class PersonInfo {
		@Id
		@OneToOne
		private Person id;

		@Basic
		private String info;

		public Person getId() {
			return this.id;
		}

		public void setId(Person id) {
			this.id = id;
		}

		public String getInfo() {
			return this.info;
		}

		public void setInfo(String info) {
			this.info = info;
		}
	}
}
