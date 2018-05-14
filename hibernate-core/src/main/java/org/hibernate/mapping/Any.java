/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.JavaTypeMapping;
import org.hibernate.boot.model.type.spi.BasicTypeResolver;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.type.MetaType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * A Hibernate "any" type (ie. polymorphic association to
 * one-of-several tables).
 * @author Gavin King
 */
public class Any extends SimpleValue {
	private String identifierTypeName;
	private String metaTypeName = "string";

	private BasicTypeResolver keyTypeResolver;

	private BasicTypeResolver discriminatorTypeResolver;

	private Map<Object,String> discriminatorMap;

	/**
	 * @deprecated Use {@link Any#Any(MetadataBuildingContext, Table)} instead.
	 */
	@Deprecated
	public Any(MetadataImplementor metadata, Table table) {
		super( metadata, table );
	}

	public Any(MetadataBuildingContext buildingContext, Table table) {
		super( buildingContext, table );
	}

	public String getIdentifierType() {
		return identifierTypeName;
	}

	public void setIdentifierType(String identifierType) {
		this.identifierTypeName = identifierType;
	}

	public Type getType() throws MappingException {
		final Type metaType = getMetadata().getTypeResolver().heuristicType( metaTypeName );

		return getMetadata().getTypeResolver().getTypeFactory().any(
				discriminatorMap == null ? metaType : new MetaType( discriminatorMap, metaType ),
				getMetadata().getTypeResolver().heuristicType( identifierTypeName )
		);
	}

	public void setTypeByReflection(String propertyClass, String propertyName) {}

	public String getMetaType() {
		return metaTypeName;
	}

	public void setMetaType(String type) {
		metaTypeName = type;
	}

	public Map getMetaValues() {
		return discriminatorMap;
	}

	public void setMetaValues(Map<Object,String> metaValues) {
		this.discriminatorMap = metaValues;
	}

	public void setTypeUsingReflection(String className, String propertyName)
		throws MappingException {
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	public void setIdentifierTypeResolver(BasicTypeResolver keyTypeResolver) {
		this.keyTypeResolver = keyTypeResolver;
	}

	public void setDiscriminatorTypeResolver(BasicTypeResolver discriminatorTypeResolver) {
		this.discriminatorTypeResolver = discriminatorTypeResolver;
	}

	public void addDiscriminatorMapping(Object discriminatorValue, String mappedEntityName) {
		if ( discriminatorMap == null ) {
			discriminatorMap = new HashMap<>();
		}
		discriminatorMap.put( discriminatorValue, mappedEntityName );
	}

	@Override
	protected void setTypeDescriptorResolver(Column column) {
		column.setTypeDescriptorResolver( new AnyTypeDescriptorResolver( columns.size() - 1  ) );
	}

	@Override
	public JavaTypeMapping getJavaTypeMapping() {
		return null;
	}

	public class AnyTypeDescriptorResolver implements TypeDescriptorResolver {
		BasicTypeResolver[] typesResolvers = new BasicTypeResolver[2];

		private int index;

		public AnyTypeDescriptorResolver(int index) {
			this.index = index;
			typesResolvers[0] = discriminatorTypeResolver;
			typesResolvers[1] = keyTypeResolver;
		}

		@Override
		public SqlTypeDescriptor resolveSqlTypeDescriptor() {
			return typesResolvers[index].resolveBasicType().getSqlTypeDescriptor();
		}

		@Override
		public JavaTypeDescriptor resolveJavaTypeDescriptor() {
			return typesResolvers[index].resolveBasicType().getJavaTypeDescriptor();
		}
	}

	@Override
	public boolean isSame(SimpleValue other) {
		return other instanceof Any && isSame( (Any) other );
	}

	public boolean isSame(Any other) {
		return super.isSame( other )
				&& Objects.equals( identifierTypeName, other.identifierTypeName )
				&& Objects.equals( metaTypeName, other.metaTypeName )
				&& Objects.equals( discriminatorMap, other.discriminatorMap );
	}
}
