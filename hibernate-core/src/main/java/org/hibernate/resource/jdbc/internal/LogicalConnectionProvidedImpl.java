/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.resource.jdbc.internal;

import java.sql.Connection;

import org.hibernate.resource.jdbc.LogicalConnection;
import org.hibernate.resource.jdbc.ResourceRegistry;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class LogicalConnectionProvidedImpl extends AbstractLogicalConnectionImplementor {
	private static final Logger log = Logger.getLogger( LogicalConnection.class );

	private Connection providedConnection;
	private final boolean initiallyAutoCommit;

	public LogicalConnectionProvidedImpl(Connection providedConnection) {
		this( providedConnection, new ResourceRegistryStandardImpl() );
	}

	public LogicalConnectionProvidedImpl(Connection providedConnection, ResourceRegistry resourceRegistry) {
		this.resourceRegistry = resourceRegistry;
		if ( providedConnection == null ) {
			throw new IllegalArgumentException( "Provided Connection cannot be null" );
		}

		this.providedConnection = providedConnection;
		this.initiallyAutoCommit = determineInitialAutoCommitMode( providedConnection );
	}

	@Override
	public boolean isOpen() {
		return providedConnection != null;
	}

	@Override
	public Connection close() {
		log.trace( "Closing logical connection" );

		getResourceRegistry().releaseResources();

		try {
			return providedConnection;
		}
		finally {
			providedConnection = null;
			log.trace( "Logical connection closed" );
		}
	}

	@Override
	public boolean isPhysicallyConnected() {
		return isOpen();
	}

	@Override
	public Connection getPhysicalConnection() {
		errorIfClosed();

		return providedConnection;
	}

	@Override
	public LogicalConnectionImplementor makeShareableCopy() {
		errorIfClosed();

		return new LogicalConnectionProvidedImpl( providedConnection );
	}

	@Override
	public Connection manualDisconnect() {
		errorIfClosed();
		try {
			resourceRegistry.releaseResources();
			return providedConnection;
		}
		finally {
			this.providedConnection = null;
		}
	}

	@Override
	public void manualReconnect(Connection connection) {
		errorIfClosed();

		if ( connection == null ) {
			throw new IllegalArgumentException( "cannot reconnect using a null connection" );
		}
		else if ( connection == providedConnection ) {
			// likely an unmatched reconnect call (no matching disconnect call)
			log.debug( "reconnecting the same connection that is already connected; should this connection have been disconnected?" );
		}
		else if ( providedConnection != null ) {
			throw new IllegalArgumentException(
					"cannot reconnect to a new user-supplied connection because currently connected; must disconnect before reconnecting."
			);
		}
		providedConnection = connection;
		log.debug( "Manually reconnected logical connection" );
	}

	@Override
	protected Connection getConnectionForTransactionManagement() {
		return providedConnection;
	}

	@Override
	protected void afterCompletion() {
		afterTransaction();

		resetConnection( initiallyAutoCommit );
	}
}
