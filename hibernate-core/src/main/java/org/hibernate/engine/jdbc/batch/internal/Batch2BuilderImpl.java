/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.engine.jdbc.batch.internal;

import java.util.function.Supplier;

import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.Batch2;
import org.hibernate.engine.jdbc.batch.spi.Batch2Builder;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.group.PreparedStatementGroup;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;

import static org.hibernate.engine.jdbc.batch.JdbcBatchLogging.BATCH_LOGGER;
import static org.hibernate.engine.jdbc.batch.JdbcBatchLogging.BATCH_TRACE_ENABLED;

/**
 * A builder for {@link Batch} instances.
 *
 * @author Steve Ebersole
 */
public class Batch2BuilderImpl implements Batch2Builder {
	private volatile int globalBatchSize;

	/**
	 * Constructs a BatchBuilderImpl
	 *
	 * @param globalBatchSize The batch size to use.  Can be overridden
	 * on {@link #buildBatch}
	 */
	public Batch2BuilderImpl(int globalBatchSize) {
		if ( BATCH_TRACE_ENABLED ) {
			BATCH_LOGGER.tracef(
					"Using standard Batch2Builder (%s)",
					globalBatchSize
			);
		}

		this.globalBatchSize = globalBatchSize;
	}

	public int getJdbcBatchSize() {
		return globalBatchSize;
	}

	public void setJdbcBatchSize(int jdbcBatchSize) {
		this.globalBatchSize = jdbcBatchSize;
	}

	@Override
	public Batch2 buildBatch(
			BatchKey key,
			Integer batchSizeToUse,
			Supplier<PreparedStatementGroup> statementGroupSupplier,
			JdbcCoordinator jdbcCoordinator) {
		final int batchSize = batchSizeToUse == null
				? globalBatchSize
				: batchSizeToUse;
		return SharedBatchBuildingCode.buildBatch(
				batchSize,
				key,
				statementGroupSupplier.get(),
				jdbcCoordinator
		);
	}
}
