/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.UpdateToJdbcUpdateConverter;
import org.hibernate.sql.ast.tree.spi.UpdateStatement;
import org.hibernate.sql.ast.tree.spi.assign.Assignment;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.LiteralParameter;
import org.hibernate.sql.ast.tree.spi.expression.PositionalParameter;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.BasicExecutionContext;
import org.hibernate.sql.exec.spi.JdbcMutationExecutor;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcUpdate;

import org.jboss.logging.Logger;

/**
 * @author Chris Cranford
 */
public class OneToManyRowsDeletionExecutor implements CollectionRowsDeletionExecutor {
	private static final Logger log = Logger.getLogger( OneToManyRowsDeletionExecutor.class );


	private final PersistentCollectionDescriptor collectionDescriptor;
	private boolean deleteByIndex;
	private Map<Column, JdbcParameter> jdbcParameterMap;
	private final JdbcUpdate removalOperation;

	public OneToManyRowsDeletionExecutor(
			PersistentCollectionDescriptor collectionDescriptor,
			SessionFactoryImplementor sessionFactory,
			Table dmlTargetTable,
			boolean deleteByIndex) {
		this.collectionDescriptor = collectionDescriptor;
		this.deleteByIndex = deleteByIndex;
		final TableReference collectionTableRef = new TableReference(
				dmlTargetTable,
				null,
				false
		);
		final Map<Column, JdbcParameter> jdbcParameterMap = new HashMap<>();

		final List<Assignment> assignments = new ArrayList<>();
		final AtomicInteger parameterCount = new AtomicInteger();


		applyNavigable(
				collectionDescriptor.getCollectionKeyDescriptor(),
				collectionTableRef,
				parameterCount,
				jdbcParameterMap::put,
				assignments,
				sessionFactory
		);

		if ( deleteByIndex ) {
			applyNavigable(
					collectionDescriptor.getIndexDescriptor(),
					collectionTableRef,
					parameterCount,
					jdbcParameterMap::put,
					assignments,
					sessionFactory
			);
		}

		final Predicate predicate = resolvePredicate(
				collectionTableRef,
				parameterCount,
				jdbcParameterMap::put,
				sessionFactory
		);


		final UpdateStatement updateStatement = new UpdateStatement( collectionTableRef, assignments, predicate );

		removalOperation = UpdateToJdbcUpdateConverter.createJdbcUpdate(
				updateStatement,
				sessionFactory
		);

		this.jdbcParameterMap = jdbcParameterMap;

	}

	private Predicate resolvePredicate(
			TableReference dmltableRef,
			AtomicInteger parameterCount,
			BiConsumer<Column, JdbcParameter> columnConsumer,
			SessionFactoryImplementor sessionFactory) {
		Junction junction = new Junction( Junction.Nature.CONJUNCTION );
		collectionDescriptor.getCollectionKeyDescriptor().visitColumns(
				(BiConsumer<SqlExpressableType, Column>) (sqlExpressableType, column) -> {
					final PositionalParameter parameter = new PositionalParameter(
							parameterCount.getAndIncrement(),
							column.getExpressableType(),
							Clause.UPDATE,
							sessionFactory.getTypeConfiguration()
					);

					columnConsumer.accept( column, parameter );

					junction.add(
							new ComparisonPredicate(
									new ColumnReference( column ),
									ComparisonOperator.EQUAL,
									parameter
							)
					);
				},
				Clause.WHERE,
				sessionFactory.getTypeConfiguration()
		);
		collectionDescriptor.getElementDescriptor().visitColumns(
				(BiConsumer<SqlExpressableType, Column>) (sqlExpressableType, column) -> {
					final PositionalParameter parameter = new PositionalParameter(
							parameterCount.getAndIncrement(),
							column.getExpressableType(),
							Clause.UPDATE,
							sessionFactory.getTypeConfiguration()
					);

					columnConsumer.accept( column, parameter );

					junction.add(
							new ComparisonPredicate(
									new ColumnReference( column ),
									ComparisonOperator.EQUAL,
									parameter
							)
					);
				},
				Clause.WHERE,
				sessionFactory.getTypeConfiguration()
		);
		return junction;
	}

	@Override
	public void execute(
			PersistentCollection collection, Object key, SharedSessionContractImplementor session) {
		Iterator deletes = collection.getDeletes( collectionDescriptor, !deleteByIndex );
		if ( log.isDebugEnabled() ) {
			log.debugf(
					"Deleting rows of collection: %s",
					MessageHelper.collectionInfoString( collectionDescriptor, collection, key, session )
			);
		}
		if ( deletes.hasNext() ) {
			int passes = 0;
			final JdbcParameterBindingsImpl jdbcParameterBindings = new JdbcParameterBindingsImpl();
			final BasicExecutionContext executionContext = new BasicExecutionContext( session, jdbcParameterBindings );

			while ( deletes.hasNext() ) {
				Object entry = deletes.next();
				if ( collectionDescriptor.getIdDescriptor() != null ) {
					bindCollectionId( entry, passes, collection, jdbcParameterBindings, session );
				}
				else {
					bindCollectionKey( key, jdbcParameterBindings, session );
					if ( deleteByIndex ) {
						bindCollectionIndex( entry, jdbcParameterBindings, session );
					}
					else {
						bindCollectionElement( entry, collection, jdbcParameterBindings, session );
					}
				}
				JdbcMutationExecutor.WITH_AFTER_STATEMENT_CALL.execute( removalOperation, executionContext );

				passes++;
				jdbcParameterBindings.clear();
				log.debugf( "Done deleting collection rows: %s deleted", passes );
			}
		}
		else {
			log.debug( "No rows to delete" );
		}
	}

