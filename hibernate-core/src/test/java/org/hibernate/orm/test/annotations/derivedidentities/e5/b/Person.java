/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.derivedidentities.e5.b;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

/**
 * @author Emmanuel Bernard
 */
@Entity
@IdClass(PersonId.class)
public class Person {
	@Id
	String firstName;
	@Id
	String lastName;
}
