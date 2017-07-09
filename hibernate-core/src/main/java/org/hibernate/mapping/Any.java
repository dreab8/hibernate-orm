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
import org.hibernate.type.BasicType;
import org.hibernate.type.MetaType;
import org.hibernate.type.Type;

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

	public void addColumn(Column column, boolean isInsertable, boolean isUpdatable) {
		int index = columns.indexOf( column );
		if ( index == -1 ) {
			columns.add(column);
			insertability.add( isInsertable );
			updatability.add( isUpdatable );
		}
		else {
			if ( insertability.get( index ) != isInsertable ) {
				throw new IllegalStateException( "Same column is added more than once with different values for isInsertable" );
			}
			if ( updatability.get( index ) != isUpdatable ) {
				throw new IllegalStateException( "Same column is added more than once with different values for isUpdatable" );
			}
		}
		column.setSqlTypeCodeResolver( new ColumnSqlTypeCodeResolverImpl( columns.size() - 1  ) );
	}

	public class ColumnSqlTypeCodeResolverImpl implements ColumnSqlTypeCodeResolver {
		BasicType[] basicTypes = new BasicType[2];

		private int index;

		public ColumnSqlTypeCodeResolverImpl(int index) {
			this.index = index;
			basicTypes[0] = (BasicType) getMetadata().getTypeResolver().heuristicType( metaTypeName );
			basicTypes[1] = (BasicType) getMetadata().getTypeResolver().heuristicType( identifierTypeName );
		}

		@Override
		public int resolveCode() {
			return basicTypes[index].getColumnSpan( getMetadata() );
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
