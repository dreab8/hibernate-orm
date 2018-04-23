/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.relational.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Andrea Boriero
 */
public class Index implements Exportable {
	private final ExportableTable table;
	private final Identifier name;
	private final List<PhysicalColumn> columns = new ArrayList<>();
	private Map<PhysicalColumn, String> columnsOrder = new HashMap<>();

	public Index(Identifier name, ExportableTable table) {
		this.name = name;
		this.table = table;
	}

	public void addColumn(PhysicalColumn column) {
		columns.add( column );
	}

	public void setColumnsOrder(Map<PhysicalColumn, String> columnsOrder){
		this.columnsOrder = columnsOrder;
	}

	public Identifier getName() {
		return name;
	}

	public List<PhysicalColumn> getColumns() {
		return columns;
	}

	public ExportableTable getTable() {
		return table;
	}

	public Map<PhysicalColumn, String> getColumnsOrder(){
		return columnsOrder;
	}

	@Override
	public String getExportIdentifier() {
		return StringHelper.qualify( getTable().getTableName().getText(), "IDX-" + getName().render() );
	}
}
