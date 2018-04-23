/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedIndex;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.metamodel.model.relational.spi.PhysicalNamingStrategy;

/**
 * A relational table index
 *
 * @author Gavin King
 */
public class Index implements MappedIndex, Exportable, Serializable {
	private Table table;
	private java.util.List<Column> columns = new ArrayList<Column>();
	private java.util.Map<Column, String> columnOrderMap = new HashMap<Column, String>();
	private Identifier name;

	public Table getTable() {
		return table;
	}

	@Override
	public void setTable(MappedTable table) {
		this.table = (Table) table;
	}

	/**
	 * @deprecated since 6.0, use {@link #setTable(MappedTable)} instead.
	 */
	@Deprecated
	public void setTable(Table table) {
		this.table = table;
	}

	public int getColumnSpan() {
		return columns.size();
	}

	/**
	 * @deprecated since 6.0, use {@link #getColumns()}
	 */
	@Deprecated
	public Iterator<Column> getColumnIterator() {
		return columns.iterator();
	}

	public java.util.Map<Column, String> getColumnOrderMap() {
		return Collections.unmodifiableMap( columnOrderMap );
	}

	public void addColumn(Column column) {
		if ( !columns.contains( column ) ) {
			columns.add( column );
		}
	}

	public void addColumn(Column column, String order) {
		addColumn( column );
		if ( StringHelper.isNotEmpty( order ) ) {
			columnOrderMap.put( column, order );
		}
	}

	@Override
	public void addColumns(List<? extends MappedColumn> columns) {
		for ( MappedColumn mappedColumn : columns ) {
			addColumn( (Column) mappedColumn );
		}
	}

	@Override
	public List<Column> getColumns() {
		return columns;
	}

	/**
	 * @deprecated since 6.0, use {@link #addColumns(List<? extends MappedColumn>()}.
	 */
	@Deprecated
	public void addColumns(Iterator extraColumns) {
		while ( extraColumns.hasNext() ) {
			addColumn( (Column) extraColumns.next() );
		}
	}

	public boolean containsColumn(Column column) {
		return columns.contains( column );
	}

	public String getName() {
		return name == null ? null : name.getText();
	}

	public void setName(String name) {
		this.name = Identifier.toIdentifier( name );
	}

	public String getQuotedName(Dialect dialect) {
		return name == null ? null : name.render( dialect );
	}

	@Override
	public String toString() {
		return getClass().getName() + "(" + getName() + ")";
	}

	@Override
	public String getExportIdentifier() {
		return StringHelper.qualify( getTable().getName(), "IDX-" + getName() );
	}

	public org.hibernate.metamodel.model.relational.spi.Index generateRuntimeIndex(
			ExportableTable runtimeTable,
			PhysicalNamingStrategy namingStrategy,
			JdbcEnvironment jdbcEnvironment) {
		org.hibernate.metamodel.model.relational.spi.Index index = new org.hibernate.metamodel.model.relational.spi.Index( name, runtimeTable );
		for ( Column column : columns ) {
			index.addColumn( column.generateRuntimeColumn( runtimeTable, namingStrategy, jdbcEnvironment ) );
		}
		return index;
	}
}
