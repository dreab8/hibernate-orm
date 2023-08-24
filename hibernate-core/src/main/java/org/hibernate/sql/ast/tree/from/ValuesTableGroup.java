/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.insert.Values;

/**
 * A special table group for a VALUES clause.
 *
 * @author Christian Beikov
 */
public class ValuesTableGroup extends AbstractTableGroup {

	private final ValuesTableReference valuesTableReference;

	public ValuesTableGroup(
			NavigablePath navigablePath,
			TableGroupProducer tableGroupProducer,
			List<Values> valuesList,
			String sourceAlias,
			List<String> columnNames,
			boolean canUseInnerJoins,
			SessionFactoryImplementor sessionFactory) {
		super(
				canUseInnerJoins,
				navigablePath,
				tableGroupProducer,
				sourceAlias,
				null,
				sessionFactory
		);
		this.valuesTableReference = new ValuesTableReference( valuesList, sourceAlias, columnNames, sessionFactory );
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean resolve) {
		if ( ( (TableGroupProducer) getModelPart() ).containsTableReference( tableExpression ) ) {
			return getPrimaryTableReference();
		}
		for ( TableGroupJoin tableGroupJoin : getNestedTableGroupJoins() ) {
			if ( resolve || tableGroupJoin.isInitialized() ) {
				final TableReference groupTableReference = tableGroupJoin.getJoinedGroup()
						.getPrimaryTableReference()
						.getTableReference( navigablePath, tableExpression, resolve );
				if ( groupTableReference != null ) {
					return groupTableReference;
				}
			}
		}
		for ( TableGroupJoin tableGroupJoin : getTableGroupJoins() ) {
			if ( resolve || tableGroupJoin.isInitialized() ) {
				final TableReference groupTableReference = tableGroupJoin.getJoinedGroup()
						.getPrimaryTableReference()
						.getTableReference( navigablePath, tableExpression, resolve );
				if ( groupTableReference != null ) {
					return groupTableReference;
				}
			}
		}
		return null;
	}

	@Override
	public ValuesTableReference getPrimaryTableReference() {
		return valuesTableReference;
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return Collections.emptyList();
	}

}
