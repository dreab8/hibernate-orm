/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain;

import javax.persistence.metamodel.Type.PersistenceType;

import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.mapping.Filterable;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public interface EntityMapping extends IdentifiableTypeMapping, Filterable {
	String getEntityName();

	String getJpaEntityName();

	Class getRuntimeEntityDescriptorClass();

	void setEntityPersisterClass(Class entityPersisterClass);

	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.ENTITY;
	}

	MappedTable getRootTable();

	boolean hasProxy();

	boolean hasFormulaAttributes();

	Class getProxyInterface();

	ExecuteUpdateResultCheckStyle getUpdateResultCheckStyle();

	Class getMappedClass();

	int getBatchSize();

	EntityIdentifier makeRuntimeIdentifierDescriptor(
			EntityHierarchy runtimeModelHierarchy,
			EntityTypeDescriptor runtimeModelRootEntity,
			RuntimeModelCreationContext creationContext);
}
