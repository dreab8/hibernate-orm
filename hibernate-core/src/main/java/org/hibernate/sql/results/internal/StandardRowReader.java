/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.query.named.RowReaderMemento;
import org.hibernate.sql.results.LoadingLogger;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.entity.internal.EntityDelayedFetchInitializer;
import org.hibernate.sql.results.graph.entity.internal.EntitySelectFetchInitializer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.sql.results.spi.RowReader;
import org.hibernate.sql.results.spi.RowTransformer;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("rawtypes")
public class StandardRowReader<T> implements RowReader<T> {
	private final List<DomainResultAssembler<?>> resultAssemblers;
	private final List<Initializer> initializers;
	private final RowTransformer<T> rowTransformer;
	private final Class<T> domainResultJavaType;

	private final int assemblerCount;

	public StandardRowReader(
			List<DomainResultAssembler<?>> resultAssemblers,
			List<Initializer> initializers,
			RowTransformer<T> rowTransformer,
			Class<T> domainResultJavaType) {
		this.resultAssemblers = resultAssemblers;
		this.initializers = initializers;
		this.rowTransformer = rowTransformer;

		this.assemblerCount = resultAssemblers.size();
		this.domainResultJavaType = domainResultJavaType;

		logDebugInfo();
	}

	protected void logDebugInfo() {
		// we'd really need some form of description for the assemblers and initializers for this
		// to be useful.
		//
		// todo (6.0) : consider whether this ^^ is worth it

//		if ( ! ResultsLogger.DEBUG_ENABLED ) {
//			return;
//		}

	}

	@Override
	public Class<T> getDomainResultResultJavaType() {
		return domainResultJavaType;
	}

	@Override
	public Class<?> getResultJavaType() {
		if ( resultAssemblers.size() == 1 ) {
			return resultAssemblers.get( 0 ).getAssembledJavaType().getJavaTypeClass();
		}

		return Object[].class;
	}

	@Override
	public List<JavaType<?>> getResultJavaTypes() {
		List<JavaType<?>> javaTypes = new ArrayList<>( resultAssemblers.size() );
		for ( DomainResultAssembler resultAssembler : resultAssemblers ) {
			javaTypes.add( resultAssembler.getAssembledJavaType() );
		}
		return javaTypes;
	}

	@Override
	public List<Initializer> getInitializers() {
		return initializers;
	}

	@Override
	public T readRow(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
		LoadingLogger.LOGGER.trace( "StandardRowReader#readRow" );

		coordinateInitializers( rowProcessingState );

		final Object[] resultRow = new Object[ assemblerCount ];

		for ( int i = 0; i < assemblerCount; i++ ) {
			final DomainResultAssembler assembler = resultAssemblers.get( i );
			LoadingLogger.LOGGER.debugf( "Calling top-level assembler (%s / %s) : %s", i, assemblerCount, assembler );
			resultRow[i] = assembler.assemble( rowProcessingState, options );
		}

		afterRow( rowProcessingState );

		return rowTransformer.transformRow( resultRow );
	}

	private void afterRow(RowProcessingState rowProcessingState) {
		LoadingLogger.LOGGER.trace( "StandardRowReader#afterRow" );

		initializers.forEach( initializer -> initializer.finishUpRow( rowProcessingState ) );
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
	private void coordinateInitializers(RowProcessingState rowProcessingState) {

		final int numberOfInitializers = initializers.size();

		for ( int i = 0; i < numberOfInitializers; i++ ) {
			final Initializer initializer = initializers.get( i );
			if ( ! initializer.isCollectionInitializer() ) {
				initializer.resolveKey( rowProcessingState );
			}
		}

		for ( int i = 0; i < numberOfInitializers; i++ ) {
			final Initializer initializer = initializers.get( i );
			if ( initializer.isCollectionInitializer() ) {
				initializer.resolveKey( rowProcessingState );
			}
		}

		for ( int i = 0; i < numberOfInitializers; i++ ) {
			Initializer initializer = initializers.get( i );
			if ( !( initializer instanceof EntityDelayedFetchInitializer ) && ! (initializer instanceof EntitySelectFetchInitializer ) ) {
				initializer.resolveInstance( rowProcessingState );
			}
		}

		for ( int i = 0; i < numberOfInitializers; i++ ) {
			Initializer initializer = initializers.get( i );
			if ( initializer instanceof EntityDelayedFetchInitializer || initializer instanceof EntitySelectFetchInitializer ) {
				initializer.resolveInstance( rowProcessingState );
			}
		}

		for ( int i = 0; i < numberOfInitializers; i++ ) {
			initializers.get( i ).initializeInstance( rowProcessingState );
		}
	}

	@Override
	@SuppressWarnings("ForLoopReplaceableByForEach")
	public void finishUp(JdbcValuesSourceProcessingState processingState) {
		for ( int i = 0; i < initializers.size(); i++ ) {
			initializers.get( i ).endLoading( processingState.getExecutionContext() );
		}
	}

	@Override
	public RowReaderMemento toMemento(SessionFactoryImplementor factory) {
		return new RowReaderMemento() {
			@Override
			public Class<?>[] getResultClasses() {
				return ArrayHelper.EMPTY_CLASS_ARRAY;
			}

			@Override
			public String[] getResultMappingNames() {
				return ArrayHelper.EMPTY_STRING_ARRAY;
			}
		};
	}
}
