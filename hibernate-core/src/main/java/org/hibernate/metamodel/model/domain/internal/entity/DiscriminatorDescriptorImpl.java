/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.metamodel.model.domain.internal.entity;

import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.BasicValueMapper;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorMappings;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class DiscriminatorDescriptorImpl<O,J> implements DiscriminatorDescriptor<J> {
	public static final String NAVIGABLE_NAME = "{discriminator}";

	private final EntityHierarchy hierarchy;
	private final BasicValueMapper<J> valueMapper;
	private Column column;

	private final NavigableRole navigableRole;

	@SuppressWarnings("WeakerAccess")
	public DiscriminatorDescriptorImpl(
			EntityHierarchy hierarchy,
			BasicValueMapping<J> valueMapping,
			RuntimeModelCreationContext creationContext) {
		this.hierarchy = hierarchy;

		this.valueMapper = valueMapping.getResolution().getValueMapper();

		this.navigableRole = hierarchy.getRootEntityType().getNavigableRole().append( NAVIGABLE_NAME );
	}

	@Override
	public boolean finishInitialization(Object bootReference, RuntimeModelCreationContext creationContext) {
		this.column = getContainer().getColumn(
				creationContext.getDatabaseObjectResolver().resolvePhysicalColumnName(
						( (BasicValueMapping<J>) bootReference ).getMappedColumn()
				)
		);

		return true;
	}

	@Override
	public EntityTypeDescriptor<O> getContainer() {
		return hierarchy.getRootEntityType();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public String getNavigableName() {
		return NAVIGABLE_NAME;
	}

	@Override
	public BasicJavaDescriptor<J> getJavaTypeDescriptor() {
		return getValueMapper().getDomainJavaDescriptor();
	}

	@Override
	public DiscriminatorMappings getDiscriminatorMappings() {
		// todo (6.0) : will probably need to collect these dynamically during "first phase" of runtime model creation
		throw new NotYetImplementedException(  );
	}

	@Override
	public String asLoggableText() {
		return getContainer().asLoggableText() + '.' + NAVIGABLE_NAME;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public Column getBoundColumn() {
		return column;
	}

	@Override
	public BasicValueMapper<J> getValueMapper() {
		return valueMapper;
	}

	@Override
	public SqlExpressableType getSqlExpressableType() {
		return valueMapper.getSqlExpressableType();
	}

}
