/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.persistence.metamodel.IdentifiableType;

import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.JoinedTableBinding;
import org.hibernate.metamodel.model.relational.spi.Table;

/**
 * Hibernate extension SPI for working with {@link IdentifiableType} implementations, which includes
 * both mapped-superclasses {@link MappedSuperclassTypeDescriptor}
 * and {@link EntityTypeDescriptor}
 *
 * @author Steve Ebersole
 */
public interface IdentifiableTypeDescriptor<T> extends InheritanceCapable<T>, IdentifiableDomainType<T> {
	@Override
	default IdentifiableTypeDescriptor<? super T> getSupertype() {
		return getSuperclassType();
	}

	@Override
	IdentifiableTypeDescriptor<? super T> getSuperclassType();

	@Override
	SimpleTypeDescriptor<?> getIdType();

	EntityHierarchy getHierarchy();

	interface InFlightAccess<X> extends ManagedTypeDescriptor.InFlightAccess<X> {
		void addSubTypeDescriptor(IdentifiableTypeDescriptor subTypeDescriptor);
	}

	@Override
	InFlightAccess<T> getInFlightAccess();

	void visitSubTypeDescriptors(Consumer<IdentifiableTypeDescriptor<? extends T>> action);
	void visitAllSubTypeDescriptors(Consumer<IdentifiableTypeDescriptor<? extends T>> action);

	IdentifiableTypeDescriptor findMatchingSubTypeDescriptors(Predicate<IdentifiableTypeDescriptor<? extends T>> matcher);

	default void visitConstraintOrderedTables(BiConsumer<Table, List<Column>> tableConsumer) {
		for ( JoinedTableBinding secondaryTableBinding : getSecondaryTableBindings() ) {
			tableConsumer.accept(
					secondaryTableBinding.getReferringTable(),
					secondaryTableBinding.getJoinForeignKey().getColumnMappings().getReferringColumns()
			);
		}

		final Table primaryTable = getPrimaryTable();
		if ( primaryTable != null ) {
			tableConsumer.accept( primaryTable, (List) primaryTable.getPrimaryKey().getColumns() );
		}
	}

	/**
	 * Access to the root table for this type.
	 */
	default Table getPrimaryTable() {
		return null;
	}

	/**
	 * Access to all "declared" secondary table mapping info for this type, not including
	 * secondary tables defined for super-types nor sub-types
	 */
	default List<JoinedTableBinding> getSecondaryTableBindings() {
		return Collections.emptyList();
	}

	/**
	 * Access to information about the entity identifier, specifically relative to this
	 * entity (in terms of Java parameterized type signature in regards to all "id attributes").
	 * Generally this delegates to {@link EntityHierarchy#getIdentifierDescriptor()} via
	 * {@link #getHierarchy()}.  We'd want to override the value coming from
	 * {@link EntityHierarchy#getIdentifierDescriptor()} in cases where we have a
	 * MappedSuperclass to the root entity and that MappedSuperclass defines the identifier
	 * (or any attributes really) using a parameterized type signature where the attribute
	 * type has not been concretely bound and is instead bound on the root entity.
	 *
	 * @todo (6.0) : we should consider doing the same for normal attributes (and version?) as well
	 * in terms of cases where generic type parameters for an entity hierarchy have not
	 * been bound as of the root entity
	 * @todo (6.0) : how should we handle the attribute as defined on the MappedSuperclass and the attribute defined on the subclass (with the conretely bound parameter type)?
	 * I mean specifically.. do we mark the MappedSuperclass attributes (somehow) as having an "unbound" attribute type and
	 * mark the subclass attribute as being a "bridged" attribute?
	 * @since 6.0
	 */
	default EntityIdentifier getIdentifierDescriptor(){
		return getHierarchy().getIdentifierDescriptor();
	}
}
