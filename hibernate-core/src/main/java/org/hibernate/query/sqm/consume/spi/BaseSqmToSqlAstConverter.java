/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.AssertionFailure;
import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.model.mapping.spi.EmbeddedValuedNavigable;
import org.hibernate.metamodel.model.mapping.EntityTypeDescriptor;
import org.hibernate.metamodel.model.mapping.spi.NavigableContainer;
import org.hibernate.query.BinaryArithmeticOperator;
import org.hibernate.query.UnaryArithmeticOperator;
import org.hibernate.query.internal.QueryHelper;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.expression.SqmNamedParameter;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmPositionalParameter;
import org.hibernate.query.sqm.tree.expression.SqmTuple;
import org.hibernate.query.sqm.tree.expression.SqmUnaryOperation;
import org.hibernate.query.sqm.tree.expression.function.SqmCastTarget;
import org.hibernate.query.sqm.tree.expression.function.SqmDistinct;
import org.hibernate.query.sqm.tree.expression.function.SqmExtractUnit;
import org.hibernate.query.sqm.tree.expression.function.SqmStar;
import org.hibernate.query.sqm.tree.expression.function.SqmTrimSpecification;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmCrossJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.insert.SqmInsertSelectStatement;
import org.hibernate.query.sqm.tree.predicate.SqmLikePredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNegatedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmNullnessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmAndPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmBetweenPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmGroupedPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInListPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmInSubQueryPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmOrPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.internal.SqlAstQuerySpecProcessingStateImpl;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.produce.spi.FromClauseAccess;
import org.hibernate.sql.ast.produce.spi.FromClauseIndex;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.query.sqm.tree.expression.function.SqmFunction;
import org.hibernate.sql.ast.produce.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.produce.spi.SqlAstQuerySpecProcessingState;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.produce.spi.TableGroupJoinProducer;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.ast.produce.sqm.spi.JdbcParameterBySqmParameterAccess;
import org.hibernate.sql.ast.produce.sqm.spi.SqmExpressionInterpretation;
import org.hibernate.sql.ast.produce.sqm.spi.SqmSelectToSqlAstConverter;
import org.hibernate.sql.ast.produce.sqm.spi.SqmToSqlAstConverter;
import org.hibernate.sql.ast.tree.expression.BinaryArithmeticExpression;
import org.hibernate.sql.ast.tree.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.tree.expression.CaseSimpleExpression;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.Distinct;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.ExtractUnit;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.Star;
import org.hibernate.sql.ast.tree.expression.SubQuery;
import org.hibernate.sql.ast.tree.expression.TrimSpecification;
import org.hibernate.sql.ast.tree.expression.UnaryOperation;
import org.hibernate.sql.ast.tree.expression.domain.BasicValuedNavigableReference;
import org.hibernate.sql.ast.tree.expression.domain.EmbeddableValuedNavigableReference;
import org.hibernate.sql.ast.tree.expression.domain.EntityValuedNavigableReference;
import org.hibernate.sql.ast.tree.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.predicate.BetweenPredicate;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.GroupedPredicate;
import org.hibernate.sql.ast.tree.predicate.InListPredicate;
import org.hibernate.sql.ast.tree.predicate.InSubQueryPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.LikePredicate;
import org.hibernate.sql.ast.tree.predicate.NegatedPredicate;
import org.hibernate.sql.ast.tree.predicate.NullnessPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectClause;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.ast.tree.sort.SortSpecification;
import org.hibernate.sql.exec.internal.JdbcParametersImpl;
import org.hibernate.sql.exec.internal.StandardJdbcParameterImpl;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameters;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import org.jboss.logging.Logger;

import static org.hibernate.query.BinaryArithmeticOperator.ADD;
import static org.hibernate.query.BinaryArithmeticOperator.DIVIDE;
import static org.hibernate.query.BinaryArithmeticOperator.MULTIPLY;
import static org.hibernate.query.BinaryArithmeticOperator.QUOT;
import static org.hibernate.query.BinaryArithmeticOperator.SUBTRACT;

/**
 * @author Steve Ebersole
 */
