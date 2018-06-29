/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.persistence.EntityNotFoundException;
import javax.persistence.TemporalType;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.internal.NonNullableTransientDependencies;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.mapping.ToOne;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.AbstractNonIdSingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.JoinablePersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.TableReferenceJoinCollector;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.ForeignKey.ColumnMappings;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceEntity;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.metamodel.spi.TableGroupInfo;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.JoinedTableGroupContext;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.produce.spi.TableGroupContext;
import org.hibernate.sql.ast.produce.spi.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.from.EntityTableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;
import org.hibernate.sql.ast.tree.spi.predicate.Junction;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.ast.tree.spi.predicate.RelationalPredicate;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.internal.AggregateSqlSelectionGroupNode;
import org.hibernate.sql.results.internal.EntityFetchImpl;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.SqlSelectionGroupNode;
import org.hibernate.sql.results.spi.SqlSelectionResolutionContext;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.spi.ValueBinder;
import org.hibernate.type.descriptor.spi.ValueExtractor;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.loader.spi.SingleIdEntityLoader.NO_LOAD_OPTIONS;


/**
 * @author Steve Ebersole
 */
public class SingularPersistentAttributeEntity<O,J>
		extends AbstractNonIdSingularPersistentAttribute<O,J>
		implements JoinablePersistentAttribute<O,J>, EntityValuedNavigable<J>, Fetchable<J>, AllowableParameterType<J>, TableGroupJoinProducer {

	private final SingularAttributeClassification classification;
	private final PersistentAttributeType persistentAttributeType;
	private final NavigableRole navigableRole;
	private final String sqlAliasStem;
	private final EntityDescriptor<J> entityDescriptor;
	private final String referencedAttributeName;
	private final ForeignKey foreignKey;

	public SingularPersistentAttributeEntity(
			ManagedTypeDescriptor<O> runtimeModelContainer,
			PersistentAttributeMapping bootModelAttribute,
			PropertyAccess propertyAccess,
			Disposition disposition,
			SingularAttributeClassification classification,
			RuntimeModelCreationContext context) {
		super( runtimeModelContainer, bootModelAttribute, propertyAccess, disposition );
		this.classification = classification;
		this.navigableRole = runtimeModelContainer.getNavigableRole().append( bootModelAttribute.getName() );

		final ToOne valueMapping = (ToOne) bootModelAttribute.getValueMapping();


		if ( valueMapping.getReferencedEntityName() == null ) {
			throw new MappingException(
					"Name of target entity of a to-one association not known : " + navigableRole.getFullPath()
			);
		}

		this.entityDescriptor = context.getInFlightRuntimeModel().findEntityDescriptor( valueMapping.getReferencedEntityName() );
		if ( entityDescriptor == null ) {
			throw new MappingException(
					String.format(
							Locale.ROOT,
							"Cannot create SingularPersistentAttributeEntity [%s] : could not locate target entity descriptor [%s]",
							navigableRole.getFullPath(),
							valueMapping.getReferencedEntityName()
					)
			);
		}

		// todo (6.0) : we need to delay resolving this.
		//		this is essentially a "second pass".  for now we assume it
		// 		points to the target entity's PK
//		assert valueMapping.isReferenceToPrimaryKey();

		// taken from ToOneFkSecondPass
		if ( valueMapping.getForeignKey() != null ) {
			this.foreignKey = context.getDatabaseObjectResolver().resolveForeignKey( valueMapping.getForeignKey() );
			this.referencedAttributeName = null;
		}
		else {
			if ( valueMapping.getReferencedPropertyName() != null ) {
				this.referencedAttributeName = valueMapping.getReferencedPropertyName();
			}
			else if ( valueMapping.isReferenceToPrimaryKey() ) {
				// do nothing
				this.referencedAttributeName = null;
			}
			else {
				this.referencedAttributeName = null;
				throw new NotYetImplementedFor6Exception(  );
			}
			this.foreignKey = null;
			//			/*
//			ManyToOne manyToOne = (ManyToOne) value;
//			PersistentClass ref = (PersistentClass) persistentClasses.get( manyToOne.getReferencedEntityName() );
//			if ( ref == null ) {
//				throw new AnnotationException(
//						"@OneToOne or @ManyToOne on "
//								+ StringHelper.qualify( entityClassName, path )
//								+ " references an unknown entity: "
//								+ manyToOne.getReferencedEntityName()
//				);
//			}
//			manyToOne.setPropertyName( path );
//			BinderHelper.createSyntheticPropertyReference( columns, ref, null, manyToOne, false, buildingContext );
//			TableBinder.bindFk( ref, null, columns, manyToOne, unique, buildingContext );
//			if ( !manyToOne.isIgnoreNotFound() ) manyToOne.createPropertyRefConstraints( persistentClasses );
//			 */
//			//throw new NotYetImplementedFor6Exception(  );
		}

		if ( SingularAttributeClassification.MANY_TO_ONE.equals( classification ) ) {
			persistentAttributeType = PersistentAttributeType.MANY_TO_ONE;
		}
		else {
			persistentAttributeType = PersistentAttributeType.ONE_TO_ONE;
		}

		this.sqlAliasStem =  SqlAliasStemHelper.INSTANCE.generateStemFromAttributeName( bootModelAttribute.getName() );

		instantiationComplete( bootModelAttribute, context );
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return persistentAttributeType;
	}

	@Override
	public EntityDescriptor<J> getEntityDescriptor() {
		return entityDescriptor;
	}

	@Override
	public EntityValuedExpressableType<J> getType() {
		return (EntityValuedExpressableType<J>) super.getType();
	}

	@Override
	public String getJpaEntityName() {
		return entityDescriptor.getJpaEntityName();
	}

	@Override
	public EntityJavaDescriptor<J> getJavaTypeDescriptor() {
		return entityDescriptor.getJavaTypeDescriptor();
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return entityDescriptor.findNavigable( navigableName );
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		entityDescriptor.visitNavigables( visitor );
	}

	@Override
	public boolean isAssociation() {
		return true;
	}

	public EntityDescriptor getAssociatedEntityDescriptor() {
		return entityDescriptor;
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return classification;
	}

	@Override
	public String asLoggableText() {
		return "SingularAttributeEntity([" + getAttributeTypeClassification().name() + "] " +
				getContainer().asLoggableText() + '.' + getAttributeName() +
				")";
	}

	@Override
	public String toString() {
		return asLoggableText();
	}

	public String getEntityName() {
		return entityDescriptor.getEntityName();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitSingularAttributeEntity( this );
	}

	@Override
	public SqmNavigableReference createSqmExpression(
			SqmFrom sourceSqmFrom,
			SqmNavigableContainerReference containerReference,
			SqmCreationContext creationContext) {
		return new SqmSingularAttributeReferenceEntity(
				containerReference,
				this,
				creationContext
		);
	}

	@Override
	public QueryResult createQueryResult(
			NavigableReference navigableReference,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		return entityDescriptor.createQueryResult(
				navigableReference,
				resultVariable,
				creationContext
		);
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public ManagedTypeDescriptor<J> getFetchedManagedType() {
		return entityDescriptor;
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			ColumnReferenceQualifier qualifier,
			FetchStrategy fetchStrategy,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		return new EntityFetchImpl(
				fetchParent,
				qualifier,
				this,
				creationContext.getLockOptions().getEffectiveLockMode( resultVariable ),
				fetchParent.getNavigablePath().append( getNavigableName() ),
				fetchStrategy,
				creationContext
		);
	}

	@Override
	public String getSqlAliasStem() {
		return sqlAliasStem;
	}

	@Override
	public void applyTableReferenceJoins(
			ColumnReferenceQualifier lhs,
			JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector joinCollector,
			TableGroupContext tableGroupContext) {
		getEntityDescriptor().applyTableReferenceJoins( lhs, joinType, sqlAliasBase, joinCollector, tableGroupContext );
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			TableGroupInfo tableGroupInfoSource,
			JoinType joinType,
			JoinedTableGroupContext tableGroupJoinContext) {
		// todo (6.0) : something like EntityDescriptor


		final SqlAliasBase sqlAliasBase = tableGroupJoinContext.getSqlAliasBaseGenerator().createSqlAliasBase( getSqlAliasStem() );

		final TableReferenceJoinCollectorImpl joinCollector = new TableReferenceJoinCollectorImpl( tableGroupJoinContext );

		getEntityDescriptor().applyTableReferenceJoins(
				tableGroupJoinContext.getColumnReferenceQualifier(),
				tableGroupJoinContext.getTableReferenceJoinType(),
				sqlAliasBase,
				joinCollector,
				tableGroupJoinContext
		);

		// handle optional entity references to be outer joins.
		if ( isNullable() && JoinType.INNER.equals( joinType ) ) {
			joinType = JoinType.LEFT;
		}

		return joinCollector.generateTableGroup( joinType, tableGroupInfoSource, tableGroupJoinContext );
	}

	@Override
	public Object unresolve(Object value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}

		if ( ! getEntityDescriptor().isInstance( value ) ) {
			throw new HibernateException( "Unexpected value for unresolve [" + value + "], expecting entity instance" );
		}

		// todo (6.0) : assumes FK refers to owner's PK which is not always true
		return getEntityDescriptor().getIdentifier( value, session );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void dehydrate(
			Object value,
			JdbcValueCollector jdbcValueCollector,
			SharedSessionContractImplementor session) {
		// todo (6.0) (fk-to-id) : another place where we assume association fks point to the pks
		getEntityDescriptor().getHierarchy().getIdentifierDescriptor().dehydrate(
				value,
				(jdbcValue, type, targetColumn) -> jdbcValueCollector.collect(
						jdbcValue,
						getEntityDescriptor().getHierarchy().getIdentifierDescriptor(),
						foreignKey.getColumnMappings().findReferringColumn( targetColumn )
				),
				session
		);
	}

	@Override
	public List<Column> getColumns() {
		// if there is no foreign key stored for this attribute, no columns exist for this side
		if ( foreignKey != null ) {
			return foreignKey.getColumnMappings().getReferringColumns();
		}
		return new ArrayList<>();
	}

	private class TableReferenceJoinCollectorImpl implements TableReferenceJoinCollector {
		private final JoinedTableGroupContext tableGroupJoinContext;

		private TableReference rootTableReference;
		private List<TableReferenceJoin> tableReferenceJoins;
		private Predicate predicate;

		@SuppressWarnings("WeakerAccess")
		public TableReferenceJoinCollectorImpl(JoinedTableGroupContext tableGroupJoinContext) {
			this.tableGroupJoinContext = tableGroupJoinContext;
		}

		@Override
		public void addRoot(TableReference root) {
			if ( rootTableReference == null ) {
				rootTableReference = root;
			}
			else {
				collectTableReferenceJoin(
						makeJoin( tableGroupJoinContext.getLhs(), rootTableReference )
				);
			}
			predicate = makePredicate( tableGroupJoinContext.getLhs(), rootTableReference );
		}

		private TableReferenceJoin makeJoin(TableGroup lhs, TableReference rootTableReference) {
			return new TableReferenceJoin(
					JoinType.LEFT,
					rootTableReference,
					makePredicate( lhs, rootTableReference )
			);
		}

		private Predicate makePredicate(TableGroup lhs, TableReference rhs) {
			final Junction conjunction = new Junction( Junction.Nature.CONJUNCTION );

			//if ( foreignKey != null ) {
				for ( ColumnMappings.ColumnMapping columnMapping: foreignKey.getColumnMappings().getColumnMappings() ) {
					final ColumnReference referringColumnReference = lhs.resolveColumnReference( columnMapping.getReferringColumn() );
					final ColumnReference targetColumnReference = rhs.resolveColumnReference( columnMapping.getTargetColumn() );

					// todo (6.0) : we need some kind of validation here that the column references are properly defined

					conjunction.add(
							new RelationalPredicate(
									RelationalPredicate.Operator.EQUAL,
									referringColumnReference,
									targetColumnReference
							)
					);
				}
//			}
//			else {
//				final SingularPersistentAttributeEntity attribute = (SingularPersistentAttributeEntity)
//						entityDescriptor.getSingularAttribute( referencedAttributeName );
//				final ForeignKey otherForeignKey = attribute.getForeignKey();
//				for ( ColumnMappings.ColumnMapping columnMapping : otherForeignKey.getColumnMappings().getColumnMappings() ) {
//					conjunction.add(
//							new RelationalPredicate(
//									RelationalPredicate.Operator.EQUAL,
//									lhs.resolveColumnReference( columnMapping.getTargetColumn() ),
//									rhs.resolveColumnReference( columnMapping.getReferringColumn() )
//							)
//					);
//				}
//			}

			return conjunction;
		}

		@Override
		public void collectTableReferenceJoin(TableReferenceJoin tableReferenceJoin) {
			if ( tableReferenceJoins == null ) {
				tableReferenceJoins = new ArrayList<>();
			}
			tableReferenceJoins.add( tableReferenceJoin );
		}

		@SuppressWarnings("WeakerAccess")
		public TableGroupJoin generateTableGroup(
				JoinType joinType,
				TableGroupInfo tableGroupInfoSource,
				JoinedTableGroupContext context) {
			final EntityTableGroup joinedTableGroup = new EntityTableGroup(
					tableGroupInfoSource.getUniqueIdentifier(),
					tableGroupJoinContext.getTableSpace(),
					SingularPersistentAttributeEntity.this,
					context.getLockOptions().getEffectiveLockMode( tableGroupInfoSource.getIdentificationVariable() ),
					context.getNavigablePath(),
					rootTableReference,
					tableReferenceJoins,
					tableGroupJoinContext.getColumnReferenceQualifier()
			);
			return new TableGroupJoin( joinType, joinedTableGroup, predicate );
		}
	}

	@Override
	public SqlSelectionGroupNode resolveSqlSelections(
			ColumnReferenceQualifier qualifier,
			SqlSelectionResolutionContext resolutionContext) {
		// todo (6.0) : handle fetching here?  or at a "higher level"?
		//
		// 		for now we just load the FK
		if ( foreignKey != null ) {
			if ( foreignKey.getColumnMappings().getReferringColumns().size() == 1 ) {
				return resolutionContext.getSqlSelectionResolver().resolveSqlSelection(
						resolutionContext.getSqlSelectionResolver().resolveSqlExpression(
								qualifier,
								foreignKey.getColumnMappings().getReferringColumns().get( 0 )
						)
				);
			}
		}

		final List<SqlSelection> selections = new ArrayList<>();
		if ( foreignKey != null ) {
			for ( Column column: foreignKey.getColumnMappings().getReferringColumns() ) {
				selections.add(
						resolutionContext.getSqlSelectionResolver().resolveSqlSelection(
								resolutionContext.getSqlSelectionResolver().resolveSqlExpression(
										qualifier,
										column
								)
						)
				);
			}
		}

		return new AggregateSqlSelectionGroupNode( selections );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object resolveHydratedState(
			Object hydratedForm,
			ResolutionContext resolutionContext,
			SharedSessionContractImplementor session,
			Object containerInstance) {
		if ( hydratedForm == null ) {
			return null;
		}

		// todo (6.0) : WrongClassException?


		// step 1 - generate EntityKey based on hydrated id form
		final Object resolvedIdentifier = getEntityDescriptor().getHierarchy().getIdentifierDescriptor().resolveHydratedState(
				hydratedForm,
				resolutionContext,
				session,
				null
		);
		final EntityKey entityKey = new EntityKey( resolvedIdentifier, getEntityDescriptor() );


		// step 2 - look for a matching entity (by EntityKey) on the context
		//		NOTE - we pass `! isOptional()` as `eager` to `resolveEntityInstance` because
		//		if it were being fetched dynamically (join fetch) that would have lead to an
		//		EntityInitializer for this entity being created and it would be available in the
		//		`resolutionContext`
		final Object entityInstance = resolutionContext.resolveEntityInstance( entityKey, ! isOptional() );
		if ( entityInstance != null ) {
			return entityInstance;
		}

		// try loading it...
		//
		// todo (6.0) : need to make sure that the "JdbcValues" we are processing here have been added to the Session's stack of "load contexts"
		//		that allows the loader(s) to resolve entity's that are being loaded here.
		//
		//		NOTE : this is how we get around having to register a "holder" EntityEntry with the PersistenceContext
		//		but still letting other (recursive) loads find references we are loading.

		J loaded = getEntityDescriptor().getSingleIdLoader().load(
				resolvedIdentifier,
				NO_LOAD_OPTIONS,
				session
		);

		if ( loaded != null ) {
			return loaded;
		}

		throw new EntityNotFoundException(
				String.format(
						Locale.ROOT,
						"Unable to resolve entity-valued association [%s] foreign key value [%s] to associated entity instance of type [%s]",
						getNavigableRole(),
						resolvedIdentifier,
						getEntityDescriptor().getJavaTypeDescriptor()
				)
		);
	}

	@Override
	public void collectNonNullableTransientEntities(
			Object value,
			ForeignKeys.Nullifier nullifier,
			NonNullableTransientDependencies nonNullableTransientEntities,
			SharedSessionContractImplementor session) {
		if ( isNullable()
				&& getAttributeTypeClassification() != SingularAttributeClassification.ONE_TO_ONE
				&& nullifier.isNullifiable( getEntityDescriptor().getEntityName(), value ) ) {
			nonNullableTransientEntities.add( getEntityDescriptor().getEntityName(), value );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// AllowableParameterType


	private final ValueBinder valueBinder = new ValueBinder() {
		@Override
		public int getNumberOfJdbcParametersNeeded() {
			return getEntityDescriptor().getHierarchy()
					.getIdentifierDescriptor()
					.getNumberOfJdbcParametersNeeded();
		}

		@Override
		@SuppressWarnings("unchecked")
		public void bind(
				PreparedStatement st,
				int index,
				Object value,
				ExecutionContext executionContext) throws SQLException {
			final Object identifier = value == null
					? null
					: getEntityDescriptor().getIdentifier( value, executionContext.getSession() );
			getEntityDescriptor().getHierarchy()
					.getIdentifierDescriptor()
					.getValueBinder( executionContext.getSession().getFactory().getTypeConfiguration() )
					.bind( st, index, identifier, executionContext );
		}

		@Override
		public void bind(
				PreparedStatement st,
				String name,
				Object value,
				ExecutionContext executionContext) throws SQLException {
			final Object identifier = value == null
					? null
					: getEntityDescriptor().getIdentifier( value, executionContext.getSession() );
			getEntityDescriptor().getHierarchy()
					.getIdentifierDescriptor()
					.getValueBinder( executionContext.getSession().getFactory().getTypeConfiguration() )
					.bind( st, name, identifier, executionContext
			);
		}
	};

	@Override
	public ValueBinder getValueBinder(TypeConfiguration typeConfiguration) {
		return valueBinder;
	}

	@Override
	public ValueExtractor getValueExtractor(TypeConfiguration typeConfiguration) {
		return null;
	}

	@Override
	public int getNumberOfJdbcParametersNeeded() {
		return foreignKey.getColumnMappings().getColumnMappings().size();
	}

	@Override
	public AllowableParameterType resolveTemporalPrecision(
			TemporalType temporalType,
			TypeConfiguration typeConfiguration) {
		throw new UnsupportedOperationException( "ManyToOne cannot be treated as temporal type" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean isDirty(Object originalValue, Object currentValue, SharedSessionContractImplementor session) {
		if ( EqualsHelper.areEqual( originalValue, currentValue ) ) {
			return false;
		}

		Object oldIdentifier = unresolve( originalValue, session );
		Object newIdentifier = unresolve( currentValue, session );

		return !getEntityDescriptor()
				.getIdentifierDescriptor()
				.getJavaTypeDescriptor()
				.areEqual( oldIdentifier, newIdentifier );
	}

	@Override
	public List<ColumnReference> resolveColumnReferences(
			ColumnReferenceQualifier qualifier,
			SqlSelectionResolutionContext resolutionContext) {
		List<ColumnReference> columnReferences = new ArrayList<>();
//		if ( foreignKey != null ) {
			for ( Column column: foreignKey.getColumnMappings().getReferringColumns() ) {
				columnReferences.add( new ColumnReference( qualifier, column ) );
			}
//		}
		return columnReferences;
	}

	@Override
	protected void instantiationComplete(
			PersistentAttributeMapping bootModelAttribute,
			RuntimeModelCreationContext context) {
		super.instantiationComplete( bootModelAttribute, context );

		// todo (6.0) : determine mutability plan based
		// for now its set to immutable

		this.mutabilityPlan = ImmutableMutabilityPlan.INSTANCE;
	}

	public ForeignKey getForeignKey() {
		return foreignKey;
	}
}