	@SuppressWarnings("WeakerAccess")
	protected void applyNavigable(
			Navigable navigable,
			TableReference dmlTableRef,
			AtomicInteger parameterCount,
			BiConsumer<Column, JdbcParameter> columnConsumer,
			List<Assignment> assignments,
			SessionFactoryImplementor sessionFactory) {
		//noinspection RedundantCast
		navigable.visitColumns(
				(BiConsumer<SqlExpressableType, Column>) (sqlExpressableType, column) -> {

					final ColumnReference columnReference = dmlTableRef.resolveColumnReference( column );

					final PositionalParameter parameter = new PositionalParameter(
							parameterCount.getAndIncrement(),
							column.getExpressableType(),
							Clause.INSERT,
							sessionFactory.getTypeConfiguration()
					);

					final Assignment assignment = new Assignment( columnReference, parameter );
					assignments.add( assignment );

					columnConsumer.accept( column, parameter );
				},
				Clause.UPDATE,
				sessionFactory.getTypeConfiguration()
		);
	}

	protected void bindCollectionKey(
			Object key,
			JdbcParameterBindingsImpl jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		collectionDescriptor.getCollectionKeyDescriptor().dehydrate(
				collectionDescriptor.getCollectionKeyDescriptor().unresolve( key, session ),
				(jdbcValue, type, boundColumn) -> createBinding(
						jdbcValue,
						boundColumn,
						type,
						jdbcParameterBindings,
						session
				),
				Clause.DELETE,
				session
		);
	}

	protected void bindCollectionElement(
			Object entry,
			PersistentCollection collection,
			JdbcParameterBindingsImpl jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		collectionDescriptor.getElementDescriptor().dehydrate(
				collectionDescriptor.getElementDescriptor().unresolve(
						collection.getElement( entry, collectionDescriptor ),
						session
				),
				(jdbcValue, type, boundColumn) -> createBinding(
						jdbcValue,
						boundColumn,
						type,
						jdbcParameterBindings,
						session
				),
				Clause.DELETE,
				session
		);
	}

	protected void bindCollectionIndex(
			Object index,
			JdbcParameterBindingsImpl jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		// todo (6.0) : probably not the correct `assumedIndex`
		if ( collectionDescriptor.getIndexDescriptor() != null ) {
			if ( collectionDescriptor.getIndexDescriptor().getBaseIndex() != 0 ) {
				index = (Integer) index + collectionDescriptor.getIndexDescriptor().getBaseIndex();
			}
			collectionDescriptor.getIndexDescriptor().dehydrate(
					collectionDescriptor.getIndexDescriptor().unresolve( index, session ),
					(jdbcValue, type, boundColumn) -> createBinding(
							jdbcValue,
							boundColumn,
							type,
							jdbcParameterBindings,
							session
					),
					Clause.DELETE,
					session
			);
		}
	}

	protected void bindCollectionId(
			Object entry,
			int assumedIdentifier,
			PersistentCollection collection,
			JdbcParameterBindingsImpl jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		// todo (6.0) : probably not the correct `assumedIdentifier`
		final Object identifier = collection.getIdentifier( entry, assumedIdentifier, collectionDescriptor );

		collectionDescriptor.getIdDescriptor().dehydrate(
				collectionDescriptor.getIdDescriptor().unresolve( identifier, session ),
				(jdbcValue, type, boundColumn) -> createBinding(
						jdbcValue,
						boundColumn,
						type,
						jdbcParameterBindings,
						session
				),
				Clause.DELETE,
				session
		);
	}

	protected void createBinding(
			Object jdbcValue,
			Column boundColumn,
			SqlExpressableType type,
			JdbcParameterBindingsImpl jdbcParameterBindings,
			SharedSessionContractImplementor session) {
		final JdbcParameter jdbcParameter = resolveJdbcParameter( boundColumn );

		jdbcParameterBindings.addBinding(
				jdbcParameter,
				new LiteralParameter(
						jdbcValue,
						type,
						Clause.DELETE,
						session.getFactory().getTypeConfiguration()
				)
		);
	}

	private JdbcParameter resolveJdbcParameter(Column boundColumn) {
		final JdbcParameter jdbcParameter = jdbcParameterMap.get( boundColumn );
		if ( jdbcParameter == null ) {
			throw new IllegalStateException( "JdbcParameter not found for Column [" + boundColumn + "]" );
		}
		return jdbcParameter;
	}
}
