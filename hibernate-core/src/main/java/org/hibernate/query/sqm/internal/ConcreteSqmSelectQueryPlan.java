/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.hibernate.ScrollMode;
import org.hibernate.internal.util.collections.streams.StingArrayCollector;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.JpaTupleTransformer;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sqm.consume.internal.SqmConsumeHelper;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.ast.consume.spi.SqlAstSelectToJdbcSelectConverter;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.spi.SqlAstSelectDescriptor;
import org.hibernate.sql.ast.produce.sqm.spi.SqmSelectInterpretation;
import org.hibernate.sql.ast.produce.sqm.spi.SqmSelectToSqlAstConverter;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.internal.RowTransformerJpaTupleImpl;
import org.hibernate.sql.exec.internal.RowTransformerPassThruImpl;
import org.hibernate.sql.exec.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.exec.internal.RowTransformerTupleTransformerAdapter;
import org.hibernate.sql.exec.internal.TupleElementImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.RowTransformer;

/**
 * Standard Hibernate implementation of SelectQueryPlan for SQM-backed
 * {@link org.hibernate.query.Query} implementations, which means
 * HQL/JPQL or {@link javax.persistence.criteria.CriteriaQuery}
 *
 * @author Steve Ebersole
 */
public class ConcreteSqmSelectQueryPlan<R> implements SelectQueryPlan<R> {
	private final SqmSelectStatement sqm;
	private final DomainParameterXref domainParameterXref;
	private final RowTransformer<R> rowTransformer;

	private JdbcSelect jdbcSelect;
	private Map<QueryParameterImplementor<?>, Map<SqmParameter, List<JdbcParameter>>> jdbcParamsXref;

	@SuppressWarnings("WeakerAccess")
	public ConcreteSqmSelectQueryPlan(
			SqmSelectStatement sqm,
			DomainParameterXref domainParameterXref,
			Class<R> resultType,
			QueryOptions queryOptions) {
		this.sqm = sqm;
		this.domainParameterXref = domainParameterXref;

		this.rowTransformer = determineRowTransformer( sqm, resultType, queryOptions );

		// todo (6.0) : we should do as much of the building as we can here
		//  	since this is the thing cached, all the work we do here will
		//  	be cached as well.
		// NOTE : this statement ^^ is not affected by load-query-influencers,
		//		multi-valued parameter expansion, etc - because those all
		//		cause the plan to not be cached.
		// NOTE2 (regarding NOTE) : not sure multi-valued parameter expansion, in
		//		particular, should veto caching of the plan.  The expansion happens
		//		for each execution - see creation of `JdbcParameterBindings` in
		//		`#performList` and `#performScroll`.
	}

	@SuppressWarnings("unchecked")
	private RowTransformer<R> determineRowTransformer(
			SqmSelectStatement sqm,
			Class<R> resultType,
			QueryOptions queryOptions) {
		if ( resultType == null || resultType.isArray() ) {
			if ( queryOptions.getTupleTransformer() != null ) {
				return makeRowTransformerTupleTransformerAdapter( sqm, queryOptions );
			}
			else {
				return RowTransformerPassThruImpl.instance();
			}
		}

		// NOTE : if we get here, a result-type of some kind (other than Object[].class) was specified

		if ( Tuple.class.isAssignableFrom( resultType ) ) {
			// resultType is Tuple..
			if ( queryOptions.getTupleTransformer() == null ) {
				final List<TupleElement<?>> tupleElementList = new ArrayList<>();
				for ( SqmSelection selection : sqm.getQuerySpec().getSelectClause().getSelections() ) {
					tupleElementList.add(
							new TupleElementImpl(
									selection.getSelectableNode().getJavaTypeDescriptor().getJavaType(),
									selection.getAlias()
							)
					);
				}
				return (RowTransformer<R>) new RowTransformerJpaTupleImpl( tupleElementList );
			}

			// there can be a TupleTransformer IF it is a JpaTupleBuilder,
			// otherwise this is considered an error
			if ( queryOptions.getTupleTransformer() instanceof JpaTupleTransformer ) {
				return makeRowTransformerTupleTransformerAdapter( sqm, queryOptions );
			}

			throw new IllegalArgumentException(
					"Illegal combination of Tuple resultType and (non-JpaTupleBuilder) TupleTransformer : " +
							queryOptions.getTupleTransformer()
			);
		}

		// NOTE : if we get here we have a resultType of some kind

		if ( queryOptions.getTupleTransformer() != null ) {
			// aside from checking the type parameters for the given TupleTransformer
			// there is not a decent way to verify that the TupleTransformer returns
			// the same type.  We rely on the API here and assume the best
			return makeRowTransformerTupleTransformerAdapter( sqm, queryOptions );
		}
		else if ( sqm.getQuerySpec().getSelectClause().getSelections().size() > 1 ) {
			throw new IllegalQueryOperationException( "Query defined multiple selections, return cannot be typed (other that Object[] or Tuple)" );
		}
		else {
			return RowTransformerSingularReturnImpl.instance();
		}
	}

