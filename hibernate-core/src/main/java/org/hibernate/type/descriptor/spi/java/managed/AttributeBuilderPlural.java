/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.spi.java.managed;

import javax.persistence.metamodel.PluralAttribute;

import org.hibernate.type.descriptor.spi.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public interface AttributeBuilderPlural extends AttributeBuilder<PluralAttribute> {
	void setCollectionType(JavaTypeDescriptor collectionType);
}
