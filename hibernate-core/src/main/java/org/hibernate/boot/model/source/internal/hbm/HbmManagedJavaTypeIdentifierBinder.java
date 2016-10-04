/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.NotYetImplementedException;

/**
 * @author Chris Cranford
 */
public class HbmManagedJavaTypeIdentifierBinder {

	private final MetadataBuildingContext metadataBuildingContext;

	public HbmManagedJavaTypeIdentifierBinder(MetadataBuildingContext metadataBuildingContext) {
		this.metadataBuildingContext = metadataBuildingContext;
	}

	public void bindIdentifier(final EntityHierarchySourceImpl hierarchySource) {
		throw new NotYetImplementedException( "IdentifierDescriptor binding not yet implemented." );
	}
}
