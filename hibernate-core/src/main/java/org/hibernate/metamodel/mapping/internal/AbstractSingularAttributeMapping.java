/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.HibernateException;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.entity.EntityResultGraphNode;
import org.hibernate.sql.results.internal.domain.BiDirectionalFetchImpl;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSingularAttributeMapping
		extends AbstractStateArrayContributorMapping
		implements SingularAttributeMapping {

	private final PropertyAccess propertyAccess;

	public AbstractSingularAttributeMapping(
			String name,
			int stateArrayPosition,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			FetchStrategy mappedFetchStrategy,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess) {
		super( name, attributeMetadataAccess, mappedFetchStrategy, stateArrayPosition, declaringType );
		this.propertyAccess = propertyAccess;
	}

	@Override
	public PropertyAccess getPropertyAccess() {
		return propertyAccess;
	}

	protected Fetch createBiDirectionalFetch(NavigablePath fetchablePath, FetchParent fetchParent) {
		final EntityResultGraphNode referencedEntityReference = resolveEntityGraphNode( fetchParent );

		if ( referencedEntityReference == null ) {
			throw new HibernateException(
					"Could not locate entity-valued reference for circular path `" + fetchablePath + "`"
			);
		}

		return new BiDirectionalFetchImpl(
				FetchTiming.IMMEDIATE,
				fetchablePath,
				fetchParent,
				this,
				referencedEntityReference.getNavigablePath()
		);
	}

	protected EntityResultGraphNode resolveEntityGraphNode(FetchParent fetchParent) {
		FetchParent processingParent = fetchParent;
		while ( processingParent != null ) {
			if ( processingParent instanceof EntityResultGraphNode ) {
				return (EntityResultGraphNode) processingParent;
			}

			if ( processingParent instanceof Fetch ) {
				processingParent = ( (Fetch) processingParent ).getFetchParent();
				continue;
			}

			processingParent = null;
		}

		return null;
	}

}
