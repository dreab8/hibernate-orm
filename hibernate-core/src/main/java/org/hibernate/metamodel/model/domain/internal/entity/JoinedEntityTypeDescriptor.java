/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.entity;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.domain.spi.EntityMappingImplementor;
import org.hibernate.boot.model.domain.spi.ManagedTypeMappingImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.loader.internal.StandardSingleIdEntityLoader;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractEntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 * @author Steve Ebersole
 */
public class JoinedEntityTypeDescriptor<J> extends AbstractEntityTypeDescriptor<J> {

	public JoinedEntityTypeDescriptor(
			EntityMappingImplementor bootMapping,
			IdentifiableTypeDescriptor<? super J> superTypeDescriptor,
			RuntimeModelCreationContext creationContext) throws HibernateException {
		super( bootMapping, superTypeDescriptor, creationContext );
	}

	@Override
	public String asLoggableText() {
		return String.format( "JoinedEntityTypeDescriptor<%s>", getEntityName() );
	}

	@Override
	public boolean finishInitialization(
			ManagedTypeMappingImplementor bootDescriptor,
			RuntimeModelCreationContext creationContext) {
		final boolean superDone = super.finishInitialization( bootDescriptor, creationContext );
		if ( !superDone ) {
			return false;
		}

		if ( bootDescriptor instanceof RootClass ) {
			// the hierarchy root
		}
		else if ( bootDescriptor instanceof JoinedSubclass ) {
			// branch/leaf
		}
		else {
			throw new IllegalStateException(
					"Expecting boot model descriptor to be RootClass or JoinedSubclass, but found : " + bootDescriptor );
		}

		return true;
	}

	@Override
	public void postInitialization(RuntimeModelCreationContext creationContext) {
	}


	@Override
	public SqmNavigableReference createSqmExpression(SqmPath lhs, SqmCreationState creationState) {
		//noinspection unchecked
		return new SqmBasicValuedSimplePath(
				new NavigablePath( getNavigableName() + DiscriminatorDescriptor.NAVIGABLE_NAME ),
				this.getHierarchy().getDiscriminatorDescriptor(),
				null,
				creationState.getCreationContext().getQueryEngine().getCriteriaBuilder()
		);
	}

	@Override
	public void insert(
			Object id, Object[] fields, Object object, SharedSessionContractImplementor session)
			throws HibernateException {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Object insert(
			Object[] fields, Object object, SharedSessionContractImplementor session) throws HibernateException {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void delete(
			Object id, Object version, Object object, SharedSessionContractImplementor session)
			throws HibernateException {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void update(
			Object id,
			Object[] fields,
			int[] dirtyFields,
			boolean hasDirtyCollection,
			Object[] oldFields,
			Object oldVersion,
			Object object,
			Object rowId,
			SharedSessionContractImplementor session) throws HibernateException {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public Serializable getIdByUniqueKey(
			Serializable key, String uniquePropertyName, SharedSessionContractImplementor session) {

		throw new NotYetImplementedFor6Exception( getClass() );
	}
	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
