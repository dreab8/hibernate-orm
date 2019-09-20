/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

/**
 * Access to a group of ModelPart by name or for iteration
 *
 * @author Steve Ebersole
 */
public interface ModelPartContainer extends ModelPart, Queryable {
}
