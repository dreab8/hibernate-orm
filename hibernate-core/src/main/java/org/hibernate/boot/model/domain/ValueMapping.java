/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain;

import java.util.List;

import org.hibernate.FetchMode;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedForeignKey;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.service.ServiceRegistry;

/**
 *
 * @author Steve Ebersole
 */
public interface ValueMapping<J> {
	MappedTable getMappedTable();

	List<MappedColumn> getMappedColumns();

	/**
	 * The foreign key this value represents.
	 */
	default ForeignKey getForeignKey() {
		return null;
	}

	default MappedForeignKey getMappedForeignKey(){
		return getForeignKey();
	}

	FetchMode getFetchMode();

	MetadataBuildingContext getMetadataBuildingContext();

	JavaTypeMapping<J> getJavaTypeMapping();

	ServiceRegistry getServiceRegistry();

}
