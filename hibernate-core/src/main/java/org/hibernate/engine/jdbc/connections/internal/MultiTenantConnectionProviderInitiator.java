/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.DataSourceBasedMultiTenantConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.jboss.logging.Logger;

import static org.hibernate.engine.jdbc.connections.internal.ConnectionConfigurationHelper.getDataSource;

/**
 * A service initiator for the {@link MultiTenantConnectionProvider} service.
 *
 * @author Steve Ebersole
 */
public class MultiTenantConnectionProviderInitiator implements StandardServiceInitiator<MultiTenantConnectionProvider<?>> {
	private static final Logger log = Logger.getLogger( MultiTenantConnectionProviderInitiator.class );

	/**
	 * Singleton access
	 */
	public static final MultiTenantConnectionProviderInitiator INSTANCE = new MultiTenantConnectionProviderInitiator();

	@Override
	public Class<MultiTenantConnectionProvider<?>> getServiceInitiated() {
		//noinspection unchecked
		return (Class<MultiTenantConnectionProvider<?>>) (Class<?>) MultiTenantConnectionProvider.class;
	}

	@Override
	public MultiTenantConnectionProvider<?> initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		if ( !configurationValues.containsKey( AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER ) ) {
			// nothing to do, but given the separate hierarchies have to handle this here.
			return null;
		}

		final Object configValue = configurationValues.get( AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER );
		if ( configValue == null ) {
			// if they also specified the data source *name*, then lets assume they want
			// DataSourceBasedMultiTenantConnectionProviderImpl
			final Object dataSourceConfigValue = getDataSource( configurationValues );
			if ( dataSourceConfigValue instanceof String ) {
				return new DataSourceBasedMultiTenantConnectionProviderImpl<>();
			}

			return null;
		}

		if ( configValue instanceof MultiTenantConnectionProvider<?> ) {
			return (MultiTenantConnectionProvider<?>) configValue;
		}
		else {
			final Class<MultiTenantConnectionProvider<?>> implClass;
			if ( configValue instanceof Class ) {
				@SuppressWarnings("unchecked")
				Class<MultiTenantConnectionProvider<?>> clazz = (Class<MultiTenantConnectionProvider<?>>) configValue;
				implClass = clazz;
			}
			else {
				final String className = configValue.toString();
				final ClassLoaderService classLoaderService = registry.getService( ClassLoaderService.class );
				try {
					implClass = classLoaderService.classForName( className );
				}
				catch (ClassLoadingException cle) {
					log.warn( "Unable to locate specified class [" + className + "]", cle );
					throw new ServiceException( "Unable to locate specified multi-tenant connection provider [" + className + "]" );
				}
			}

			try {
				return implClass.newInstance();
			}
			catch (Exception e) {
				log.warn( "Unable to instantiate specified class [" + implClass.getName() + "]", e );
				throw new ServiceException( "Unable to instantiate specified multi-tenant connection provider [" + implClass.getName() + "]" );
			}
		}
	}
}
