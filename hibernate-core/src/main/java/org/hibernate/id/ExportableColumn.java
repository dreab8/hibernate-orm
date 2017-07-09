/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.type.BasicType;

/**
 * @author Steve Ebersole
 */
public class ExportableColumn extends Column {

	private BasicType type;

	public ExportableColumn(Database database, Table table, String name, BasicType type) {
		this(
				database,
				table,
				name,
				type,
				database.getDialect().getTypeName( type.sqlTypes( null )[0] )
		);
	}

	public ExportableColumn(
			Database database,
			Table table,
			String name,
			BasicType type,
			String dbTypeDeclaration) {
		super( name );
		setSqlType( dbTypeDeclaration );
		this.type = type;
	}

	@Override
	public int getSqlTypeCode(Mapping mapping) throws MappingException {
		return type.sqlTypes( mapping )[0];
	}
}
