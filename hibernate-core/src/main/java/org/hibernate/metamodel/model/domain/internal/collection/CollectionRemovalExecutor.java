/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Delegate for removing a collection by key.
 *
 * @author Steve Ebersole
 */
public interface CollectionRemovalExecutor {
	/**
	 * A no-op instance
	 */
	CollectionRemovalExecutor NO_OP = (key, session) -> {};

	/**
	 * Remove (delete) the collection indicated by key
	 */
	void execute(Object key, SharedSessionContractImplementor session);
}
