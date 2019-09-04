/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.internal.util.beans.BeanInfoHelper;
import org.hibernate.query.sqm.tree.expression.Compatibility;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class DynamicInstantiationInjectionAssemblerImpl<T> implements DomainResultAssembler<T> {
	private final JavaTypeDescriptor<T> target;
	private final List<BeanInjection> beanInjections = new ArrayList<>();

	@SuppressWarnings("WeakerAccess")
	public DynamicInstantiationInjectionAssemblerImpl(
			JavaTypeDescriptor<T> target,
			List<ArgumentReader<?>> argumentReaders) {
		this.target = target;
		final Class targetJavaType = target.getJavaType();

		BeanInfoHelper.visitBeanInfo(
				targetJavaType,
				beanInfo -> {
					for ( ArgumentReader<?> argumentReader : argumentReaders ) {
						boolean found = false;
						for ( PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors() ) {
							if ( argumentReader.getAlias().equals( propertyDescriptor.getName() ) ) {
								if ( propertyDescriptor.getWriteMethod() != null ) {
									final boolean assignmentCompatible = Compatibility.areAssignmentCompatible(
											propertyDescriptor.getWriteMethod().getParameterTypes()[0],
											argumentReader.getAssembledJavaTypeDescriptor().getClass()
									);
									if ( assignmentCompatible ) {
										propertyDescriptor.getWriteMethod().setAccessible( true );
										beanInjections.add(
												new BeanInjection(
														new BeanInjectorSetter( propertyDescriptor.getWriteMethod() ),
														argumentReader
												)
										);
										found = true;
										break;
									}
								}
							}
						}
						if ( found ) {
							continue;
						}

						// see if we can find a Field with the given name...
						final Field field = findField(
								targetJavaType,
								argumentReader.getAlias(),
								argumentReader.getAssembledJavaTypeDescriptor().getJavaType()
						);
						if ( field != null ) {
							beanInjections.add(
									new BeanInjection(
											new BeanInjectorField( field ),
											argumentReader
									)
							);
						}
						else {
							throw new InstantiationException(
									"Unable to determine dynamic instantiation injection strategy for " +
											targetJavaType.getName() + "#" + argumentReader.getAlias()
							);
						}
					}
				}
		);

		if ( argumentReaders.size() != beanInjections.size() ) {
			throw new IllegalStateException( "The number of readers did not match the number of injections" );
		}
	}

	private Field findField(Class declaringClass, String name, Class javaType) {
		try {
			Field field = declaringClass.getDeclaredField( name );
			// field should never be null
			if ( Compatibility.areAssignmentCompatible( field.getType(), javaType ) ) {
				field.setAccessible( true );
				return field;
			}
		}
		catch (NoSuchFieldException ignore) {
		}

		return null;
	}

	@Override
	public JavaTypeDescriptor<T> getAssembledJavaTypeDescriptor() {
		return target;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
		try {
			final T result = target.getJavaType().newInstance();
			for ( BeanInjection beanInjection : beanInjections ) {
				beanInjection.getBeanInjector().inject(
						result,
						beanInjection.getValueAssembler().assemble( rowProcessingState, options )
				);
			}
			return result;
		}
		catch (IllegalAccessException | InstantiationException | java.lang.InstantiationException e) {
			throw new InstantiationException( "Could not call default constructor [" + target.getJavaType().getSimpleName() + "]", e );
		}
	}
}
