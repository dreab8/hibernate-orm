/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;


@Jpa(
		annotatedClasses = {
				ExdendingMappedSuperclassTest.Description.class,
				ExdendingMappedSuperclassTest.Item.class,
				ExdendingMappedSuperclassTest.ExtendedDescription.class,
		}
)
public class ExdendingMappedSuperclassTest {

	@Test
	public void testSetDescription(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager ->
				entityManager.persist( new Item( 1L, new Description( "Test" ) ) )
		);
	}

	@Test
	public void testSetExtendedDescription(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager ->
				entityManager.persist( new Item( 2L, new ExtendedDescription( "Test2" ) ) )
		);
	}

	@Entity(name = "Item")
	public static class Item {

		@Id
		private Long id;

		@Embedded
		private Description description;

		protected Item() {
		}

		public Item(Long id, Description description) {
			this.id = id;
			this.description = description;
		}

		public Long getId() {
			return id;
		}

		public Description getDescription() {
			return description;
		}
	}

	@MappedSuperclass
	public static class Description {

		private String name;

		public Description() {
		}

		public Description(String name) {
			this.name = name;
		}
	}

	@Embeddable
	public static class ExtendedDescription extends Description {

		public ExtendedDescription() {
			super();
		}

		public ExtendedDescription(String name) {
			super( name );
		}
	}
}
