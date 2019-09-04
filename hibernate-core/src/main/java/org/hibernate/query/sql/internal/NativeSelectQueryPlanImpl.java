/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.internal;

import java.util.List;
import java.util.Set;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.ScrollMode;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.sql.spi.NativeSelectQueryPlan;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;
import org.hibernate.sql.results.spi.JdbcValuesMappingDescriptor;
import org.hibernate.sql.results.spi.RowTransformer;

/**
 * @author Steve Ebersole
 */
public class NativeSelectQueryPlanImpl<R> implements NativeSelectQueryPlan<R> {
	private final String sql;
	private final Set<String> affectedTableNames;

	private final List<QueryParameterImplementor<?>> parameterList;

	private final JdbcValuesMappingDescriptor resultSetMapping;
	private final RowTransformer<R> rowTransformer;

	public NativeSelectQueryPlanImpl(
			String sql,
			Set<String> affectedTableNames,
			List<QueryParameterImplementor<?>> parameterList,
			JdbcValuesMappingDescriptor resultSetMapping,
			RowTransformer<R> rowTransformer) {
		this.sql = sql;
		this.affectedTableNames = affectedTableNames;
		this.parameterList = parameterList;
		this.resultSetMapping = resultSetMapping;
		this.rowTransformer = rowTransformer;
	}

	@Override
	public List<R> performList(ExecutionContext executionContext) {
		throw new NotYetImplementedFor6Exception( getClass() );

//		final List<JdbcParameterBinder> jdbcParameterBinders = resolveJdbcParameterBinders( executionContext );
//
//		final JdbcSelect jdbcSelect = new JdbcSelectImpl(
//				sql,
//				jdbcParameterBinders,
//				resultSetMapping,
//				affectedTableNames
//		);
//
//		// todo (6.0) : need to make this swappable (see note in executor class)
//		final JdbcSelectExecutor executor = JdbcSelectExecutorStandardImpl.INSTANCE;
//
//		return executor.list( jdbcSelect, JdbcParameterBindings.NO_BINDINGS, executionContext, rowTransformer );
	}
//
//	private List<JdbcParameterBinder> resolveJdbcParameterBinders(ExecutionContext executionContext) {
//		final List<JdbcParameterBinder> jdbcParameterBinders = CollectionHelper.arrayList( parameterList.size() );
//
//		for ( QueryParameterImplementor parameter : parameterList ) {
//			final QueryParameterBinding parameterBinding = executionContext.getDomainParameterBindingContext()
//					.getQueryParameterBindings()
//					.getBinding( parameter );
//			AllowableParameterType type = parameterBinding.getBindType();
//			if ( type == null ) {
//				type = parameter.getHibernateType();
//			}
//
//			type.dehydrate(
//					type.unresolve( parameterBinding.getBindValue(), executionContext.getSession() ),
//					(jdbcValue, sqlExpressableType, boundColumn) -> jdbcParameterBinders.add(
//							(statement, startPosition, jdbcParameterBindings, executionContext1) -> {
//								//noinspection unchecked
//								sqlExpressableType.getJdbcValueBinder().bind(
//										statement,
//										startPosition,
//										jdbcValue,
//										executionContext1
//								);
//								return 1;
//							}
//					),
//					Clause.IRRELEVANT,
//					executionContext.getSession()
//			);
//		}
//
//		return jdbcParameterBinders;
//	}

	@Override
	public ScrollableResultsImplementor<R> performScroll(ScrollMode scrollMode, ExecutionContext executionContext) {
		throw new NotYetImplementedFor6Exception( getClass() );
//
//		// todo (6.0) : see notes above in `#performList`
//
//		final List<JdbcParameterBinder> jdbcParameterBinders = resolveJdbcParameterBinders( executionContext );
//
//		final JdbcSelect jdbcSelect = new JdbcSelectImpl(
//				sql,
//				jdbcParameterBinders,
//				resultSetMapping,
//				affectedTableNames
//		);
//		final JdbcSelectExecutor executor = JdbcSelectExecutorStandardImpl.INSTANCE;
//
//		return executor.scroll(
//				jdbcSelect,
//				scrollMode,
//				// the binders created here encapsulate their bind value
//				JdbcParameterBindings.NO_BINDINGS,
//				executionContext,
//				rowTransformer
//		);
	}
}
