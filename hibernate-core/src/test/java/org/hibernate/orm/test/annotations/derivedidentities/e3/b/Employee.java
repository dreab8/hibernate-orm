/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.derivedidentities.e3.b;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;


/**
 * @author Emmanuel Bernard
 */
@Entity
public class Employee {
	@EmbeddedId
	EmployeeId empId;
}
