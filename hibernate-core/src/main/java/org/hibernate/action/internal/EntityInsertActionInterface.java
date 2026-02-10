/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import org.hibernate.engine.spi.ComparableExecutable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;


public interface EntityInsertActionInterface extends ComparableExecutable {
	Object[] getState();

	EntityPersister getPersister();

	SharedSessionContractImplementor getSession() ;

	Object getInstance();

	String getEntityName();

}
