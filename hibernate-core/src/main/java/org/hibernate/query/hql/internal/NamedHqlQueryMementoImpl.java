/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.hql.spi.HqlQueryImplementor;
import org.hibernate.query.hql.spi.NamedHqlQueryMemento;
import org.hibernate.query.spi.AbstractNamedQueryMemento;
import org.hibernate.query.spi.ParameterMemento;
import org.hibernate.query.sqm.internal.QuerySqmImpl;

/**
 * Definition of a named query, defined in the mapping metadata.
 *
 * Additionally, as of JPA 2.1, named query definition can also come
 * from a compiled query.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class NamedHqlQueryMementoImpl extends AbstractNamedQueryMemento implements NamedHqlQueryMemento, Serializable {
	private final String hqlString;
	private final Integer firstResult;
	private final Integer maxResults;

	public NamedHqlQueryMementoImpl(
			String name,
			String hqlString,
			List<ParameterMemento> parameterMementos,
			Integer firstResult,
			Integer maxResults,
			Boolean cacheable,
			String cacheRegion,
			CacheMode cacheMode,
			FlushMode flushMode,
			Boolean readOnly,
			LockOptions lockOptions,
			Integer timeout,
			Integer fetchSize,
			String comment,
			Map<String,Object> hints) {
		super(
				name,
				parameterMementos,
				cacheable,
				cacheRegion,
				cacheMode,
				flushMode,
				readOnly,
				lockOptions,
				timeout,
				fetchSize,
				comment,
				hints
		);
		this.hqlString = hqlString;
		this.firstResult = firstResult;
		this.maxResults = maxResults;
	}

	@Override
	public String getHqlString() {
		return hqlString;
	}

	@Override
	public NamedHqlQueryMemento makeCopy(String name) {
		return new NamedHqlQueryMementoImpl(
				name,
				getParameterMementos(),
				getHqlString(),
				firstResult,
				maxResults,
				getCacheable(),
				getCacheRegion(),
				getCacheMode(),
				getFlushMode(),
				getReadOnly(),
				getLockOptions(),
				getTimeout(),
				getFetchSize(),
				getComment(),
				getHints()
		);
	}

	@Override
	public <T> HqlQueryImplementor<T> toQuery(SharedSessionContractImplementor session, Class<T> resultType) {
		final QuerySqmImpl<T> query = new QuerySqmImpl<>(
				hqlString,
				session.getFactory().getQueryEngine().getSemanticQueryProducer().interpret( hqlString ),
				resultType,
				session
		);

		if ( firstResult != null ) {
			query.setFirstResult( firstResult );
		}
		if ( maxResults != null ) {
			query.setMaxResults( maxResults );
		}

		applyBaseOptions( query, session );

		return query;
	}
}
