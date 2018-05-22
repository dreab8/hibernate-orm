/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.named.internal;

import java.util.List;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.named.spi.AbstractNamedQueryDescriptor;
import org.hibernate.query.named.spi.NamedHqlQueryDescriptor;
import org.hibernate.query.named.spi.ParameterDescriptor;
import org.hibernate.query.spi.HqlQueryImplementor;
import org.hibernate.query.sqm.internal.QuerySqmImpl;

/**
 * @author Steve Ebersole
 */
public class NamedHqlQueryDescriptorImpl extends AbstractNamedQueryDescriptor implements NamedHqlQueryDescriptor {
	private final String hqlString;
	private final Integer firstResult;
	private final Integer maxResults;

	public NamedHqlQueryDescriptorImpl(
			String name,
			List<ParameterDescriptor> parameterDescriptors,
			String hqlString,
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
				parameterDescriptors,
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
	public NamedHqlQueryDescriptor makeCopy(String name) {
		return new NamedHqlQueryDescriptorImpl(
				name,
				getParameterDescriptors(),
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
