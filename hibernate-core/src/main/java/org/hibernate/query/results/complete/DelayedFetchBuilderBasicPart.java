/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.complete;

import java.util.function.BiFunction;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.BasicValuedFetchBuilder;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/**
 * @author Christian Beikov
 */
public class DelayedFetchBuilderBasicPart
		implements CompleteFetchBuilder, BasicValuedFetchBuilder, ModelPartReferenceBasic {
	private final NavigablePath navigablePath;
	private final BasicValuedModelPart referencedModelPart;
	private final boolean isEnhancedForLazyLoading;

	public DelayedFetchBuilderBasicPart(
			NavigablePath navigablePath,
			BasicValuedModelPart referencedModelPart,
			boolean isEnhancedForLazyLoading) {
		this.navigablePath = navigablePath;
		this.referencedModelPart = referencedModelPart;
		this.isEnhancedForLazyLoading = isEnhancedForLazyLoading;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public BasicValuedModelPart getReferencedPart() {
		return referencedModelPart;
	}

	@Override
	public BasicFetch<?> buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		return new BasicFetch<>(
				-1,
				parent,
				fetchPath,
				referencedModelPart,
				true,
				null,
				FetchTiming.DELAYED,
				isEnhancedForLazyLoading,
				domainResultCreationState
		);
	}
}
