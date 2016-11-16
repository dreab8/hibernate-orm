/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.common.internal;

import org.hibernate.persister.common.spi.AbstractSingularAttribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.sqm.domain.EntityType;
import org.hibernate.sqm.domain.ManagedType;
import org.hibernate.sqm.domain.SingularAttribute;

/**
 * @author Steve Ebersole
 */
public class SingularAttributeEntity
		extends AbstractSingularAttribute<org.hibernate.type.EntityType> {
	private final SingularAttribute.Classification classification;
	private final Column[] columns;
	private final EntityType sqmType;

	public SingularAttributeEntity(
			ManagedType declaringType,
			String name,
			SingularAttribute.Classification classification,
			EntityType sqmType,
			Column[] columns) {
		super( declaringType, name, sqmType );
		this.sqmType = sqmType;
		this.classification = classification;
		this.columns = columns;
	}

	@Override
	public SingularAttribute.Classification getAttributeTypeClassification() {
		return classification;
	}

	public Column[] getColumns() {
		return columns;
	}

	@Override
	public boolean isId() {
		return false;
	}

	@Override
	public boolean isVersion() {
		return false;
	}

	@Override
	public EntityType asManagedType() {
		return sqmType;
	}
}
