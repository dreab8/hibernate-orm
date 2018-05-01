/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain;

import java.util.List;

import org.hibernate.FetchMode;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;

/**
 *
 * @author Steve Ebersole
 */
public interface ValueMapping<J> {

	Table getTable();

	List<Selectable> getColumns();

	FetchMode getFetchMode();

	MetadataBuildingContext getMetadataBuildingContext();

	JavaTypeMapping<J> getJavaTypeMapping();

	ServiceRegistry getServiceRegistry();

}
