/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

import org.hibernate.Incubating;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.service.ServiceRegistry;

/**
 * The context in which all SQM creations occur (think SessionFactory).
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SqmCreationContext {
	/**
	 * Access to the domain model metadata
	 */
	MetamodelImplementor getDomainModel();

	/**
	 * Access to the ServiceRegistry for the context
	 */
	ServiceRegistry getServiceRegistry();

	QueryEngine getQueryEngine();

	default NodeBuilder getNodeBuilder() {
		return getQueryEngine().getCriteriaBuilder();
	}
}
