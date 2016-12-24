/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.entity.internal;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.persister.common.spi.AbstractSingularAttribute;
import org.hibernate.persister.common.spi.Attribute;
import org.hibernate.persister.common.spi.AttributeContainer;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.JoinColumnMapping;
import org.hibernate.persister.common.spi.SingularAttribute;
import org.hibernate.persister.common.spi.VirtualAttribute;
import org.hibernate.persister.embeddable.spi.EmbeddablePersister;
import org.hibernate.persister.entity.spi.EntityHierarchy;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.persister.entity.spi.IdentifierDescriptor;
import org.hibernate.sqm.domain.EntityReference;
import org.hibernate.type.spi.CompositeType;

/**
 * @author Steve Ebersole
 */
public class IdentifierDescriptorCompositeNonAggregated
		extends AbstractSingularAttribute<CompositeType>
		implements IdentifierDescriptor, SingularAttribute, AttributeContainer, VirtualAttribute {
	// todo : IdClass handling eventually

	private final EmbeddablePersister embeddablePersister;

	public IdentifierDescriptorCompositeNonAggregated(EntityHierarchy entityHierarchy, EmbeddablePersister embeddablePersister) {
		super( entityHierarchy.getRootEntityPersister(), "<id>", embeddablePersister.getOrmType(), false );
		this.embeddablePersister = embeddablePersister;
	}

	@Override
	public List<Column> getColumns() {
		return embeddablePersister.collectColumns();
	}

	@Override
	public CompositeType getIdType() {
		return embeddablePersister.getOrmType();
	}

	@Override
	public boolean hasSingleIdAttribute() {
		return false;
	}

	@Override
	public SingularAttribute getIdAttribute() {
		return this;
	}

	@Override
	public EntityPersister getAttributeContainer() {
		return (EntityPersister) super.getAttributeContainer();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SingularAttributeImplementor


	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.EMBEDDED;
	}

	@Override
	public String getAttributeName() {
		return "<id>";
	}

	@Override
	public String asLoggableText() {
		return "IdentifierCompositeNonAggregated(" + getAttributeContainer().asLoggableText() + ")";
	}

	@Override
	public AttributeContainer getSuperAttributeContainer() {
		// composite inheritance not supported
		return null;
	}

	@Override
	public List<Attribute> getNonIdentifierAttributes() {
		return Collections.emptyList();
	}

	@Override
	public Attribute findAttribute(String name) {
		return embeddablePersister.findAttribute( name );
	}

	@Override
	public List<JoinColumnMapping> resolveJoinColumnMappings(Attribute attribute) {
		return getAttributeContainer().resolveJoinColumnMappings( attribute );
	}

	@Override
	public Optional<EntityReference> toEntityReference() {
		return Optional.empty();
	}
}