	@SuppressWarnings("unchecked")
	private RowTransformer makeRowTransformerTupleTransformerAdapter(
			SqmSelectStatement sqm,
			QueryOptions queryOptions) {
		return new RowTransformerTupleTransformerAdapter<>(
				sqm.getQuerySpec().getSelectClause().getSelections()
						.stream()
						.map( SqmSelection::getAlias )
						.collect( StingArrayCollector.INSTANCE ),
				queryOptions.getTupleTransformer()
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<R> performList(ExecutionContext executionContext) {
		if ( jdbcSelect == null ) {
			final SqmSelectToSqlAstConverter sqmConverter = getSqmSelectToSqlAstConverter( executionContext );
			final SqmSelectInterpretation interpretation = sqmConverter.interpret( sqm );
			jdbcSelect = SqlAstSelectToJdbcSelectConverter.interpret(
					interpretation,
					executionContext.getSession().getSessionFactory()
			);

			this.jdbcParamsXref = SqmConsumeHelper.generateJdbcParamsXref( domainParameterXref, interpretation );
		}

		final JdbcParameterBindings jdbcParameterBindings = QueryHelper.createJdbcParameterBindings(
				executionContext.getDomainParameterBindingContext().getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				executionContext.getSession()
		);

		try {
			// todo (6.0) : make these executors resolvable to allow plugging in custom ones.
			//		Dialect?
			return JdbcSelectExecutorStandardImpl.INSTANCE.list(
					jdbcSelect,
					jdbcParameterBindings,
					executionContext,
					rowTransformer
			);
		}
		finally {
			domainParameterXref.clearExpansions();
		}
	}

	private SqmSelectToSqlAstConverter getSqmSelectToSqlAstConverter(ExecutionContext executionContext) {
		// todo (6.0) : for cases where we have no "load query influencers" we could use a cached SQL AST
		return new SqmSelectToSqlAstConverter(
				executionContext.getQueryOptions(),
				domainParameterXref,
				executionContext.getDomainParameterBindingContext().getQueryParameterBindings(),
				executionContext.getLoadQueryInfluencers(),
				afterLoadAction -> {},
				executionContext.getSession().getFactory()
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public ScrollableResultsImplementor performScroll(ScrollMode scrollMode, ExecutionContext executionContext) {

		final SqmSelectToSqlAstConverter sqmConverter = getSqmSelectToSqlAstConverter( executionContext );

		final SqmSelectInterpretation interpretation = sqmConverter.interpret( sqm );

		final JdbcSelect jdbcSelect = SqlAstSelectToJdbcSelectConverter.interpret(
				interpretation,
				executionContext.getSession().getSessionFactory()
		);

		final Map<QueryParameterImplementor<?>, Map<SqmParameter, List<JdbcParameter>>> jdbcParamsXref =
				SqmConsumeHelper.generateJdbcParamsXref( domainParameterXref, sqmConverter );

		final JdbcParameterBindings jdbcParameterBindings = QueryHelper.createJdbcParameterBindings(
				executionContext.getDomainParameterBindingContext().getQueryParameterBindings(),
				domainParameterXref,
				jdbcParamsXref,
				executionContext.getSession()
		);

		try {
			return JdbcSelectExecutorStandardImpl.INSTANCE.scroll(
					jdbcSelect,
					scrollMode,
					jdbcParameterBindings,
					executionContext,
					rowTransformer
			);
		}
		finally {
			domainParameterXref.clearExpansions();
		}
	}
}
