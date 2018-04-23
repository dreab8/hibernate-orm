/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.Iterator;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.ValueMapping;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

/**
 * A value is anything that is persisted by value, instead of
 * by reference. It is essentially a Hibernate Type, together
 * with zero or more columns. Values are wrapped by things with
 * higher level semantics, for example properties, collections,
 * classes.
 *
 * @author Gavin King
 */
public interface Value<J> extends ValueMapping<J>, Serializable {
	int getColumnSpan();
	/**
	 * @deprecated since 6.0, use {@link #getMappedColumns()} instead.
	 */
	@Deprecated
	Iterator<Selectable> getColumnIterator();
	Type getType() throws MappingException;

	/**
	 * @deprecated since 6.0, use {@link #getMappedTable()} instead.
	 */
	@Deprecated
	Table getTable();
	boolean hasFormula();

	boolean isAlternateUniqueKey();
	boolean isNullable();
	boolean[] getColumnUpdateability();
	boolean[] getColumnInsertability();
	boolean isSimpleValue();

	/**
	 * @deprecated since 6.0, use {@link #isValid()} instead.
	 */
	@Deprecated
	boolean isValid(Mapping mapping) throws MappingException;

	default boolean isValid() throws MappingException{
		return isValid( null );
	}
	void setTypeUsingReflection(String className, String propertyName) throws MappingException;
	Object accept(ValueVisitor visitor);

	boolean isSame(Value other);
	FetchMode getFetchMode();

	ServiceRegistry getServiceRegistry();

	default ForeignKey createForeignKey() throws MappingException {
		return null;
	}
}
