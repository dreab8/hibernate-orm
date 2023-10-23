/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.sql.ast.tree.cte.CteContainer;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.FromClause;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.SelectStatement;

public abstract class AbstractUpdateOrDeleteStatement extends AbstractMutationStatement {
	private final FromClause fromClause;
	private final Predicate restriction;

	public AbstractUpdateOrDeleteStatement(
			NamedTableReference targetTable,
			FromClause fromClause,
			Predicate restriction) {
		this( null, targetTable, fromClause, restriction, Collections.emptyList() );
	}

	public AbstractUpdateOrDeleteStatement(
			NamedTableReference targetTable,
			FromClause fromClause,
			Predicate restriction,
			List<ColumnReference> returningColumns) {
		this( null, targetTable, fromClause, restriction, returningColumns );
	}

	public AbstractUpdateOrDeleteStatement(
			CteContainer cteContainer,
			NamedTableReference targetTable,
			FromClause fromClause,
			Predicate restriction,
			List<ColumnReference> returningColumns) {
		super( cteContainer, targetTable, returningColumns );
		this.fromClause = fromClause;
		this.restriction = restriction;
	}

	public FromClause getFromClause() {
		return fromClause;
	}

	public Predicate getRestriction() {
		return restriction;
	}

	@Override
	public Set<String> getAffectedTableNames() {
		if ( affectedTableNames == null ) {
			affectedTableNames = new HashSet<>();
			affectedTableNames.addAll( getTargetTable().getAffectedTableNames() );
			for ( TableGroup tableGroup : getFromClause().getRoots() ) {
				tableGroup.applyAffectedTableNames( affectedTableNames::add, this );
			}
			if ( restriction instanceof InSubQueryPredicate ) {
				SelectStatement subQuery = ( (InSubQueryPredicate) restriction ).getSubQuery();
				affectedTableNames.addAll( subQuery.getAffectedTableNames() );
			}
		}
		return affectedTableNames;
	}

}
