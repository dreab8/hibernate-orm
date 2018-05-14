/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.spi;

import java.io.Serializable;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.type.descriptor.java.internal.EnumJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.JavaTypeDescriptorBaseline;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarbinaryTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;

import org.jboss.logging.Logger;

/**
 * Basically a map from {@link Class} -> {@link JavaTypeDescriptor}
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 *
 * @since 5.3
 */
public class JavaTypeDescriptorRegistry implements JavaTypeDescriptorBaseline.BaselineTarget {
	private static final Logger log = Logger.getLogger( JavaTypeDescriptorRegistry.class );

	private final TypeConfiguration typeConfiguration;
	private final ConcurrentHashMap<String, JavaTypeDescriptor> descriptorsByName = new ConcurrentHashMap<>();

	public JavaTypeDescriptorRegistry(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
		JavaTypeDescriptorBaseline.prime( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// baseline descriptors

	@Override
	public void addBaselineDescriptor(BasicJavaDescriptor descriptor) {
		if ( descriptor.getJavaType() == null ) {
			throw new IllegalStateException( "Illegal to add BasicJavaTypeDescriptor with null Java type" );
		}
		addBaselineDescriptor( (Class) descriptor.getJavaType(), descriptor );
	}

	@Override
	public void addBaselineDescriptor(Class describedJavaType, BasicJavaDescriptor descriptor) {
		performInjections( descriptor );
		descriptorsByName.put( describedJavaType.getName(), descriptor );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// descriptor access

	@SuppressWarnings("unchecked")
	public <T> JavaTypeDescriptor<T> getDescriptor(Class<T> javaType) {
		if ( javaType == null ) {
			throw new IllegalArgumentException( "Class passed to locate Java type descriptor cannot be null" );
		}

		JavaTypeDescriptor javaTypeDescriptor = descriptorsByName.computeIfAbsent(
				javaType.getName(),
				k -> makeOnTheFlyJavaTypeDescriptor( javaType )
		);

		return javaTypeDescriptor;
	}

	@SuppressWarnings("unchecked")
	private <T> BasicJavaDescriptor<T> makeOnTheFlyJavaTypeDescriptor(Class<T> javaType) {
		if ( javaType.isEnum() ) {
			return new EnumJavaDescriptor( javaType );
		}

		if ( Serializable.class.isInstance( javaType ) ) {
			log.debugf(
					"Could not find matching JavaTypeDescriptor for requested Java class [%s]; using fallback via its Serializable interface.  " +
							"This means Hibernate does not know how to perform certain basic operations in relation to this Java type" +
							"which can lead to those operations having a large performance impact.  " + solution(),
					javaType.getName(),
					JavaTypeDescriptorRegistry.class.getName(),
					TypeContributor.class.getName(),
					TypeConfiguration.class.getName()
			);
			return new OnTheFlySerializableJavaDescriptor( javaType );
		}

		throw new HibernateException(
				String.format(
						Locale.ROOT,
						"Cannot create on-the-fly JavaTypeDescriptor for given Java type: %s.  " + solution(),
						TypeContributor.class.getName(),
						TypeConfiguration.class.getName()
				)
		);
	}

	private String solution() {
		return "Consider registering these JavaTypeDescriptors with the %s during bootstrap, " +
				" either directly or through a registered %s accessing the %s ";
	}

	private class OnTheFlySerializableJavaDescriptor<T extends Serializable> extends AbstractBasicJavaDescriptor<T> {
		private final SqlTypeDescriptor sqlTypeDescriptor;

		public OnTheFlySerializableJavaDescriptor(Class<T> type) {
			super( type );

			// todo (6.0) : would be nice to expose for config by user
			// todo (6.0) : ^^ might also be nice to allow them to plug in a "JavaTypeDescriptorResolver"
			// 		- that allows them to hook into the #getDescriptor call either as the primary or as a fallback


			log.debugf(
					"Could not find matching JavaTypeDescriptor for requested Java class [%s]; using fallback via its Serializable interface.  " +
							"This means Hibernate does not know how to perform certain basic operations in relation to this Java type" +
							"which can lead to those operations having a large performance impact.  Consider registering these " +
							"JavaTypeDescriptors with the %s during bootstrap, either directly or through a registered %s " +
							"accessing the %s ",
					getJavaType().getName(),
					JavaTypeDescriptorRegistry.class.getName(),
					TypeContributor.class.getName(),
					TypeConfiguration.class.getName()
			);


			sqlTypeDescriptor = VarbinaryTypeDescriptor.INSTANCE;
		}

		@Override
		public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
			return sqlTypeDescriptor;
		}

		@Override
		public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
			if ( type.equals( byte[].class ) ) {
				throw new UnsupportedOperationException( "Cannot unwrap Serializable to format other than byte[]" );
			}

			return (X) SerializationHelper.serialize( value );
		}

		@Override
		@SuppressWarnings("unchecked")
		public <X> T wrap(X value, WrapperOptions options) {
			if ( value == null ) {
				return null;
			}

			if ( value.getClass().equals( byte[].class ) ) {
				throw new UnsupportedOperationException( "Cannot unwrap Serializable to format other than byte[]" );
			}

			final byte[] bytes = (byte[]) value;

			return (T) SerializationHelper.deserialize( bytes );
		}
	}

	@SuppressWarnings("unchecked")
	public <T> JavaTypeDescriptor<T> getDescriptor(String javaTypeName) {
		if ( javaTypeName == null ) {
			throw new IllegalArgumentException( "Java type name passed to locate Java type descriptor cannot be null" );
		}
		return descriptorsByName.get( javaTypeName );
	}

	public void addDescriptor(JavaTypeDescriptor descriptor) {
		addDescriptorInternal( descriptor.getTypeName(), descriptor );
	}

	private void addDescriptorInternal(JavaTypeDescriptor descriptor) {
		addDescriptorInternal( descriptor.getTypeName(), descriptor );
	}

	private void addDescriptorInternal(Class javaType, JavaTypeDescriptor descriptor) {
		addDescriptorInternal( javaType.getName(), descriptor );
	}

	private void addDescriptorInternal(String registrationKey, JavaTypeDescriptor descriptor) {
		performInjections( descriptor );

		final JavaTypeDescriptor old = descriptorsByName.put( registrationKey, descriptor );
		if ( old != null && old != descriptor ) {
			log.debugf(
					"JavaTypeDescriptorRegistry entry replaced : %s -> %s (was %s)",
					descriptor.getJavaType(),
					descriptor,
					old
			);
		}
	}

	private void performInjections(JavaTypeDescriptor descriptor) {
		if ( descriptor instanceof TypeConfigurationAware ) {
			// would be nice to make the JavaTypeDescriptor for an entity, e.g., aware of the the TypeConfiguration
			( (TypeConfigurationAware) descriptor ).setTypeConfiguration( typeConfiguration );
		}
	}
}
