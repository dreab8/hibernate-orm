/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.Collection;

import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedPrimaryKey;
import org.hibernate.boot.model.relational.MappedTable;

import org.jboss.logging.Logger;

/**
 * A primary key constraint
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class PrimaryKey extends Constraint implements MappedPrimaryKey {
	private static final Logger log = Logger.getLogger( PrimaryKey.class );

	public PrimaryKey(MappedTable table){
		setTable( table );
	}

	@Override
	public void addColumn(Column column) {
		addColumn( (MappedColumn) column );
	}

	@Override
	public void addColumn(MappedColumn column) {
		final Collection<MappedColumn> columns = getMappedTable().getMappedColumns();
		columns.stream().filter( c -> c.getText().equals( column.getText() ) )
				.forEach( c -> {
					( (Column) c ).setNullable( false );
					log.debugf(
							"Forcing column [%s] to be non-null as it is part of the primary key for table [%s]",
							column.getText(),
							getTableNameForLogging( column )
					);
				} );
		super.addColumn( column );
	}

	protected String getTableNameForLogging(MappedColumn mappedColumn) {
		if ( getMappedTable() != null ) {
			if ( getMappedTable().getNameIdentifier() != null ) {
				return getMappedTable().getNameIdentifier().getCanonicalName();
			}
			else {
				return "<unknown>";
			}
		}
		else if ( mappedColumn instanceof Column ) {
			Column column = (Column) mappedColumn;
			if ( column.getTableName() != null ) {
				return column.getTableName().getCanonicalName();
			}
		}
		return "<unknown>";
	}

	@Override
	public String generatedConstraintNamePrefix() {
		return "PK_";
	}

	// todo (6.0) - Should this class implement Exportable like in 5.x?
//	@Override
//	public String getExportIdentifier() {
//		return StringHelper.qualify( getTable().getExportIdentifier(), "PK-" + getName() );
//	}
}
