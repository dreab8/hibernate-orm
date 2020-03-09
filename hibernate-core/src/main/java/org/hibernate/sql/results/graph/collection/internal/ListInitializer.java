/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.collection.internal.PersistentList;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * CollectionInitializer for PersistentList loading
 * 
 * @author Steve Ebersole
 */
public class ListInitializer extends AbstractImmediateCollectionInitializer {
	private final DomainResultAssembler listIndexAssembler;
	private final DomainResultAssembler elementAssembler;

	private final int listIndexBase;

	public ListInitializer(
			NavigablePath navigablePath,
			PluralAttributeMapping attributeMapping,
			FetchParentAccess parentAccess,
			LockMode lockMode,
			DomainResultAssembler keyContainerAssembler,
			DomainResultAssembler keyCollectionAssembler,
			DomainResultAssembler listIndexAssembler,
			DomainResultAssembler elementAssembler) {
		super( navigablePath, attributeMapping, parentAccess, lockMode, keyContainerAssembler, keyCollectionAssembler );
		this.listIndexAssembler = listIndexAssembler;
		this.elementAssembler = elementAssembler;

		listIndexBase = attributeMapping.getIndexMetadata().getListIndexBase();
	}

	@Override
	public PersistentList getCollectionInstance() {
		return (PersistentList) super.getCollectionInstance();
	}

	@Override
	protected void readCollectionRow(
			CollectionKey collectionKey,
			List loadingState,
			RowProcessingState rowProcessingState) {
		int index = (int) listIndexAssembler.assemble( rowProcessingState );

		if ( listIndexBase != 0 ) {
			index -= listIndexBase;
		}

		for ( int i = loadingState.size(); i <= index; ++i ) {
			//noinspection unchecked
			loadingState.add( i, null );
		}

		//noinspection unchecked
		loadingState.set( index, elementAssembler.assemble( rowProcessingState ) );
	}

	@Override
	public String toString() {
		return "ListInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}
}
