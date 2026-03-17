/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal.lock;

import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.LockingClauseStrategy;

import java.util.Collection;
import java.util.Set;

public class LoadedValuesCollectorFactory {
	private final Collection<NavigablePath> pathsToLock;

	public LoadedValuesCollectorFactory(Set<NavigablePath> pathsToLock) {
		this.pathsToLock = pathsToLock;
	}

	public LoadedValuesCollectorFactory(LockingClauseStrategy lockingClauseStrategy) {
		pathsToLock = LockingHelper.extractPathsToLock( lockingClauseStrategy );
	}

	public LoadedValuesCollectorImpl build() {
		return new LoadedValuesCollectorImpl( pathsToLock );
	}
}
