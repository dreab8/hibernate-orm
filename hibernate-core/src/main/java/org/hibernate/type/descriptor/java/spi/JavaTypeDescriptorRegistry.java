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
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.internal.EnumJavaDescriptor;
import org.hibernate.type.descriptor.java.internal.JavaTypeDescriptorBaseline;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.VarbinarySqlDescriptor;
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
public class JavaTypeDescriptorRegistry implements JavaTypeDescriptorBaseline.BaselineTarget, Serializable {
	private static final Logger log = Logger.getLogger( JavaTypeDescriptorRegistry.class );


	private ConcurrentHashMap<Class, JavaTypeDescriptor> descriptorsByClass = new ConcurrentHashMap<>();
	private final TypeConfiguration typeConfiguration;
	private final ConcurrentHashMap<String, org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor> descriptorsByName = new ConcurrentHashMap<>();

	@SuppressWarnings("unused")
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

	public <T> JavaTypeDescriptor<T> getDescriptor(Class<T> javaType) {
		return RegistryHelper.INSTANCE.resolveDescriptor(
				descriptorsByClass,
				javaType,
				() -> {
					log.debugf(
							"Could not find matching scoped JavaTypeDescriptor for requested Java class [%s]; " +
									"falling back to static registry",
							javaType.getName()
					);

					return org.hibernate.type.descriptor.java.JavaTypeDescriptorRegistry.INSTANCE.getDescriptor( javaType );
				}
		);
	}

	@SuppressWarnings("unchecked")
	public <T> org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor<T> getJavaTypeDescriptor(Class<T> javaType) {
		if ( javaType == null ) {
			throw new IllegalArgumentException( "Class passed to locate Java type descriptor cannot be null" );
		}

		org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor javaTypeDescriptor = descriptorsByName.computeIfAbsent(
				javaType.getName(),
				k -> makeOnTheFlyJavaTypeDescriptor( javaType )
		);

		return javaTypeDescriptor;
	}

	public void addDescriptor(JavaTypeDescriptor descriptor) {
		JavaTypeDescriptor old = descriptorsByClass.put( descriptor.getJavaType(), descriptor );
		if ( old != null ) {
			log.debugf(
					"JavaTypeDescriptorRegistry entry replaced : %s -> %s (was %s)",
					descriptor.getJavaType(),
					descriptor,
					old
			);
		}
	}

	private void performInjections(org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor descriptor) {
		if ( descriptor instanceof TypeConfigurationAware ) {
			// would be nice to make the JavaTypeDescriptor for an entity, e.g., aware of the the TypeConfiguration
			( (TypeConfigurationAware) descriptor ).setTypeConfiguration( typeConfiguration );
		}
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


			sqlTypeDescriptor = VarbinarySqlDescriptor.INSTANCE;
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
}
