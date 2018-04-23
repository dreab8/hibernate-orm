/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Represents the commonality between {@link PhysicalColumn} and {@link DerivedColumn}
 *
 * @author Steve Ebersole
 */
public interface Column  {
	Table getSourceTable();

	String getExpression();

	// todo : nullable, size, etc

	// todo : org.hibernate.annotations.ColumnTransformer#read and org.hibernate.annotations.ColumnTransformer#write?

	String toLoggableString();

	// todo (6.0) : need to account for rendering a column for both read and write separately
	//		this is to help account for custom read/write fragments on PhysicalColumn.  We
	//		can apply the same logic (polymorphism) to DerivedColumns as well, except that:
	//			1) the DerivedColumn's reader is always the formula expression (illegal to apply transformer)
	//			2) the DerivedColumn's writer is always null. again illegal to apply transformer, and by definition a DerivedColumn is read-only

	String render(String identificationVariable);


	SqlTypeDescriptor getSqlTypeDescriptor();

	/**
	 * @deprecated Use {@link #getSqlTypeDescriptor()} instead
	 */
	@Deprecated
	default int getJdbcType() {
		return getSqlTypeDescriptor().getJdbcTypeCode();
	}

}
