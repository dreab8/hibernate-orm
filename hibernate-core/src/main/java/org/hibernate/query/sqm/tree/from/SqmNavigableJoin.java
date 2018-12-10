/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.LockOptions;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.expression.domain.SqmAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.ConversionException;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.FromClauseIndex;
import org.hibernate.sql.ast.produce.spi.JoinedTableGroupContext;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.produce.spi.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.spi.QuerySpec;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableContainerReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

import org.jboss.logging.Logger;

/**
 * Models a join based on a mapped attribute reference.
 *
 * @author Steve Ebersole
 */
public class SqmNavigableJoin
		extends AbstractSqmJoin
		implements SqmQualifiedJoin {
	private static final Logger log = Logger.getLogger( SqmNavigableJoin.class );

	private final SqmFrom lhs;
	private final SqmNavigableReference navigableReference;
	private final boolean fetched;

	private SqmPredicate onClausePredicate;

	public SqmNavigableJoin(
			SqmFrom lhs,
			SqmNavigableReference navigableReference,
			String uid,
			String alias,
			SqmJoinType joinType,
			boolean fetched) {
		super(
				navigableReference.getSourceReference().getExportedFromElement().getContainingSpace(),
				uid,
				alias,
				joinType
		);
		this.lhs = lhs;

		this.navigableReference = navigableReference;
		this.fetched = fetched;
	}



	public SqmFrom getLhs() {
		return lhs;
	}

	public SqmAttributeReference getAttributeReference() {
		return (SqmAttributeReference) navigableReference;
	}

	@Override
	public SqmNavigableReference getNavigableReference() {
		return getAttributeReference();
	}

	public boolean isFetched() {
		return fetched;
	}

	@Override
	public SqmPredicate getOnClausePredicate() {
		return onClausePredicate;
	}

	public void setOnClausePredicate(SqmPredicate predicate) {
		log.tracef(
				"Setting join predicate [%s] (was [%s])",
				predicate.toString(),
				this.onClausePredicate == null ? "<null>" : this.onClausePredicate.toString()
		);

		this.onClausePredicate = predicate;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitQualifiedAttributeJoinFromElement( this );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return navigableReference.getJavaTypeDescriptor();
	}

	@Override
	public TableGroup locateMapping(FromClauseIndex fromClauseIndex, SqlAstCreationContext creationContext) {
		if ( getNavigableReference().getReferencedNavigable() instanceof EmbeddedValuedNavigable ) {
			return fromClauseIndex.findResolvedTableGroup( getLhs(), creationContext );
		}

		try {
			return fromClauseIndex.resolveTableGroup( getUniqueIdentifier() );
		}
		catch (ConversionException e) {

			final TableGroup lhsTableGroup = fromClauseIndex.findResolvedTableGroup( getLhs(), creationContext );
			final PersistentAttributeDescriptor joinedAttribute = getAttributeReference().getReferencedNavigable();
			if ( joinedAttribute instanceof SingularPersistentAttributeEmbedded ) {
				return lhsTableGroup;
			}

			final TableGroupJoinProducer joinProducer = (TableGroupJoinProducer) joinedAttribute;
			final TableGroupJoin tableGroupJoin = joinProducer.createTableGroupJoin(
					this,
					getJoinType().getCorrespondingSqlJoinType(),
					new JoinedTableGroupContext() {
						@Override
						public NavigableContainerReference getLhs() {
							return (NavigableContainerReference) lhsTableGroup.getNavigableReference();
						}

						@Override
						public ColumnReferenceQualifier getColumnReferenceQualifier() {
							return lhsTableGroup;
						}

						@Override
						public SqlExpressionResolver getSqlExpressionResolver() {
							return creationContext.getSqlSelectionResolver();
						}

						@Override
						public NavigablePath getNavigablePath() {
							return getNavigableReference().getNavigablePath();
						}

						@Override
						public QuerySpec getQuerySpec() {
							return creationContext.getCurrentQuerySpec();
						}

						@Override
						public TableSpace getTableSpace() {
							return creationContext.getTableSpace();
						}

						@Override
						public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
							return creationContext.getSqlAliasBaseGenerator();
						}

						@Override
						public JoinType getTableReferenceJoinType() {
							// TableReferences within the joined TableGroup can be
							// inner-joined (unless they are optional, which is handled
							// inside the producers)
							return JoinType.INNER;
						}

						@Override
						public LockOptions getLockOptions() {
							return creationContext.getLockOptions();
						}
					}
			);

			// todo (6.0) : Can we actually use the on-clause here?
			//
			// Due to the nature of how the joins are being rendered, we aren't able to use the on-clause
			// in the rendered SQL otherwise we hit situations where the a specified join column alias
			// reference causes a column not found problem.
			//
			// We temporarily will recreate the table-group-join and specify the predicate as a part of
			// the current query-spec where-clause instead.
			final TableGroupJoin realJoin = new TableGroupJoin(
					tableGroupJoin.getJoinType(),
					tableGroupJoin.getJoinedGroup(),
					null
			);

			creationContext.getTableSpace().addJoinedTableGroup( realJoin );
			creationContext.getTableGroupStack().push( realJoin.getJoinedGroup() );
			fromClauseIndex.crossReference( this, realJoin.getJoinedGroup() );

			if ( tableGroupJoin.getPredicate() != null ) {
				creationContext.getCurrentQuerySpec().addRestriction( tableGroupJoin.getPredicate() );
			}

			final NavigableReference navigableReference = realJoin.getJoinedGroup().getNavigableReference();
			if ( !creationContext.getNavigableReferenceStack().isEmpty() ) {
				final NavigableReference parent = creationContext.getNavigableReferenceStack().getCurrent();
				( (NavigableContainerReference) parent ).addNavigableReference( navigableReference );
				creationContext.getNavigableReferenceStack().push( navigableReference );
			}

			return realJoin.getJoinedGroup();
//
//
//			final Navigable navigable = getNavigableReference().getReferencedNavigable();
//			if ( navigable instanceof SingularPersistentAttributeEntity ) {
//				final EntityTypeDescriptor entityDescriptor = ( (SingularPersistentAttributeEntity) navigable ).getEntityDescriptor();
//				final TableSpace tableSpace = creationContext.getTableSpace();
//				final QuerySpec querySpec = creationContext.getCurrentQuerySpec();
//
//				final EntityTableGroup entityTableGroup = entityDescriptor.createRootTableGroup(
//						getNavigableReference().getExportedFromElement(),
//						new RootTableGroupContext() {
//							@Override
//							public void addRestriction(Predicate predicate) {
//								getQuerySpec().addRestriction( predicate );
//							}
//
//							@Override
//							public QuerySpec getQuerySpec() {
//								return querySpec;
//							}
//
//							@Override
//							public TableSpace getTableSpace() {
//								return tableSpace;
//							}
//
//							@Override
//							public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
//								return creationContext.getSqlAliasBaseGenerator();
//							}
//
//							@Override
//							public JoinType getTableReferenceJoinType() {
//								return JoinType.INNER;
//							}
//
//							@Override
//							public LockOptions getLockOptions() {
//								return creationContext.getLockOptions();
//							}
//						}
//				);
//
//				creationContext.getTableGroupStack().push( entityTableGroup );
//				fromClauseIndex.crossReference( getNavigableReference().getExportedFromElement(), entityTableGroup );
//
//				final SqmFrom lhs = getLhs();
//				TableGroupJoin tableGroupJoin = ( (SingularPersistentAttributeEntity) navigable ).createTableGroupJoin(
//						getLhs(),
//						JoinType.INNER,
//						new JoinedTableGroupContext() {
//							@Override
//							public NavigableContainerReference getLhs() {
//								Nothrow new NotYetImplementedFor6Exception(  );
//							}
//
//							@Override
//							public ColumnReferenceQualifier getColumnReferenceQualifier() {
//								throw new NotYetImplementedFor6Exception(  );
//							}
//
//							@Override
//							public SqlExpressionResolver getSqlExpressionResolver() {
//								throw new NotYetImplementedFor6Exception(  );
//							}
//
//							@Override
//							public NavigablePath getNavigablePath() {
//								throw new NotYetImplementedFor6Exception(  );
//							}
//
//							@Override
//							public QuerySpec getQuerySpec() {
//								return creationContext.getCurrentQuerySpec();
//							}
//
//							@Override
//							public TableSpace getTableSpace() {
//								return creationContext.getTableSpace();
//							}
//
//							@Override
//							public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
//								return creationContext.getSqlAliasBaseGenerator();
//							}
//
//							@Override
//							public JoinType getTableReferenceJoinType() {
//								return JoinType.INNER;
//							}
//
//							@Override
//							public LockOptions getLockOptions() {
//								return creationContext.getLockOptions();
//							}
//						}
//				);
//
//				//TableGroupJoin tableGroupJoin = new TableGroupJoin( JoinType.INNER, entityTableGroup, null );
//				tableSpace.addJoinedTableGroup( tableGroupJoin );
//
//				creationContext.getNavigableReferenceStack().push( entityTableGroup.getNavigableReference() );
//
//				return entityTableGroup;
//			}

			// our uid is not yet known.. we should create the TableGroup here - at least initiate it
			// return null;
		}
	}
}
