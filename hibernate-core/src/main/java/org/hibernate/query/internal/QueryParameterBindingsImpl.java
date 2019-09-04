/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.persistence.Parameter;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.QueryException;
import org.hibernate.QueryParameterException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.query.spi.NamedParameterDescriptor;
import org.hibernate.engine.query.spi.OrdinalParameterDescriptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.MathHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.spi.ParameterMetadataImplementor;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.QueryParameterListBinding;
import org.hibernate.type.SerializableType;
import org.hibernate.type.Type;

/**
 * Manages the group of QueryParameterBinding for a particular query.
 *
 * @author Steve Ebersole
 * @author Chris Cranford
 */
@Incubating
public class QueryParameterBindingsImpl implements QueryParameterBindings {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( QueryParameterBindingsImpl.class );

	private final SessionFactoryImplementor sessionFactory;
	private final ParameterMetadataImplementor<QueryParameterImplementor<?>> parameterMetadata;
	private final boolean queryParametersValidationEnabled;

	private final int jdbcStyleOrdinalCountBase;

	private Map<QueryParameter, QueryParameterBinding> parameterBindingMap;

	/**
	 * Constructs a QueryParameterBindings based on the passed information
	 *
	 * @apiNote Calls {@link #from(ParameterMetadataImplementor,SessionFactoryImplementor,boolean)}
	 * using {@link org.hibernate.boot.spi.SessionFactoryOptions#isQueryParametersValidationEnabled}
	 * as `queryParametersValidationEnabled`
	 */
	public static QueryParameterBindingsImpl from(
			ParameterMetadataImplementor parameterMetadata,
			SessionFactoryImplementor sessionFactory) {
		return from(
				parameterMetadata,
				sessionFactory,
				sessionFactory.getSessionFactoryOptions().isQueryParametersValidationEnabled()
		);
	}

	/**
	 * Constructs a QueryParameterBindings based on the passed information
	 */
	public static QueryParameterBindingsImpl from(
			ParameterMetadataImplementor parameterMetadata,
			SessionFactoryImplementor sessionFactory,
			boolean queryParametersValidationEnabled) {
		if ( parameterMetadata == null ) {
			throw new QueryParameterException( "Query parameter metadata cannot be null" );
		}

		return new QueryParameterBindingsImpl(
				sessionFactory,
				parameterMetadata,
				queryParametersValidationEnabled
		);
	}

	private QueryParameterBindingsImpl(
			SessionFactoryImplementor sessionFactory,
			ParameterMetadataImplementor parameterMetadata,
			boolean queryParametersValidationEnabled) {
		this.sessionFactory = sessionFactory;
		this.parameterMetadata = parameterMetadata;
		this.queryParametersValidationEnabled = queryParametersValidationEnabled;

		this.parameterBindingMap = CollectionHelper.concurrentMap( parameterMetadata.getParameterCount() );

		this.jdbcStyleOrdinalCountBase = sessionFactory.getSessionFactoryOptions().jdbcStyleParamsZeroBased() ? 0 : 1;
	}

	@SuppressWarnings({"WeakerAccess", "unchecked"})
	protected QueryParameterBinding makeBinding(QueryParameterImplementor queryParameter) {
		assert ! parameterBindingMap.containsKey( queryParameter );

		if ( ! parameterMetadata.containsReference( queryParameter ) ) {
			throw new IllegalArgumentException(
					"Cannot create binding for parameter reference [" + queryParameter + "] - reference is not a parameter of this query"
			);
		}

		final QueryParameterBinding binding = new QueryParameterBindingImpl( queryParameter, sessionFactory, null, queryParametersValidationEnabled );
		parameterBindingMap.put( queryParameter, binding );

		return binding;
	}

	@Override
	public boolean isBound(QueryParameterImplementor parameter) {
		return getBinding( parameter ).isBound();
	}

	@Override
	public QueryParameterBinding<?> getBinding(QueryParameterImplementor parameter) {
		if ( parameterBindingMap == null ) {
			return null;
		}

		QueryParameterBinding binding = parameterBindingMap.get( parameter );

		if ( binding == null ) {
			if ( ! parameterMetadata.containsReference( parameter ) ) {
				throw new IllegalArgumentException(
						"Could not resolve QueryParameter reference [" + parameter + "] to QueryParameterBinding"
				);
			}

			binding = makeBinding( parameter );
		}

		return binding;
	}

	@Override
	public QueryParameterBinding<?> getBinding(int position) {
		return getBinding( parameterMetadata.getQueryParameter( position ) );
	}

	@Override
	public QueryParameterBinding<?> getBinding(String name) {
		return getBinding( parameterMetadata.getQueryParameter( name ) );
	}

	@Override
	public void validate() {
		parameterMetadata.visitRegistrations(
				queryParameter -> {
					if ( ! parameterBindingMap.containsKey( queryParameter ) ) {
						if ( queryParameter.getName() != null ) {
							throw new QueryException( "Named parameter not bound : " + queryParameter.getName() );
						}
						else {
							throw new QueryException( "Ordinal parameter not bound : " + queryParameter.getPosition() );
						}
					}
				}
		);
	}

	@Override
	public boolean hasAnyMultiValuedBindings() {
		for ( QueryParameterBinding binding : parameterBindingMap.values() ) {
			if ( binding.isMultiValued() ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void visitBindings(BiConsumer action) {
		parameterMetadata.visitRegistrations(
				queryParameterImplementor -> {
					action.accept( queryParameterImplementor, parameterBindingMap.get( queryParameterImplementor ) );
				}
		);
	}
}
