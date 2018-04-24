/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import org.hibernate.HibernateException;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Marker interface for basic types.
 *
 * @author Steve Ebersole
 */
public interface BasicType<T> extends Type<T> {
	/**
	 * Get the names under which this type should be registered in the type registry.
	 *
	 * @return The keys under which to register this type.
	 */
	String[] getRegistrationKeys();

	BasicJavaDescriptor<T> getJavaTypeDescriptor();

	/**
	 * Get the Java type handled by this Hibernate mapping Type.  May return {@code null}
	 * in the case of non-basic types in dynamic domain models.
	 */
	default Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	/**
	 * The descriptor of the SQL type part of this basic-type
	 */
	SqlTypeDescriptor getSqlTypeDescriptor();

	default boolean areEqual(T x, T y) throws HibernateException {
		return EqualsHelper.areEqual( x, y );
	}
}
