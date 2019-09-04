/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.procedure.internal;

import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.query.internal.QueryParameterBindingImpl;
import org.hibernate.query.procedure.ProcedureParameterBinding;
import org.hibernate.query.procedure.spi.ProcedureParameterBindingImplementor;
import org.hibernate.query.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.spi.QueryParameterBindingTypeResolver;

/**
 * Implementation of the {@link ProcedureParameterBinding} contract.
 *
 * @author Steve Ebersole
 */
public class ProcedureParameterBindingImpl<T>
		extends QueryParameterBindingImpl<T>
		implements ProcedureParameterBindingImplementor<T> {
	public ProcedureParameterBindingImpl(
			ProcedureParameterImplementor<T> queryParameter,
			QueryParameterBindingTypeResolver typeResolver) {
		super( queryParameter, typeResolver, true );
	}

	public ProcedureParameterBindingImpl(
			AllowableParameterType<T> bindType,
			ProcedureParameterImplementor<T> queryParameter,
			QueryParameterBindingTypeResolver typeResolver) {
		super( queryParameter, typeResolver, bindType, true );
	}
}
