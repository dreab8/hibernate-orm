/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.type.AbstractStandardBasicType;
import org.hibernate.type.BasicType;
import org.hibernate.type.MetaType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * A Hibernate "any" type (ie. polymorphic association to
 * one-of-several tables).
 * @author Gavin King
 */
public class Any extends SimpleValue {
	private String identifierTypeName;
	private String metaTypeName = "string";
	private Map metaValues;

	public Any(MetadataImplementor metadata, Table table) {
		super( metadata, table );
	}

	public String getIdentifierType() {
		return identifierTypeName;
	}

	public void setIdentifierType(String identifierType) {
		this.identifierTypeName = identifierType;
	}

	public Type getType() throws MappingException {
		final BasicType metaType = (BasicType) getMetadata().getTypeResolver().heuristicType( metaTypeName );

		return getMetadata().getTypeResolver().getTypeFactory().any(
				metaValues == null ? metaType : new MetaType( metaValues, metaType ),
				getMetadata().getTypeResolver().heuristicType( identifierTypeName )
		);
	}

	public void setSqlTypeDescriptorResolver(Column column) {
		column.setSqlTypeDescriptorResolver( new AnySqlTypeDescriptorResolver( columns.size() - 1 ) );
	}

	public class AnySqlTypeDescriptorResolver implements SqlTypeDescriptorResolver {
		AbstractStandardBasicType[] basicTypes = new AbstractStandardBasicType[2];

		private int index;

		public AnySqlTypeDescriptorResolver(int index) {
			this.index = index;
			basicTypes[0] = (AbstractStandardBasicType) getMetadata().getTypeResolver().heuristicType( metaTypeName );
			basicTypes[1] = (AbstractStandardBasicType) getMetadata().getTypeResolver().heuristicType(
					identifierTypeName );
		}

		@Override
		public SqlTypeDescriptor resolveSqlTypeDescriptor() {
			return basicTypes[index].getSqlTypeDescriptor();
		}
	}

	public String getMetaType() {
		return metaTypeName;
	}

	public void setMetaType(String type) {
		metaTypeName = type;
	}

	public Map getMetaValues() {
		return metaValues;
	}

	public void setMetaValues(Map metaValues) {
		this.metaValues = metaValues;
	}

	public void setTypeUsingReflection(String className, String propertyName)
		throws MappingException {
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
}
