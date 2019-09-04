/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.util.function.Predicate;
import javax.persistence.Parameter;

import org.hibernate.query.ParameterMetadata;

/**
 * @author Steve Ebersole
 */
public interface ParameterMetadataImplementor extends ParameterMetadata {
	@FunctionalInterface
	interface ParameterCollector {
		<P extends QueryParameterImplementor<?>> void collect(P queryParameter);
	}

	void collectAllParameters(ParameterCollector collector);

	boolean hasAnyMatching(Predicate<QueryParameterImplementor<?>> filter);

	@Override
	QueryParameterImplementor<?> getQueryParameter(String name);

	@Override
	QueryParameterImplementor<?> getQueryParameter(int positionLabel);

	@Override
	QueryParameterImplementor<?> resolve(Parameter param);
}
