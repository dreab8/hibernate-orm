/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.mapping;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

@TestForIssue(jiraKey = "HHH-11587")
public class UnidirectionalOneToManyOrderColumnTest extends BaseEntityManagerFunctionalTestCase {

	@Test
	public void testRemovingAnElement() {
		doInJPA( this::entityManagerFactory, entityManager -> {

			ParentData parent = new ParentData();
			entityManager.persist( parent );

			String[] childrenStr = new String[] {"One", "Two", "Three"};
			for ( String str : childrenStr ) {
				ChildData child = new ChildData( str );
				entityManager.persist( child );
				parent.getChildren().add( child );
			}

			entityManager.flush();

			List<ChildData> children = parent.getChildren();
			children.remove( 0 );
		} );
	}

	@Test
	public void testAddingAnElement() {
		doInJPA( this::entityManagerFactory, entityManager -> {

			ParentData parent = new ParentData();
			entityManager.persist( parent );

			String[] childrenStr = new String[] {"One", "Two", "Three"};
			for ( String str : childrenStr ) {
				ChildData child = new ChildData( str );
				entityManager.persist( child );
				parent.getChildren().add( child );
			}

			entityManager.flush();

			List<ChildData> children = parent.getChildren();
			children.add( 1, new ChildData( "Another" ) );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				ParentData.class,
				ChildData.class
		};
	}

	@Entity(name = "ParentData")
	@Table(name = "PARENT")
	public static class ParentData {
		@Id
		@GeneratedValue
		long id;

		@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
		@OrderColumn(name = "listOrder")
		private List<ChildData> children = new ArrayList<>();

		public List<ChildData> getChildren() {
			return children;
		}
	}

	@Entity(name = "ChildData")
	@Table(name = "CHILD")
	public static class ChildData {
		@Id
		@GeneratedValue
		long id;

		String childId;

		public ChildData() {
		}

		public ChildData(String id) {
			childId = id;
		}

		@Override
		public String toString() {
			return childId;
		}
	}

}
