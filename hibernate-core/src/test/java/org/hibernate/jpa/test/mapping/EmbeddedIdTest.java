/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.mapping;

import java.io.Serializable;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
public class EmbeddedIdTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				MyEntity.class,
				MyEntity2.class,
				MyEntity3.class,
				MyEntity4.class,
				MyEntity5.class,
				MyEntity6.class
		};
	}

	@Test
	public void testId() {
		TransactionUtil.doInHibernate( this::sessionFactory, session1 -> {

		} );
	}

	@Entity
	public static class MyEntity {
		@EmbeddedId
		EmbeddedIdClass myEntity_Id;
	}


	@Embeddable
	public static class EmbeddedIdClass implements Serializable {
		private Long embeddedIdClass_Id_1;
		private Integer embeddedIdClass_Id_2;
		private Integer embeddedIdClass_Id_3;


		@ManyToOne
		private MyEntity3 embeddedIdClass_Id_4;
	}

	@Entity
	public static class MyEntity2 {
		@Id
		int myEntity_2_Id;

		@Embedded
		EmbeddedIdClass myEntity_2_embeddedProperty;
	}

	@Entity
	public static class MyEntity4 {
		@Id
		int myEntity_4_Id;

		@ManyToOne
		MyEntity one_to_may_property;
	}

	@Entity
	public static class MyEntity3 {
		@Id
		int myEntity_3_Id;
	}

	@Entity
	public static class MyEntity5 {
		@Id
		@ManyToOne
		MyEntity3 myEntity_3_Id;
	}

	@Entity
	public static class MyEntity6 {

		@Id
		Long id;

		@ElementCollection
		Set<EmbeddedIdClass> embeddeds_element_collection;

//		@ElementCollection
//		Set<String> string_collection;

//		@OneToMany
//		Set<MyEntity> entities;
	}
}
