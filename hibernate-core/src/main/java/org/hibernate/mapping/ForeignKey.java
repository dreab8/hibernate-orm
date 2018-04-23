/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedForeignKey;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.internal.util.JavaTypeHelper;

/**
 * A foreign key constraint
 *
 * @author Gavin King
 */
public class ForeignKey extends Constraint implements MappedForeignKey {
	private MappedTable referencedTable;
	private String referencedEntityName;
	private String keyDefinition;
	private boolean cascadeDeleteEnabled;
	private List<Column> referencedColumns = new ArrayList<Column>();
	private boolean creationEnabled = true;

	public ForeignKey() {
	}

	@Override
	public void disableCreation() {
		creationEnabled = false;
	}

	@Override
	public boolean isCreationEnabled() {
		return creationEnabled;
	}

	@Override
	public void setName(String name) {
		super.setName( name );
		// the FK name "none" is a magic value in the hbm.xml binding that indicated to
		// not create a FK.
		if ( "none".equals( name ) ) {
			disableCreation();
		}
	}

	@Override
	public MappedTable getReferencedTable() {
		return referencedTable;
	}

	private void appendColumns(StringBuilder buf, List<Selectable> columns) {
		boolean firstPass = true;
		for ( Selectable column : columns ) {
			if ( firstPass ) {
				firstPass = false;
			}
			else {
				buf.append( ',' );
			}
			buf.append( column.getText() );
		}
	}

	@Override
	public void setReferencedTable(MappedTable referencedTable) throws MappingException {
		this.referencedTable = referencedTable;
	}

	/**
	 * Validates that columnspan of the foreignkey and the primarykey is the same.
	 * <p/>
	 * Furthermore it aligns the length of the underlying tables columns.
	 */
	@Override
	public void alignColumns() {
		if ( isReferenceToPrimaryKey() ) {
			alignColumns( referencedTable );
		}
	}

	private void alignColumns(MappedTable referencedTable) {
		final List<Selectable> columns = JavaTypeHelper.cast( getColumns() );
		final List<Selectable> targetColumns = JavaTypeHelper.cast( referencedTable.getMappedPrimaryKey().getColumns() );

		final int referencedPkColumnSpan = targetColumns.size();

		if ( referencedPkColumnSpan != columns.size() ) {
			StringBuilder sb = new StringBuilder();
			sb.append( "Foreign key (" ).append( getName() ).append( ":" )
					.append( getMappedTable().getName() )
					.append( " [" );
			appendColumns( sb, columns );
			sb.append( "])" )
					.append( ") must have same number of columns as the referenced primary key (" )
					.append( referencedTable.getName() )
					.append( " [" );
			appendColumns( sb, targetColumns );
			sb.append( "])" );
			throw new MappingException( sb.toString() );
		}

		for ( int i = 0; i < columns.size(); i++ ) {
			if ( columns.get( i ) instanceof Column && targetColumns.get( i ) instanceof Column ) {
				( (Column) columns.get( i ) ).setLength(
						( (Column) targetColumns.get( i ) ).getLength()
				);
			}
		}
	}

	@Override
	public String getReferencedEntityName() {
		return referencedEntityName;
	}

	@Override
	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName;
	}

	@Override
	public String getKeyDefinition() {
		return keyDefinition;
	}

	@Override
	public void setKeyDefinition(String keyDefinition) {
		this.keyDefinition = keyDefinition;
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return cascadeDeleteEnabled;
	}

	@Override
	public void setCascadeDeleteEnabled(boolean cascadeDeleteEnabled) {
		this.cascadeDeleteEnabled = cascadeDeleteEnabled;
	}

	@Override
	public boolean isPhysicalConstraint() {
		return referencedTable.isPhysicalTable()
				&& getTable().isPhysicalTable()
				&& !referencedTable.hasDenormalizedTables();
	}

	/**
	 * Returns the referenced columns if the foreignkey does not refer to the primary key
	 */
	@Override
	public List getReferencedColumns() {
		return referencedColumns;
	}

	@Override
	public List<Column> getTargetColumns() {
		if ( referencedColumns != null && !referencedColumns.isEmpty() ) {
			return referencedColumns;
		}
		else {
			return getReferencedTable().getMappedPrimaryKey().getColumns();
		}
	}
	/**
	 * Does this foreignkey reference the primary key of the reference table
	 */
	@Override
	public boolean isReferenceToPrimaryKey() {
		return referencedColumns.isEmpty();
	}

	/**
	 * @deprecated since 6.0, use {@link #addReferencedColumns(List<? extends MappedColumn >()}.
	 */
	@Deprecated
	public void addReferencedColumns(Iterator referencedColumnsIterator) {
		while ( referencedColumnsIterator.hasNext() ) {
			Selectable col = (Selectable) referencedColumnsIterator.next();
			if ( !col.isFormula() ) {
				addReferencedColumn( (Column) col );
			}
		}
	}

	@Override
	public void addReferencedColumns(List<? extends MappedColumn> referencedColumns) {
		addReferencedColumns( referencedColumns.iterator() );
	}

	private void addReferencedColumn(Column column) {
		if ( !referencedColumns.contains( column ) ) {
			referencedColumns.add( column );
		}
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"Boot-model ForeignKey[ (%s) => (%s) ]",
				getColumns(),
				getReferencedColumns()
		);
	}

	@Override
	public String generatedConstraintNamePrefix() {
		return "FK_";
	}
}
