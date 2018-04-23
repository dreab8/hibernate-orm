/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.DenormalizedMappedTable;
import org.hibernate.boot.model.relational.MappedForeignKey;
import org.hibernate.boot.model.relational.MappedIndex;
import org.hibernate.boot.model.relational.MappedNamespace;
import org.hibernate.boot.model.relational.MappedPrimaryKey;
import org.hibernate.boot.model.relational.MappedUniqueKey;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.internal.util.collections.JoinedIterator;

/**
 * @author Gavin King
 */
@SuppressWarnings("unchecked")
public class DenormalizedTable extends Table implements DenormalizedMappedTable<Column> {

	private final MappedTable<Column> includedTable;

	public DenormalizedTable(Namespace namespace, Identifier physicalTableName, boolean isAbstract, Table includedTable) {
		super( namespace, physicalTableName, isAbstract );
		this.includedTable = includedTable;
		includedTable.setHasDenormalizedTables();
	}

	public DenormalizedTable(
			Namespace namespace,
			Identifier physicalTableName,
			String subselectFragment,
			boolean isAbstract,
			Table includedTable) {
		super( namespace, physicalTableName, subselectFragment, isAbstract );
		this.includedTable = includedTable;
		includedTable.setHasDenormalizedTables();
	}

	public DenormalizedTable(Namespace namespace, String subselect, boolean isAbstract, Table includedTable) {
		super( namespace, subselect, isAbstract );
		this.includedTable = includedTable;
		includedTable.setHasDenormalizedTables();
	}

	public DenormalizedTable(
			MappedNamespace namespace,
			org.hibernate.naming.Identifier tableName,
			boolean isAbstract,
			MappedTable includedTable) {
		super( namespace, tableName, isAbstract );
		this.includedTable = includedTable;
		includedTable.setHasDenormalizedTables();
	}

	public DenormalizedTable(
			MappedNamespace namespace,
			org.hibernate.naming.Identifier tableName,
			String subselectFragment,
			boolean isAbstract,
			MappedTable includedTable) {
		super( namespace, tableName, subselectFragment, isAbstract );
		this.includedTable = includedTable;
		includedTable.setHasDenormalizedTables();
	}

	public DenormalizedTable(
			MappedNamespace namespace,
			String subselect,
			boolean isAbstract,
			MappedTable includedTable) {
		super( namespace, subselect, isAbstract );
		this.includedTable = includedTable;
		includedTable.setHasDenormalizedTables();
	}

	@Override
	public void createForeignKeys() {
		includedTable.createForeignKeys();
		for ( MappedForeignKey fk : includedTable.getMappedForeignKeys() ) {
			createForeignKey(
					Constraint.generateName(
							fk.generatedConstraintNamePrefix(),
							this,
							fk.getColumns()
					),
					fk.getColumns(),
					fk.getReferencedEntityName(),
					fk.getKeyDefinition(),
					fk.getReferencedColumns()
			);
		}
	}

	@Override
	public Column getColumn(Column column) {
		Column superColumn = super.getColumn( column );
		if ( superColumn != null ) {
			return superColumn;
		}
		else {
			return includedTable.getColumn( column );
		}
	}

	@Override
	public Column getColumn(org.hibernate.naming.Identifier name) {
		getColumn( name );
		Column superColumn = super.getColumn( name );
		if ( superColumn != null ) {
			return superColumn;
		}
		else {
			return includedTable.getColumn( name );
		}
	}

	@Override
	public Iterator getColumnIterator() {
		return getMappedColumns().iterator();
	}

	@Override
	public java.util.Set<Column> getMappedColumns() {
		Set<Column> mappedColumns = new HashSet<>();
		mappedColumns.addAll( includedTable.getMappedColumns() );
		mappedColumns.addAll( super.getMappedColumns() );
		return Collections.unmodifiableSet( mappedColumns );
	}

	@Override
	public boolean containsColumn(Column column) {
		return super.containsColumn( column ) || includedTable.containsColumn( column );
	}

	@Override
	public PrimaryKey getPrimaryKey() {
		return (PrimaryKey) includedTable.getMappedPrimaryKey();
	}

	@Override
	public MappedPrimaryKey getMappedPrimaryKey() {
		return includedTable.getMappedPrimaryKey();
	}

	@Override
	public Iterator getUniqueKeyIterator() {
		return getMappedUniqueKeys().iterator();
	}

	@Override
	public Collection<MappedUniqueKey> getMappedUniqueKeys() {
		includedTable.getMappedUniqueKeys().forEach( uniqueKey -> createMappedUniqueKey( uniqueKey.getColumns() ) );
		return super.getMappedUniqueKeys();
	}

	@Override
	public Iterator getIndexIterator() {
		return getMappedIndexes().iterator();
	}

	@Override
	public Collection<MappedIndex> getMappedIndexes() {
		final List<MappedIndex> indexes = new ArrayList<>(  );
		indexes.addAll(includedTable.getMappedIndexes()  );
		indexes.addAll( super.getMappedIndexes() );
		return indexes;
	}

	/**
	 * @deprecated since 6.0, use {{@link #getIncludedMappedTable()}} instead.
	 */
	@Deprecated
	public Table getIncludedTable() {
		return (Table) includedTable;
	}

	@Override
	public MappedTable getIncludedMappedTable() {
		return includedTable;
	}
}
