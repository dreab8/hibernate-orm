/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.spi.java.managed;

import javax.persistence.metamodel.Attribute;

/**
 * @author Steve Ebersole
 */
public interface AttributeDeclarer {
	void attributeBuilt(Attribute attribute);
}
