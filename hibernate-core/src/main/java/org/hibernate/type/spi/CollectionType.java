/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.spi;

import org.hibernate.persister.collection.spi.CollectionPersister;

/**
 * @author Steve Ebersole
 */
public interface CollectionType extends Type {
	<O,C,E> CollectionPersister<O,C,E> getCollectionPersister();

	@Override
	default Classification getClassification() {
		return Classification.COLLECTION;
	}

	@Override
	default Class getJavaType() {
		return getCollectionPersister().getJavaType();
	}

	@Override
	default String asLoggableText() {
		return getCollectionPersister().asLoggableText();
	}

	@Override
	default JdbcLiteralFormatter getJdbcLiteralFormatter() {
		return null;
	}
}
