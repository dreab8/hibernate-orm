/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.mapping;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.MetaValue;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
public class AnyMappingTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {FirstProperty.class, SecondProperty.class};
	}

	@Test
	public void testId() {
		TransactionUtil.doInHibernate( this::sessionFactory, session1 -> {

		} );
	}

	@Entity
	public static class MyEntity4 {
		@Id
		int myEntity_4_Id;

		@ManyToOne
		MyEntity one_to_may_property;
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
	public static class MyEntity3 {
		@Id
		int myEntity_3_Id;
	}

	public interface Property {

		String getName();

		String asString();
	}

	@Entity
	public static class FirstProperty implements Property {

		@Id
		Long myEntity_Id;

		@Override
		public String getName() {
			return null;
		}

		@Override
		public String asString() {
			return null;
		}
	}

	@Entity
	public static class SecondProperty implements Property {
		@Id
		Long myEntity_Id;

		@Override
		public String getName() {
			return null;
		}

		@Override
		public String asString() {
			return null;
		}
	}


	@Entity
	public static class MyAnyEntity {

		@Any(
				metaDef = "PropertyMetaDef",
				metaColumn = @Column(name = "property_type")
		)
		@AnyMetaDef(name = "PropertyMetaDef", metaType = "string", idType = "long",
				metaValues = {
						@MetaValue(value = "S", targetEntity = FirstProperty.class),
						@MetaValue(value = "I", targetEntity = SecondProperty.class)
				}
		)
		private Property property;
	}
}
