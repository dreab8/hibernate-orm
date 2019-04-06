/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.DomainType;

/**
 * @author Steve Ebersole
 */
public interface DomainTypeDescriptor<J> extends DomainType<J> {

	// todo (6.0) : is this the correct place for isDirty and isModified methods?
	/**
	 * Return whether any of the managed type's persistent attribute state is dirty.
	 */
	default boolean isDirty(Object one, Object another, SharedSessionContractImplementor session) {
		return !getJavaTypeDescriptor().areEqual( (J) one, (J) another );
	}

	default boolean isModified(Object old, Object current, SharedSessionContractImplementor session) {
		return isDirty( old, current, session );
	}

	@Override
	default boolean areEqual(J x, J y) throws HibernateException{
		return getJavaTypeDescriptor().areEqual( x,y );
	}

	default int extractHashCode( J o ){
		return getJavaTypeDescriptor().extractHashCode( o );
	}
}