public abstract class BaseSqmToSqlAstConverter
		extends BaseSemanticQueryWalker<Object>
		implements SqmToSqlAstConverter<Object>, JdbcParameterBySqmParameterAccess {

	private static final Logger log = Logger.getLogger( BaseSqmToSqlAstConverter.class );

	protected enum Shallowness {
		NONE,
		CTOR,
		FUNCTION,
		SUBQUERY
	}

	private final SqlAstCreationContext creationContext;
	private final QueryOptions queryOptions;
	private final DomainParameterXref domainParameterXref;
	private final QueryParameterBindings domainParameterBindings;
	private final LoadQueryInfluencers loadQueryInfluencers;
	private final Callback callback;

	private final SqlAliasBaseManager sqlAliasBaseManager = new SqlAliasBaseManager();

	private final FromClauseIndex fromClauseIndex = new FromClauseIndex();

	private final Stack<SqlAstProcessingState> processingStateStack = new StandardStack<>();

	private final Stack<Clause> currentClauseStack = new StandardStack<>();
	private final Stack<SqmSelectToSqlAstConverter.Shallowness> shallownessStack = new StandardStack<>( SqmSelectToSqlAstConverter.Shallowness.NONE );

	public BaseSqmToSqlAstConverter(
			SqlAstCreationContext creationContext,
			QueryOptions queryOptions,
			DomainParameterXref domainParameterXref,
			QueryParameterBindings domainParameterBindings,
			LoadQueryInfluencers loadQueryInfluencers,
			Callback callback) {
		super( creationContext.getServiceRegistry() );
		this.creationContext = creationContext;
		this.queryOptions = queryOptions;
		this.domainParameterXref = domainParameterXref;
		this.domainParameterBindings = domainParameterBindings;
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.callback = callback;
	}

	protected Stack<SqlAstProcessingState> getProcessingStateStack() {
		return processingStateStack;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqlAstCreationState

	@Override
	public SqlAstCreationContext getCreationContext() {
		return creationContext;
	}

	@Override
	public SqlAstProcessingState getCurrentProcessingState() {
		return processingStateStack.getCurrent();
	}

	@Override
	public SqlExpressionResolver getSqlExpressionResolver() {
		return getCurrentProcessingState().getSqlExpressionResolver();
	}

	@Override
	public FromClauseAccess getFromClauseAccess() {
		return fromClauseIndex;
	}

	@Override
	public SqlAliasBaseGenerator getSqlAliasBaseGenerator() {
		return sqlAliasBaseManager;
	}

	@Override
	public LockMode determineLockMode(String identificationVariable) {
		return queryOptions.getLockOptions().getEffectiveLockMode( identificationVariable );
	}

	@Override
	public List<Fetch> visitFetches(FetchParent fetchParent) {
		return Collections.emptyList();
	}

	private QuerySpec currentQuerySpec() {
		return ( (SqlAstQuerySpecProcessingState) processingStateStack.getCurrent() ).getInflightQuerySpec();
	}

	public QueryOptions getQueryOptions() {
		return queryOptions;
	}

	protected FromClauseIndex getFromClauseIndex() {
		return fromClauseIndex;
	}

	protected Stack<Clause> getCurrentClauseStack() {
		return currentClauseStack;
	}

	protected <T> void primeStack(Stack<T> stack, T initialValue) {
		verifyCanBePrimed( stack );
		stack.push( initialValue );
	}

	private static void verifyCanBePrimed(Stack stack) {
		if ( !stack.isEmpty() ) {
			throw new IllegalStateException( "Cannot prime an already populated Stack" );
		}
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Statements

	@Override
	public Object visitUpdateStatement(SqmUpdateStatement statement) {
		throw new AssertionFailure( "UpdateStatement not supported" );
	}

	@Override
	public Object visitDeleteStatement(SqmDeleteStatement statement) {
		throw new AssertionFailure( "DeleteStatement not supported" );
	}

	@Override
	public Object visitInsertSelectStatement(SqmInsertSelectStatement statement) {
		throw new AssertionFailure( "InsertStatement not supported" );
	}

	@Override
	public SelectStatement visitSelectStatement(SqmSelectStatement statement) {
		throw new AssertionFailure( "SelectStatement not supported" );
	}


	@Override
	public QuerySpec visitQuerySpec(SqmQuerySpec sqmQuerySpec) {
		final QuerySpec sqlQuerySpec = new QuerySpec( processingStateStack.isEmpty() );

		processingStateStack.push(
				new SqlAstQuerySpecProcessingStateImpl(
						sqlQuerySpec,
						processingStateStack.getCurrent(),
						this,
						currentClauseStack::getCurrent,
						() -> (expression) -> {},
						() -> sqlQuerySpec.getSelectClause()::addSqlSelection
				)
		);

		try {
			// we want to visit the from-clause first
			visitFromClause( sqmQuerySpec.getFromClause() );

			final SqmSelectClause selectClause = sqmQuerySpec.getSelectClause();
			if ( selectClause != null ) {
				visitSelectClause( selectClause );
			}

			final SqmWhereClause whereClause = sqmQuerySpec.getWhereClause();
			if ( whereClause != null && whereClause.getPredicate() != null ) {
				currentClauseStack.push( Clause.WHERE );
				try {
					sqlQuerySpec.setWhereClauseRestrictions(
							(Predicate) whereClause.getPredicate().accept( this )
					);
				}
				finally {
					currentClauseStack.pop();
				}
			}

			// todo : group-by
			// todo : having

			if ( sqmQuerySpec.getOrderByClause() != null ) {
				currentClauseStack.push( Clause.ORDER );
				try {
					for ( SqmSortSpecification sortSpecification : sqmQuerySpec.getOrderByClause().getSortSpecifications() ) {
						sqlQuerySpec.addSortSpecification( visitSortSpecification( sortSpecification ) );
					}
				}
				finally {
					currentClauseStack.pop();
				}
			}

			sqlQuerySpec.setLimitClauseExpression( visitLimitExpression( sqmQuerySpec.getLimitExpression() ) );
			sqlQuerySpec.setOffsetClauseExpression( visitOffsetExpression( sqmQuerySpec.getOffsetExpression() ) );

			return sqlQuerySpec;
		}
		finally {
			processingStateStack.pop();
		}
	}

	@Override
	public Void visitOrderByClause(SqmOrderByClause orderByClause) {
		super.visitOrderByClause( orderByClause );
		return null;
	}

	@Override
	public SortSpecification visitSortSpecification(SqmSortSpecification sortSpecification) {
		return new SortSpecification(
				toSqlExpression( sortSpecification.getSortExpression().accept( this ) ),
				sortSpecification.getCollation(),
				sortSpecification.getSortOrder()
		);
	}

	@Override
	public Expression visitOffsetExpression(SqmExpression expression) {
		if ( expression == null ) {
			return null;
		}

		currentClauseStack.push( Clause.OFFSET );
		try {
			return (Expression) expression.accept( this );
		}
		finally {
			currentClauseStack.pop();
		}
	}

	@Override
	public Expression visitLimitExpression(SqmExpression expression) {
		if ( expression == null ) {
			return null;
		}

		currentClauseStack.push( Clause.LIMIT );
		try {
			return (Expression) expression.accept( this );
		}
		finally {
			currentClauseStack.pop();
		}
	}


	@Override
	public Void visitFromClause(SqmFromClause sqmFromClause) {
		currentClauseStack.push( Clause.FROM );

		try {
			sqmFromClause.visitRoots(
					sqmRoot -> {
						final NavigableReference rootReference = visitRootPath( sqmRoot );
						assert rootReference instanceof TableGroup;
						currentQuerySpec().getFromClause().addRoot( (TableGroup) rootReference );
					}
			);
		}
		finally {
			currentClauseStack.pop();
		}
		return null;
	}


	@Override
	public NavigableReference visitRootPath(SqmRoot<?> sqmRoot) {
		log.tracef( "Starting resolution of SqmRoot [%s] to TableGroup", sqmRoot );

		if ( fromClauseIndex.isResolved( sqmRoot ) ) {
			final TableGroup resolvedTableGroup = fromClauseIndex.findTableGroup( sqmRoot.getNavigablePath() );
			log.tracef( "SqmRoot [%s] resolved to existing TableGroup [%s]", sqmRoot, resolvedTableGroup );
			return resolvedTableGroup;
		}

		final EntityTypeDescriptor entityDescriptor = sqmRoot.getReferencedPathSource().getEntityDescriptor();
		final TableGroup group = entityDescriptor.createRootTableGroup(
				sqmRoot.getNavigablePath(),
				sqmRoot.getExplicitAlias(),
				JoinType.INNER,
				LockMode.NONE,
				this
		);

		fromClauseIndex.register( sqmRoot, group );

		log.tracef( "Resolved SqmRoot [%s] to new TableGroup [%s]", sqmRoot, group );

		sqmRoot.visitSqmJoins(
				sqmJoin -> {
					final TableGroupJoin tableGroupJoin = (TableGroupJoin) sqmJoin.accept( this );
					if ( tableGroupJoin != null ) {
						group.addTableGroupJoin( tableGroupJoin );
					}
				}
		);

		return group;
	}

	@Override
	public TableGroupJoin visitQualifiedAttributeJoin(SqmAttributeJoin<?, ?> sqmJoin) {
		final TableGroup lhsTableGroup = fromClauseIndex.findTableGroup( sqmJoin.getLhs().getNavigablePath() );

		final NavigableContainer<?> joinedNavigable = sqmJoin.sqmAs( NavigableContainer.class );
		if ( joinedNavigable instanceof EmbeddedValuedNavigable ) {
			// register the LHS TableGroup as the embedded's TableGroup
			fromClauseIndex.registerTableGroup( sqmJoin.getNavigablePath(), lhsTableGroup );

			// we also still want to process its joins, adding them to the LHS TableGroup
			sqmJoin.visitSqmJoins(
					sqmJoinJoin -> {
						final TableGroupJoin tableGroupJoin = (TableGroupJoin) sqmJoinJoin.accept( this );
						if ( tableGroupJoin != null ) {
							lhsTableGroup.addTableGroupJoin( tableGroupJoin );
						}
					}
			);

			return null;
		}

		final TableGroupJoin tableJoinJoin = fromClauseIndex.findTableGroupJoin( sqmJoin.getNavigablePath() );
		if ( tableJoinJoin != null ) {
			return tableJoinJoin;
		}

		final TableGroupJoinProducer joinProducer = joinedNavigable.as( TableGroupJoinProducer.class );

		final TableGroupJoin tableGroupJoin = joinProducer.createTableGroupJoin(
				sqmJoin.getNavigablePath(),
				fromClauseIndex.getTableGroup( sqmJoin.getLhs().getNavigablePath() ),
				sqmJoin.getExplicitAlias(),
				sqmJoin.getSqmJoinType().getCorrespondingSqlJoinType(),
				LockMode.NONE,
				this
		);

		fromClauseIndex.register( sqmJoin, tableGroupJoin );
		lhsTableGroup.addTableGroupJoin( tableGroupJoin );

		// add any additional join restrictions
		if ( sqmJoin.getJoinPredicate() != null ) {
			currentQuerySpec().addRestriction(
					(Predicate) sqmJoin.getJoinPredicate().accept( this )
			);
		}


		return tableGroupJoin;
	}

	@Override
	public TableGroup visitCrossJoin(SqmCrossJoin<?> sqmJoin) {
		final EntityTypeDescriptor entityMetadata = sqmJoin.getReferencedPathSource().getEntityDescriptor();
		final TableGroup group = entityMetadata.createRootTableGroup(
				sqmJoin.getNavigablePath(),
				sqmJoin.getExplicitAlias(),
				JoinType.INNER,
				LockMode.NONE,
				this
		);

		fromClauseIndex.register( sqmJoin, group );

		sqmJoin.visitSqmJoins(
				sqmJoinJoin -> {
					final TableGroupJoin tableGroupJoin = (TableGroupJoin) sqmJoinJoin.accept( this );
					if ( tableGroupJoin != null ) {
						group.addTableGroupJoin( tableGroupJoin );
					}
				}
		);

		return new TableGroupJoin( JoinType.CROSS, group, null ).getJoinedGroup();
	}

	@Override
	public Object visitQualifiedEntityJoin(SqmEntityJoin<?> joinedFromElement) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SelectClause visitSelectClause(SqmSelectClause selectClause) {
		currentClauseStack.push( Clause.SELECT );
		shallownessStack.push( SqmSelectToSqlAstConverter.Shallowness.SUBQUERY );
		try {
			super.visitSelectClause( selectClause );

			currentQuerySpec().getSelectClause().makeDistinct( selectClause.isDistinct() );
			return currentQuerySpec().getSelectClause();
		}
		finally {
			shallownessStack.pop();
			currentClauseStack.pop();
		}
	}

	@Override
	public BasicValuedNavigableReference visitBasicValuedPath(SqmBasicValuedSimplePath path) {
		return new BasicValuedNavigableReference(
				path.getNavigablePath(),
				path.getReferencedPathSource(),
				this
		);
	}

	@Override
	public EmbeddableValuedNavigableReference visitEmbeddableValuedPath(SqmEmbeddedValuedSimplePath path) {
		return new EmbeddableValuedNavigableReference(
				path.getNavigablePath(),
				path.getReferencedPathSource(),
				determineLockMode( path.getExplicitAlias() ),
				this
		);
	}

	@Override
	public Object visitEntityValuedPath(SqmEntityValuedSimplePath path) {
		return new EntityValuedNavigableReference(
				path.getNavigablePath(),
				path.getReferencedPathSource(),
				determineLockMode( path.getExplicitAlias() ),
				this
		);
	}

	@Override
	public Object visitPluralValuedPath(SqmPluralValuedSimplePath path) {
		throw new NotYetImplementedFor6Exception();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Expressions

	@Override
	public Object visitLiteral(SqmLiteral literal) {
		return new QueryLiteral(
				literal.getLiteralValue(),
				literal.getNodeType().getSqlExpressableType( creationContext.getDomainModel().getTypeConfiguration() ),
				getCurrentClauseStack().getCurrent()
		);
	}

	@Override
	public Object visitTuple(SqmTuple sqmTuple) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public GraphImplementor getCurrentResultGraphNode() {
		return null;
	}

	private final Map<SqmParameter,List<JdbcParameter>> jdbcParamsBySqmParam = new IdentityHashMap<>();
	private final JdbcParameters jdbcParameters = new JdbcParametersImpl();

	@Override
	public Map<SqmParameter, List<JdbcParameter>> getJdbcParamsBySqmParam() {
		return jdbcParamsBySqmParam;
	}

	@Override
	public Expression visitNamedParameterExpression(SqmNamedParameter expression) {
		return consumeSqmParameter( expression );
	}

	private Expression consumeSqmParameter(SqmParameter sqmParameter) {
		final List<JdbcParameter> jdbcParametersForSqm = new ArrayList<>();

		if ( jdbcParamsBySqmParam.containsKey( sqmParameter ) ) {
			// this is a "correction" in the case where a Criteria
			assert sqmParameter instanceof SqmCriteriaParameter;
			final SqmParameter copy = sqmParameter.copy();
			domainParameterXref.addCriteriaAdjustment(
					domainParameterXref.getQueryParameter( sqmParameter ),
					(SqmCriteriaParameter) sqmParameter,
					copy
			);

			sqmParameter = copy;
		}

		resolveSqmParameter( sqmParameter, jdbcParametersForSqm::add );

		jdbcParameters.addParameters( jdbcParametersForSqm );
		jdbcParamsBySqmParam.put( sqmParameter, jdbcParametersForSqm );

		if ( jdbcParametersForSqm.size() > 1 ) {
			return new SqlTuple( jdbcParametersForSqm, sqmParameter.getNodeType() );
		}
		else {
			return jdbcParametersForSqm.get( 0 );
		}
	}

	private void resolveSqmParameter(SqmParameter expression, Consumer<JdbcParameter> jdbcParameterConsumer) {
		AllowableParameterType expressableType = expression.getNodeType();

		if ( expressableType == null ) {
			final QueryParameterImplementor<?> queryParameter = domainParameterXref.getQueryParameter( expression );
			final QueryParameterBinding binding = domainParameterBindings.getBinding( queryParameter );
			expressableType = QueryHelper.determineParameterType( binding, queryParameter, creationContext.getDomainModel().getTypeConfiguration() );

			if ( expressableType == null ) {
				log.debugf( "Could not determine ExpressableType for parameter [%s], falling back to Object-handling", expression );
				expressableType = StandardSpiBasicTypes.OBJECT_TYPE;
			}

			expression.applyInferableType( expressableType );
		}

		expressableType.visitJdbcTypes(
				type -> {
					final StandardJdbcParameterImpl jdbcParameter = new StandardJdbcParameterImpl(
							jdbcParameters.getJdbcParameters().size(),
							type,
							currentClauseStack.getCurrent(),
							getCreationContext().getDomainModel().getTypeConfiguration()
					);
					jdbcParameterConsumer.accept( jdbcParameter );
				},
				currentClauseStack.getCurrent(),
				getCreationContext().getDomainModel().getTypeConfiguration()
		);
	}

	@Override
	public Object visitPositionalParameterExpression(SqmPositionalParameter expression) {
		return consumeSqmParameter( expression );
	}

	@Override
	public Object visitCriteriaParameter(SqmCriteriaParameter expression) {
		return consumeSqmParameter( expression );
	}


	@Override
	public Object visitFunction(SqmFunction sqmFunction) {
		shallownessStack.push( Shallowness.FUNCTION );
		try {
			return sqmFunction.convertToSqlAst( this );
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public Object visitTrimSpecification(SqmTrimSpecification specification) {
		shallownessStack.push( Shallowness.FUNCTION );
		try {
			return new TrimSpecification(
					specification.getSpecification()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public Object visitCastTarget(SqmCastTarget target) {
		shallownessStack.push( Shallowness.FUNCTION );
		try {
			return new CastTarget(
					target.getType().getSqlExpressableType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public Object visitExtractUnit(SqmExtractUnit unit) {
		shallownessStack.push( Shallowness.FUNCTION );
		try {
			return new ExtractUnit(
					unit.getUnitName(),
					unit.getType().getSqlExpressableType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	@Override
	public Object visitDistinct(SqmDistinct distinct) {
		return new Distinct( toSqlExpression( distinct.getExpression().accept(this) ) );
	}

	@Override
	public Object visitStar(SqmStar sqmStar) {
		return new Star();
	}

	@Override
	public Object visitUnaryOperationExpression(SqmUnaryOperation expression) {
		shallownessStack.push( Shallowness.NONE );

		try {
			return new UnaryOperation(
					interpret( expression.getOperation() ),
					toSqlExpression( expression.getOperand().accept( this ) ),
					expression.getNodeType().getSqlExpressableType()

			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	private UnaryArithmeticOperator interpret(UnaryArithmeticOperator operator) {
		return operator;
	}

	@Override
	public Expression visitBinaryArithmeticExpression(SqmBinaryArithmetic expression) {
		shallownessStack.push( Shallowness.NONE );

		try {
			return new BinaryArithmeticExpression(
					toSqlExpression( expression.getLeftHandOperand().accept( this ) ),
					interpret( expression.getOperator() ),
					toSqlExpression( expression.getRightHandOperand().accept( this ) ),
					expression.getNodeType().getSqlExpressableType()
			);
		}
		finally {
			shallownessStack.pop();
		}
	}

	private BinaryArithmeticOperator interpret(BinaryArithmeticOperator operator) {
		switch ( operator ) {
			case ADD: {
				return ADD;
			}
			case SUBTRACT: {
				return SUBTRACT;
			}
			case MULTIPLY: {
				return MULTIPLY;
			}
			case DIVIDE: {
				return DIVIDE;
			}
			case QUOT: {
				return QUOT;
			}
		}

		throw new IllegalStateException( "Unexpected BinaryArithmeticOperator : " + operator );
	}

	@Override
	public Object visitSubQueryExpression(SqmSubQuery sqmSubQuery) {
		final QuerySpec subQuerySpec = visitQuerySpec( sqmSubQuery.getQuerySpec() );

		final ExpressableType<?> expressableType = sqmSubQuery.getNodeType();

		return new SubQuery(
				subQuerySpec,
				expressableType instanceof BasicValuedExpressableType<?>
						? ( (BasicValuedExpressableType) expressableType ).getSqlExpressableType( getCreationContext().getDomainModel().getTypeConfiguration() )
						: null,
				expressableType
		);
	}

	@Override
	public CaseSimpleExpression visitSimpleCaseExpression(SqmCaseSimple<?,?> expression) {
		final CaseSimpleExpression result = new CaseSimpleExpression(
				expression.getNodeType().getSqlExpressableType(),
				toSqlExpression( expression.getFixture().accept( this ) )
		);

		for ( SqmCaseSimple.WhenFragment whenFragment : expression.getWhenFragments() ) {
			result.when(
					toSqlExpression( whenFragment.getCheckValue().accept( this ) ),
					toSqlExpression( whenFragment.getResult().accept( this ) )
			);
		}

		result.otherwise( toSqlExpression( expression.getOtherwise().accept( this ) ) );

		return result;
	}

	@Override
	public CaseSearchedExpression visitSearchedCaseExpression(SqmCaseSearched<?> expression) {
		final CaseSearchedExpression result = new CaseSearchedExpression(
				( (BasicValuedExpressableType) expression.getNodeType() ).getSqlExpressableType()
		);

		for ( SqmCaseSearched.WhenFragment whenFragment : expression.getWhenFragments() ) {
			result.when(
					(Predicate) whenFragment.getPredicate().accept( this ),
					toSqlExpression( whenFragment.getResult().accept( this ) )
			);
		}

		result.otherwise( toSqlExpression( expression.getOtherwise().accept( this ) ) );

		return result;
	}

//	@Override
//	public Object visitPluralAttributeElementBinding(PluralAttributeElementBinding binding) {
//		final TableGroup resolvedTableGroup = fromClauseIndex.findResolvedTableGroup( binding.getFromElement() );
//
//		return getCurrentDomainReferenceExpressionBuilder().buildPluralAttributeElementReferenceExpression(
//				binding,
//				resolvedTableGroup,
//				PersisterHelper.convert( binding.getNavigablePath() )
//		);
//	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Predicates


	@Override
	public GroupedPredicate visitGroupedPredicate(SqmGroupedPredicate predicate) {
		return new GroupedPredicate ( (Predicate ) predicate.getSubPredicate().accept( this ) );
	}

	@Override
	public Junction visitAndPredicate(SqmAndPredicate predicate) {
		final Junction conjunction = new Junction( Junction.Nature.CONJUNCTION );
		conjunction.add( (Predicate) predicate.getLeftHandPredicate().accept( this ) );
		conjunction.add( (Predicate) predicate.getRightHandPredicate().accept( this ) );
		return conjunction;
	}

	@Override
	public Junction visitOrPredicate(SqmOrPredicate predicate) {
		final Junction disjunction = new Junction( Junction.Nature.DISJUNCTION );
		disjunction.add( (Predicate) predicate.getLeftHandPredicate().accept( this ) );
		disjunction.add( (Predicate) predicate.getRightHandPredicate().accept( this ) );
		return disjunction;
	}

	@Override
	public NegatedPredicate visitNegatedPredicate(SqmNegatedPredicate predicate) {
		return new NegatedPredicate(
				(Predicate) predicate.getWrappedPredicate().accept( this )
		);
	}

	@Override
	public ComparisonPredicate visitComparisonPredicate(SqmComparisonPredicate predicate) {
		final Expression lhs = toSqlExpression( predicate.getLeftHandExpression().accept( this ) );
		final Expression rhs = toSqlExpression( predicate.getRightHandExpression().accept( this ) );

		return new ComparisonPredicate(
				lhs,
				interpret( predicate.getSqmOperator() ),
				rhs
		);
	}

	private Expression toSqlExpression(Object value) {
		if ( value instanceof SqmExpressionInterpretation ) {
			return ( (SqmExpressionInterpretation) value ).toSqlExpression( this );
		}

		// any other special cases?

		return (Expression) value;
	}

	private ComparisonOperator interpret(ComparisonOperator operator) {
		switch ( operator ) {
			case EQUAL: {
				return ComparisonOperator.EQUAL;
			}
			case NOT_EQUAL: {
				return ComparisonOperator.NOT_EQUAL;
			}
			case GREATER_THAN_OR_EQUAL: {
				return ComparisonOperator.GREATER_THAN_OR_EQUAL;
			}
			case GREATER_THAN: {
				return ComparisonOperator.GREATER_THAN;
			}
			case LESS_THAN_OR_EQUAL: {
				return ComparisonOperator.LESS_THAN_OR_EQUAL;
			}
			case LESS_THAN: {
				return ComparisonOperator.LESS_THAN;
			}
		}

		throw new IllegalStateException( "Unexpected RelationalPredicate Type : " + operator );
	}

	@Override
	public BetweenPredicate visitBetweenPredicate(SqmBetweenPredicate predicate) {
		final Expression expression = toSqlExpression( predicate.getExpression().accept( this ) );
		final Expression lowerBound = toSqlExpression( predicate.getLowerBound().accept( this ) );
		final Expression upperBound = toSqlExpression( predicate.getUpperBound().accept( this ) );

		return new BetweenPredicate(
				expression,
				lowerBound,
				upperBound,
				predicate.isNegated()
		);
	}

	@Override
	public LikePredicate visitLikePredicate(SqmLikePredicate predicate) {
		final Expression escapeExpression = predicate.getEscapeCharacter() == null
				? null
				: toSqlExpression( predicate.getEscapeCharacter().accept( this ) );

		return new LikePredicate(
				toSqlExpression( predicate.getMatchExpression().accept( this ) ),
				toSqlExpression( predicate.getPattern().accept( this ) ),
				escapeExpression,
				predicate.isNegated()
		);
	}

	@Override
	public NullnessPredicate visitIsNullPredicate(SqmNullnessPredicate predicate) {
		return new NullnessPredicate(
				toSqlExpression( predicate.getExpression().accept( this ) ),
				predicate.isNegated()
		);
	}

	@Override
	public InListPredicate visitInListPredicate(SqmInListPredicate<?> predicate) {
		// special case:
		//		if there is just a single element and it is an SqmParameter
		//		and the corresponding QueryParameter binding is multi-valued...
		//		lets expand the SQL AST for each bind value
		if ( predicate.getListExpressions().size() == 1 ) {
			final SqmExpression<?> sqmExpression = predicate.getListExpressions().get( 0 );
			if ( sqmExpression instanceof SqmParameter ) {
				final SqmParameter sqmParameter = (SqmParameter) sqmExpression;
				final QueryParameterImplementor<?> domainParam = domainParameterXref.getQueryParameter( sqmParameter );
				final QueryParameterBinding domainParamBinding = domainParameterBindings.getBinding( domainParam );

				if ( domainParamBinding.isMultiValued() ) {
					final InListPredicate inListPredicate = new InListPredicate(
							toSqlExpression( predicate.getTestExpression().accept( this ) )
					);

					boolean first = true;
					for ( Object bindValue : domainParamBinding.getBindValues() ) {
						final SqmParameter sqmParamToConsume;
						// for each bind value do the following:
						//		1) create a pseudo-SqmParameter (though re-use the original for the first value)
						if ( first ) {
							sqmParamToConsume = sqmParameter;
							first = false;
						}
						else {
							sqmParamToConsume = sqmParameter.copy();
							domainParameterXref.addExpansion( domainParam, sqmParameter, sqmParamToConsume );
						}

						inListPredicate.addExpression( consumeSqmParameter( sqmParamToConsume ) );
					}

					return inListPredicate;
				}
			}
		}


		final InListPredicate inPredicate = new InListPredicate(
				toSqlExpression( predicate.getTestExpression().accept( this ) ),
				predicate.isNegated()
		);

		for ( SqmExpression expression : predicate.getListExpressions() ) {
			inPredicate.addExpression( toSqlExpression( expression.accept( this ) ) );
		}

		return inPredicate;
	}

	@Override
	public InSubQueryPredicate visitInSubQueryPredicate(SqmInSubQueryPredicate predicate) {
		return new InSubQueryPredicate(
				toSqlExpression( predicate.getTestExpression().accept( this ) ),
				(QuerySpec) predicate.getSubQueryExpression().accept( this ),
				predicate.isNegated()
		);
	}
}
