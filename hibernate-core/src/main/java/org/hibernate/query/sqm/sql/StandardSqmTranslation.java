/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.AbstractUpdateOrDeleteStatement;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;

/**
 * @author Christian Beikov
 */
public class StandardSqmTranslation<T extends Statement> implements SqmTranslation<T> {

	private final T sqlAst;
	private final Map<SqmParameter<?>, List<List<JdbcParameter>>> jdbcParamMap;
	private final Map<SqmParameter<?>, MappingModelExpressible<?>> parameterMappingModelTypeMap;
	private final SqlExpressionResolver sqlExpressionResolver;
	private final Set<String> affectedTableNames;
	private final FromClauseAccess fromClauseAccess;

	public StandardSqmTranslation(
			T sqlAst,
			Map<SqmParameter<?>, List<List<JdbcParameter>>> jdbcParamMap,
			Map<SqmParameter<?>, MappingModelExpressible<?>> parameterMappingModelTypeMap,
			SqlExpressionResolver sqlExpressionResolver,
			Set<String> affectedTableNames,
			FromClauseAccess fromClauseAccess) {
		this.sqlAst = sqlAst;
		this.jdbcParamMap = jdbcParamMap;
		this.parameterMappingModelTypeMap = parameterMappingModelTypeMap;
		this.sqlExpressionResolver = sqlExpressionResolver;
		if(sqlAst instanceof AbstractUpdateOrDeleteStatement ){
			this.affectedTableNames = ((AbstractUpdateOrDeleteStatement)sqlAst).getAffectedTableNames();
		}
		else {
			this.affectedTableNames = affectedTableNames;
		}
		this.fromClauseAccess = fromClauseAccess;
	}

	@Override
	public T getSqlAst() {
		return sqlAst;
	}

	@Override
	public Map<SqmParameter<?>, List<List<JdbcParameter>>> getJdbcParamsBySqmParam() {
		return jdbcParamMap;
	}

	@Override
	public Map<SqmParameter<?>, MappingModelExpressible<?>> getSqmParameterMappingModelTypeResolutions() {
		return parameterMappingModelTypeMap;
	}

	@Override
	public Set<String> getAffectedTableNames() {
		return affectedTableNames;
	}

	@Override
	public SqlExpressionResolver getSqlExpressionResolver() {
		return sqlExpressionResolver;
	}

	@Override
	public FromClauseAccess getFromClauseAccess() {
		return fromClauseAccess;
	}
}
