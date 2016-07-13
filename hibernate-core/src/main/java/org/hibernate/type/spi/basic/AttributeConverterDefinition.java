/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.basic;

import javax.persistence.AttributeConverter;

/**
 * Internal descriptor for an AttributeConverter implementation, with the intent of being
 * incorporated into a {@link org.hibernate.type.spi.BasicType}
 *
 * @author Steve Ebersole
 */
public interface AttributeConverterDefinition {
	/**
	 * Access to the AttributeConverter instance to be used by the built BasicType.
	 *
	 * @return The AttributeConverter
	 */
	AttributeConverter getAttributeConverter();

	/**
	 * The Java type of the user's domain model attribute, as defined by the AttributeConverter's
	 * parameterized type signature.
	 *
	 * @return The application domain model's attribute Java Type
	 */
	Class<?> getDomainType();

	/**
	 * The "intermediate" Java type of the JDBC/SQL datatype (as we'd read through ResultSet, e.g.), as
	 * defined by the AttributeConverter's parameterized type signature.
	 *
	 * @return The "intermediate" JDBC/SQL Java type.
	 */
	Class<?> getJdbcType();
}
