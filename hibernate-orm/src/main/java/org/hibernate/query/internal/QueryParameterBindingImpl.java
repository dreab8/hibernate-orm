/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query.internal;

import java.util.Collection;
import javax.persistence.TemporalType;

import org.hibernate.query.QueryParameter;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindingTypeResolver;
import org.hibernate.query.spi.QueryParameterBindingValidator;
import org.hibernate.type.spi.Type;

/**
 * The standard Hibernate QueryParameterBinding implementation
 *
 * @author Steve Ebersole
 */
public class QueryParameterBindingImpl<T> implements QueryParameterBinding<T> {
	private final QueryParameter<T> queryParameter;
	private final QueryParameterBindingTypeResolver typeResolver;
	private final boolean isBindingValidationRequired;

	private boolean isBound;
	private boolean isMultiValued;

	private Type bindType;

	private T bindValue;
	private Collection<T> bindValues;

	public QueryParameterBindingImpl(
			QueryParameter<T> queryParameter,
			QueryParameterBindingTypeResolver typeResolver,
			boolean isBindingValidationRequired) {
		this( queryParameter.getHibernateType(), queryParameter, typeResolver, isBindingValidationRequired );
	}

	public QueryParameterBindingImpl(
			Type bindType,
			QueryParameter<T> queryParameter,
			QueryParameterBindingTypeResolver typeResolver,
			boolean isBindingValidationRequired) {
		this.bindType = bindType;
		this.queryParameter = queryParameter;
		this.typeResolver = typeResolver;
		this.isBindingValidationRequired = isBindingValidationRequired;
	}

	@Override
	public Type getBindType() {
		return bindType;
	}

	@Override
	public boolean allowsMultiValued() {
		return queryParameter.allowsMultiValuedBinding();
	}

	@Override
	public boolean isBound() {
		return isBound;
	}

	@Override
	public boolean isMultiValued() {
		return isMultiValued;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// single-valued binding support

	@Override
	public T getBindValue() {
		if ( isMultiValued ) {
			throw new IllegalStateException( "Binding is multi-valued; illegal call to #getBindValue" );
		}

		return bindValue;
	}

	@Override
	public void setBindValue(T value) {
		if ( isBindingValidationRequired ) {
			validate( value );
		}
		this.isBound = true;
		this.isMultiValued = false;

		this.bindValue = value;
		this.bindValues = null;

		if ( bindType == null ) {
			this.bindType = typeResolver.resolveParameterBindType( value );
		}
		bindValue( value );
	}

	@Override
	public void setBindValue(T value, Type clarifiedType) {
		if ( isBindingValidationRequired ) {
			validate( value, clarifiedType );
		}
		bindValue( value );
		if ( clarifiedType != null ) {
			this.bindType = clarifiedType;
		}
	}

	@Override
	public void setBindValue(T value, TemporalType clarifiedTemporalType) {
		if ( isBindingValidationRequired ) {
			validate( value, clarifiedTemporalType );
		}
		bindValue( value );
		this.bindType = BindingTypeHelper.INSTANCE.determineTypeForTemporalType( clarifiedTemporalType, bindType, value );
	}

	private void bindValue(T value) {
		this.isBound = true;
		this.bindValue = value;

		if ( bindType == null ) {
			this.bindType = typeResolver.resolveParameterBindType( value );
		}
	}

	private void validate(T value) {
		QueryParameterBindingValidator.INSTANCE.validate( getBindType(), value );
	}

	private void validate(T value, Type clarifiedType) {
		QueryParameterBindingValidator.INSTANCE.validate( clarifiedType, value );
	}

	private void validate(T value, TemporalType clarifiedTemporalType) {
		QueryParameterBindingValidator.INSTANCE.validate( getBindType(), value, clarifiedTemporalType );
	}
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// multi-valued binding support

	@Override
	public Collection<T> getBindValues() {
		if ( !isMultiValued ) {
			throw new IllegalStateException( "Binding is not multi-valued; illegal call to #getBindValues" );
		}

		return bindValues;
	}

	@Override
	public void setBindValues(Collection<T> values) {
		this.isBound = true;
		this.isMultiValued = true;

		this.bindValue = null;
		this.bindValues = values;

		if ( bindType == null && !values.isEmpty() ) {
			this.bindType = typeResolver.resolveParameterBindType( values.iterator().next() );
		}

	}

	@Override
	public void setBindValues(Collection<T> values, Type clarifiedType) {
		setBindValues( values );
		if ( clarifiedType != null ) {
			this.bindType = clarifiedType;
		}
	}

	@Override
	public void setBindValues(Collection<T> values, TemporalType clarifiedTemporalType) {
		setBindValues( values );
		final Object exampleValue = values.isEmpty() ? null : values.iterator().next();
		this.bindType = BindingTypeHelper.INSTANCE.determineTypeForTemporalType( clarifiedTemporalType, bindType, exampleValue );
	}
}
