/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.convert.spi;

import javax.persistence.AttributeConverter;

import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;

import com.fasterxml.classmate.ResolvedType;

/**
 * Boot-time descriptor of a JPA AttributeConverter
 *
 * @author Steve Ebersole
 */
public interface ConverterDescriptor<O,R> {
	/**
	 * The AttributeConverter class
	 */
	Class<? extends AttributeConverter> getAttributeConverterClass();

	/**
	 * The Java type of the user's domain model attribute, as defined by the AttributeConverter's
	 * parameterized type signature.
	 *
	 * @return The application domain model's attribute Java Type
	 */
	BasicJavaDescriptor<O> getDomainType();

	/**
	 * The "intermediate" Java type of the JDBC/SQL datatype (as we'd read through ResultSet, e.g.), as
	 * defined by the AttributeConverter's parameterized type signature.
	 *
	 * @return The "intermediate" JDBC/SQL Java type.
	 */
	BasicJavaDescriptor<R> getJdbcType();

	/**
	 * The resolved Classmate type descriptor for the conversion's domain type
	 */
	ResolvedType getDomainValueResolvedType();

	/**
	 * The resolved Classmate type descriptor for the conversion's relational type
	 */
	ResolvedType getRelationalValueResolvedType();

	/**
	 * Get the auto-apply checker for this converter.  Should never return `null` - prefer
	 * {@link org.hibernate.boot.model.convert.internal.AutoApplicableConverterDescriptorBypassedImpl#INSTANCE}
	 * instead.
	 */
	AutoApplicableConverterDescriptor getAutoApplyDescriptor();

	/**
	 * Factory for the runtime representation of the converter
	 */
	JpaAttributeConverter createJpaAttributeConverter(JpaAttributeConverterCreationContext context);
}
