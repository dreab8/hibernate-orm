/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.Comparator;

import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.JavaTypeMapping;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.collection.internal.StandardMapSemantics;
import org.hibernate.collection.internal.StandardOrderedMapSemantics;
import org.hibernate.collection.internal.StandardSortedMapSemantics;
import org.hibernate.collection.spi.CollectionSemantics;

/**
 * A map has a primary key consisting of
 * the key columns + index columns.
 */
public class Map extends IndexedCollection {
	private CollectionJavaTypeMapping javaTypeMapping;

	public Map(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );
	}

	public boolean isMap() {
		return true;
	}

	public void createAllKeys() throws MappingException {
		super.createAllKeys();
		if ( !isInverse() ) {
			getIndex().createForeignKey();
		}
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	@Override
	public JavaTypeMapping getJavaTypeMapping() {
		if ( javaTypeMapping == null ) {
			javaTypeMapping = resolveJavaTypeMapping();
		}
		return javaTypeMapping;
	}

	@Override
	@SuppressWarnings("unchecked")
	public CollectionSemantics getCollectionSemantics() {
		final Comparator comparator = getComparator();
		if ( comparator != null ) {
			return StandardSortedMapSemantics.INSTANCE;
		}

		if ( hasOrder() ) {
			return StandardOrderedMapSemantics.INSTANCE;
		}

		return StandardMapSemantics.INSTANCE;
	}

	private CollectionJavaTypeMapping resolveJavaTypeMapping() {
		final Class<? extends java.util.Map> javaTypeMappingClass;

		if ( isSorted() ) {
			javaTypeMappingClass = java.util.SortedMap.class;
		}
		else if ( hasOrder() ) {
			javaTypeMappingClass = java.util.TreeMap.class;
		}
		else {
			javaTypeMappingClass = java.util.Map.class;
		}

		return new CollectionJavaTypeMapping(
				getMetadataBuildingContext().getBootstrapContext().getTypeConfiguration(),
				javaTypeMappingClass
		);
	}
}
