/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.spi;

import org.hibernate.sqm.domain.DomainMetamodel;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public interface SqmDomainMetamodelImplementor extends DomainMetamodel {
	@Override
	BasicType resolveCastTargetType(String name);
}
