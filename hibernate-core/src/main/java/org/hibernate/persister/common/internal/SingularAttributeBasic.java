/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.common.internal;

import org.hibernate.persister.common.spi.AbstractSingularAttribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.type.mapper.spi.basic.BasicType;
import org.hibernate.sqm.domain.ManagedType;
import org.hibernate.sqm.domain.SingularAttribute;

/**
 * @author Steve Ebersole
 */
public class SingularAttributeBasic
		extends AbstractSingularAttribute<BasicType> {
	private final Column[] columns;

	public SingularAttributeBasic(
			ManagedType declaringType,
			String name,
			BasicType type,
			Column[] columns) {
		super( declaringType, name, type );
		this.columns = columns;
	}

	@Override
	public SingularAttribute.Classification getAttributeTypeClassification() {
		return SingularAttribute.Classification.BASIC;
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
	public ManagedType asManagedType() {
		return null;
	}
}
