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
package org.wip60.Inheritance;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Polymorphism;
import org.hibernate.annotations.PolymorphismType;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 * <p>
 * We can query against the interface, and Hibernate is going to fetch only the entities
 * that are either mapped with @Polymorphism(type = PolymorphismType.IMPLICIT) or
 * they are not annotated at all with the @Polymorphism annotation (implying the IMPLICIT behavior):
 */
public class PolymorphismTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Book.class,
				Blog.class
		};
	}


	@Before
	public void setUp() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					Book book = new Book();
					book.setId( 1L );
					book.setAuthor( "Vlad Mihalcea" );
					book.setTitle( "High-Performance Java Persistence" );
					session.persist( book );

					Blog blog = new Blog();
					blog.setId( 1L );
					blog.setSite( "vladmihalcea.com" );
					session.persist( blog );
				}
		);
	}

	@Test
	public void testLoadSuperclass() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					List<DomainModelEntity> accounts = session
							.createQuery(
									"select e " +
											"from org.wip60.Inheritance.DomainModelEntity e" )
							.getResultList();

					assertEquals( 1, accounts.size() );
					assertTrue( accounts.get( 0 ) instanceof Book );
				}
		);
	}

	@Entity(name = "Book")
	public static class Book implements DomainModelEntity<Long> {

		@Id
		private Long id;


		private String title;

		private String author;

		@Override
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public void setAuthor(String author) {
			this.author = author;
		}

		public String getTitle() {
			return title;
		}

		public String getAuthor() {
			return author;
		}
	}

	@Entity(name = "Blog")
	@Polymorphism(type = PolymorphismType.EXPLICIT)// it is not fetched
	public static class Blog implements DomainModelEntity<Long> {

		@Id
		private Long id;

		private String site;

		@Override
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public void setSite(String site) {
			this.site = site;
		}
	}
}
