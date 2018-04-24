/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.convert.internal;

import javax.persistence.AttributeConverter;

import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ProvidedInstanceManagedBeanImpl;

/**
 * ConverterDescriptor implementation for cases where we are handed
 * the AttributeConverter instance to use.
 *
 * @author Steve Ebersole
 */
public class InstanceBasedConverterDescriptor extends AbstractConverterDescriptor {
	private final AttributeConverter converterInstance;

	public InstanceBasedConverterDescriptor(
			AttributeConverter converterInstance,
			BootstrapContext context) {
		this( converterInstance, null, context );
	}

	public InstanceBasedConverterDescriptor(
			AttributeConverter converterInstance,
			Boolean forceAutoApply,
			BootstrapContext context) {
		super( converterInstance.getClass(), forceAutoApply, context );
		this.converterInstance = converterInstance;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected ManagedBean<? extends AttributeConverter> createManagedBean(JpaAttributeConverterCreationContext context) {
		return new ProvidedInstanceManagedBeanImpl( converterInstance );
	}
}
