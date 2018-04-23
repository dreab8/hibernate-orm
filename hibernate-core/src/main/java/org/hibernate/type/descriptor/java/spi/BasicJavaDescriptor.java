/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.spi;

/**
 * @apiNote Currently this is the only high-level categorization of
 * JavaTypeDescriptor, but 6.0 will have specific JavaTypeDescriptor
 * categorizations for managed-type, mapped-superclass, identifiable-type, entity, embeddable,
 * collections.
 *
 * @author Steve Ebersole
 */
public interface BasicJavaDescriptor<T> extends JavaTypeDescriptor<T> {

}
