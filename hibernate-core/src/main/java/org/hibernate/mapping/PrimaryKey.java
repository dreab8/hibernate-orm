/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.Iterator;

import org.hibernate.boot.model.relational.MappedPrimaryKey;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;

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
		setMappedTable( table );
	}

	@Override
	public void addColumn(Column column) {
		final Iterator<Column> columnIterator = getTable().getColumnIterator();
		while ( columnIterator.hasNext() ) {
			final Column next = columnIterator.next();
			if ( next.getCanonicalName().equals( column.getCanonicalName() ) ) {
				next.setNullable( false );
				log.debugf(
						"Forcing column [%s] to be non-null as it is part of the primary key for table [%s]",
						column.getCanonicalName(),
						getTableNameForLogging( column )
				);
			}
		}
		super.addColumn( column );
	}

	protected String getTableNameForLogging(Column column) {
		if ( getTable() != null ) {
			if ( getTable().getNameIdentifier() != null ) {
				return getTable().getNameIdentifier().getCanonicalName();
			}
			else {
				return "<unknown>";
			}
		}
		else if ( column.getValue() != null && column.getValue().getTable() != null ) {
			return column.getValue().getTable().getNameIdentifier().getCanonicalName();
		}
		return "<unknown>";
	}

	public String generatedConstraintNamePrefix() {
		return "PK_";
	}

	public String getExportIdentifier() {
		return StringHelper.qualify( getTable().getName(), "PK-" + getName() );
	}
}
