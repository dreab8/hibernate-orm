/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.Iterator;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;

/**
 * A one-to-one association mapping
 * @author Gavin King
 */
public class OneToOne extends ToOne {

	private boolean constrained;
	private ForeignKeyDirection foreignKeyType;
	private KeyValue identifier;
	private String propertyName;
	private String entityName;

	public OneToOne(MetadataImplementor metadata, Table table, PersistentClass owner) throws MappingException {
		super( metadata, table );
		this.identifier = owner.getKey();
		this.entityName = owner.getEntityName();
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName==null ? null : propertyName.intern();
	}
	
	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String propertyName) {
		this.entityName = entityName==null ? null : entityName.intern();
	}
	
	public Type getType() throws MappingException {
		if ( getColumnIterator().hasNext() ) {
			return getMetadata().getTypeResolver().getTypeFactory().specialOneToOne(
					getReferencedEntityName(), 
					foreignKeyType,
					referenceToPrimaryKey, 
					referencedPropertyName,
					isLazy(),
					isUnwrapProxy(),
					entityName,
					propertyName
			);
		}
		else {
			return getMetadata().getTypeResolver().getTypeFactory().oneToOne(
					getReferencedEntityName(), 
					foreignKeyType,
					referenceToPrimaryKey, 
					referencedPropertyName,
					isLazy(),
					isUnwrapProxy(),
					entityName,
					propertyName
			);
		}
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

	public class ColumnSqlTypeCodeResolverImpl implements ColumnSqlTypeCodeResolver{

		private int index;

		public ColumnSqlTypeCodeResolverImpl(int index) {
			this.index = index;
		}

		@Override
		public int resolveCode() {
			if ( getColumnIterator().hasNext() ) {
				final PersistentClass referencedPersistentClass = getMetadata().getEntityBinding(
						getReferencedEntityName() );
				if ( referenceToPrimaryKey || referencedPropertyName == null ) {
					return ( (Column) referencedPersistentClass.getIdentifier().getColumn( index ) ).getSqlTypeCode(
							getMetadata() );
				}
				else {
					final Property referencedProperty = referencedPersistentClass.getReferencedProperty(
							getReferencedPropertyName() );
					return ( (Column) referencedProperty.getValue()
							.getColumn( index ) ).getSqlTypeCode( getMetadata() );
				}
			}else{
				throw new IllegalStateException( "No SqlType code to resolve for "  + entityName);
			}
		}
	}

	public void createForeignKey() throws MappingException {
		if ( constrained && referencedPropertyName==null) {
			//TODO: handle the case of a foreign key to something other than the pk
			createForeignKeyOfEntity( getReferencedEntityName() );
		}
	}

	public java.util.List getConstraintColumns() {
		ArrayList list = new ArrayList();
		Iterator iter = identifier.getColumnIterator();
		while ( iter.hasNext() ) {
			list.add( iter.next() );
		}
		return list;
	}
	/**
	 * Returns the constrained.
	 * @return boolean
	 */
	public boolean isConstrained() {
		return constrained;
	}

	/**
	 * Returns the foreignKeyType.
	 * @return AssociationType.ForeignKeyType
	 */
	public ForeignKeyDirection getForeignKeyType() {
		return foreignKeyType;
	}

	/**
	 * Returns the identifier.
	 * @return Value
	 */
	public KeyValue getIdentifier() {
		return identifier;
	}

	/**
	 * Sets the constrained.
	 * @param constrained The constrained to set
	 */
	public void setConstrained(boolean constrained) {
		this.constrained = constrained;
	}

	/**
	 * Sets the foreignKeyType.
	 * @param foreignKeyType The foreignKeyType to set
	 */
	public void setForeignKeyType(ForeignKeyDirection foreignKeyType) {
		this.foreignKeyType = foreignKeyType;
	}

	/**
	 * Sets the identifier.
	 * @param identifier The identifier to set
	 */
	public void setIdentifier(KeyValue identifier) {
		this.identifier = identifier;
	}

	public boolean isNullable() {
		return !constrained;
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
	
}
