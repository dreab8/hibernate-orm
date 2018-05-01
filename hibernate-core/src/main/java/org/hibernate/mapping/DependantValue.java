/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.JavaTypeMapping;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * A value which is "typed" by reference to some other
 * value (for example, a foreign key is typed by the
 * referenced primary key).
 *
 * @author Gavin King
 */
public class DependantValue extends SimpleValue {
	private KeyValue wrappedValue;
	private boolean nullable;
	private boolean updateable;
	private boolean isNationalized;

	/**
	 * @deprecated Use {@link DependantValue#DependantValue(MetadataBuildingContext, Table, KeyValue)} instead.
	 */
	@Deprecated
	public DependantValue(MetadataImplementor metadata, Table table, KeyValue prototype) {
		super( metadata, table );
		this.wrappedValue = prototype;
	}

	public DependantValue(MetadataBuildingContext buildingContext, Table table, KeyValue prototype) {
		super( buildingContext, table );
		this.wrappedValue = prototype;
	}

	public Type getType() throws MappingException {
		return wrappedValue.getType();
	}

	@Override
	protected void setTypeDescriptorResolver(Column column) {
		column.setTypeDescriptorResolver( new DependantValueTypeDescriptorResolver( columns.size() - 1 ) );
	}

	@Override
	public JavaTypeMapping getJavaTypeMapping() {
		return wrappedValue.getJavaTypeMapping();
	}

	public class DependantValueTypeDescriptorResolver implements TypeDescriptorResolver {
		private int index;

		public DependantValueTypeDescriptorResolver(int index) {
			this.index = index;
		}

		@Override
		public SqlTypeDescriptor resolveSqlTypeDescriptor() {
			return ( (Column) wrappedValue.getColumns().get( index ) ).getSqlTypeDescriptor();
		}

		@Override
		public JavaTypeDescriptor resolveJavaTypeDescriptor() {
			return wrappedValue.getJavaTypeMapping().resolveJavaTypeDescriptor();
		}
	}

	public void setTypeUsingReflection(String className, String propertyName) {}
	
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	public boolean isNullable() {
		return nullable;
	}
	
	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}
	
	public boolean isUpdateable() {
		return updateable;
	}
	
	public void setUpdateable(boolean updateable) {
		this.updateable = updateable;
	}

	public void makeNationalized() {
		this.isNationalized = true;
	}

	public boolean isNationalized() {
		return isNationalized;
	}

	@Override
	public boolean isSame(SimpleValue other) {
		return other instanceof DependantValue && isSame( (DependantValue) other );
	}

	public boolean isSame(DependantValue other) {
		return super.isSame( other )
				&& isSame( wrappedValue, other.wrappedValue );
	}

}
