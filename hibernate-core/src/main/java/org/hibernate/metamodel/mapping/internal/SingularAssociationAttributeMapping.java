/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.ToOne;
import org.hibernate.metamodel.mapping.Association;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAliasStemHelper;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchDelayedImpl;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchJoinedImpl;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchSelectImpl;
import org.hibernate.type.ForeignKeyDirection;

/**
 * @author Steve Ebersole
 */
public class SingularAssociationAttributeMapping extends AbstractSingularAttributeMapping
		implements EntityValuedFetchable, EntityAssociationMapping, Association, TableGroupJoinProducer {

	public enum Cardinality {
		ONE_TO_ONE,
		MANY_TO_ONE,
		LOGICAL_ONE_TO_ONE
	}

	private final NavigableRole navigableRole;

	private final String sqlAliasStem;
	private final boolean isNullable;
	private final boolean unwrapProxy;
	private final EntityMappingType entityMappingType;

	private final String referencedPropertyName;
	private final boolean referringPrimaryKey;

	private final Cardinality cardinality;

	private ForeignKeyDescriptor foreignKeyDescriptor;
	private ForeignKeyDirection foreignKeyDirection;
	private String identifyingColumnsTableExpression;

	public SingularAssociationAttributeMapping(
			String name,
			NavigableRole navigableRole,
			int stateArrayPosition,
			ToOne bootValue,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			FetchStrategy mappedFetchStrategy,
			EntityMappingType entityMappingType,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess) {
		super(
				name,
				stateArrayPosition,
				attributeMetadataAccess,
				mappedFetchStrategy,
				declaringType,
				propertyAccess
		);
		this.sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromAttributeName( name );
		this.isNullable = bootValue.isNullable();
		this.referencedPropertyName = bootValue.getReferencedPropertyName();
		this.referringPrimaryKey = bootValue.isReferenceToPrimaryKey();
		this.unwrapProxy = bootValue.isUnwrapProxy();
		this.entityMappingType = entityMappingType;

		if ( referringPrimaryKey ) {
			assert referencedPropertyName == null;
		}
		else {
			assert referencedPropertyName != null;
		}

		if ( bootValue instanceof ManyToOne ) {
			final ManyToOne manyToOne = (ManyToOne) bootValue;
			if ( manyToOne.isLogicalOneToOne() ) {
				cardinality = Cardinality.LOGICAL_ONE_TO_ONE;
			}
			else {
				cardinality = Cardinality.MANY_TO_ONE;
			}
		}
		else {
			assert bootValue instanceof OneToOne;
			cardinality = Cardinality.ONE_TO_ONE;
		}

		this.navigableRole = navigableRole;
	}

	public void setForeignKeyDescriptor(ForeignKeyDescriptor foreignKeyDescriptor) {
		this.foreignKeyDescriptor = foreignKeyDescriptor;
	}

	public void setIdentifyingColumnsTableExpression(String tableExpression) {
		identifyingColumnsTableExpression = tableExpression;
	}

	public void setForeignKeyDirection(ForeignKeyDirection direction) {
		foreignKeyDirection = direction;
	}

	public ForeignKeyDescriptor getForeignKeyDescriptor() {
		return this.foreignKeyDescriptor;
	}

	public String getReferencedPropertyName() {
		return referencedPropertyName;
	}

	@Override
	public EntityMappingType getMappedTypeDescriptor() {
		return getEntityMappingType();
	}

	@Override
	public EntityMappingType getEntityMappingType() {
		return entityMappingType;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public Fetch resolveCircularFetch(
			NavigablePath fetchablePath,
			FetchParent fetchParent,
			DomainResultCreationState creationState) {
		// NOTE - a circular fetch reference ultimately needs 2 pieces of information:
		//		1) The NavigablePath that is circular (`fetchablePath`)
		//		2) The NavigablePath to the entity-valued-reference that is the "other side" of the circularity

		final ModelPart parentModelPart = fetchParent.getReferencedModePart();

		if ( ! Fetchable.class.isInstance( parentModelPart ) ) {
			// the `fetchParent` would have to be a Fetch as well for this to be circular...
			return null;
		}

		final FetchParent associationFetchParent = fetchParent.resolveContainingAssociationParent();
		if ( associationFetchParent == null ) {
			return null;
		}
		final ModelPart referencedModePart = associationFetchParent.getReferencedModePart();
		assert referencedModePart instanceof Association;

		final Association associationParent = (Association) referencedModePart;

		if ( foreignKeyDescriptor.equals( associationParent.getForeignKeyDescriptor() ) ) {
			// we need to determine the NavigablePath referring to the entity that the bi-dir
			// fetch will "return" for its Assembler.  so we walk "up" the FetchParent graph
			// to find the "referenced entity" reference

			return createBiDirectionalFetch( fetchablePath, fetchParent );
		}

		// this is the case of a JoinTable
		// 	PARENT(id)
		// 	PARENT_CHILD(parent_id, child_id)
		// 	CHILD(id)
		// 	the FKDescriptor for the association `Parent.child` will be
		//		PARENT_CHILD.child.id -> CHILD.id
		// and the FKDescriptor for the association `Child.parent` will be
		//		PARENT_CHILD.parent.id -> PARENT.id
		// in such a case the associationParent.getIdentifyingColumnExpressions() is PARENT_CHILD.parent_id
		// while the getIdentifyingColumnExpressions for this association is PARENT_CHILD.child_id
		// so we will check if the parentAssociation ForeignKey Target match with the association entity identifier table and columns
		final ForeignKeyDescriptor associationParentForeignKeyDescriptor = associationParent.getForeignKeyDescriptor();
		if ( referencedModePart instanceof SingularAssociationAttributeMapping
				&& ( (SingularAssociationAttributeMapping) referencedModePart ).getDeclaringType() == getPartMappingType() ) {
			if ( this.foreignKeyDescriptor.getReferringTableExpression()
					.equals( associationParentForeignKeyDescriptor.getReferringTableExpression() ) ) {
				final SingleTableEntityPersister entityPersister = (SingleTableEntityPersister) getDeclaringType();
				if ( associationParentForeignKeyDescriptor.getTargetTableExpression()
						.equals( entityPersister.getTableName() ) ) {
					final String[] identifierColumnNames = entityPersister.getIdentifierColumnNames();
					if ( associationParentForeignKeyDescriptor.areTargetColumnNamesEqualsTo( identifierColumnNames ) ) {
						return createBiDirectionalFetch( fetchablePath, fetchParent );
					}
					return null;
				}

			}
		}
		return null;
	}

	@Override
	public EntityFetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final TableGroup lhsTableGroup = sqlAstCreationState.getFromClauseAccess().getTableGroup(
				fetchParent.getNavigablePath()
		);

		if ( fetchTiming == FetchTiming.IMMEDIATE && selected ) {
			if ( sqlAstCreationState.getFromClauseAccess().findTableGroup( fetchablePath ) == null ) {
				SqlAstJoinType sqlAstJoinType;
				if ( isNullable ) {
					sqlAstJoinType = SqlAstJoinType.LEFT;
				}
				else {
					sqlAstJoinType = SqlAstJoinType.INNER;
				}
				final TableGroupJoin tableGroupJoin = createTableGroupJoin(
						fetchablePath,
						lhsTableGroup,
						null,
						sqlAstJoinType,
						lockMode,
						creationState.getSqlAliasBaseManager(),
						creationState.getSqlAstCreationState().getSqlExpressionResolver(),
						creationState.getSqlAstCreationState().getCreationContext()
				);

				sqlAstCreationState.getFromClauseAccess().registerTableGroup(
						fetchablePath,
						tableGroupJoin.getJoinedGroup()
				);
			}

			return new EntityFetchJoinedImpl(
					fetchParent,
					this,
					lockMode,
					true,
					fetchablePath,
					creationState
			);
		}

		//noinspection rawtypes
		final DomainResult keyResult;

		if ( referringPrimaryKey ) {
			keyResult = foreignKeyDescriptor.createDomainResult( fetchablePath, lhsTableGroup, creationState );
		}
		else {
			keyResult = ( (EntityPersister) getDeclaringType() ).getIdentifierMapping()
					.createDomainResult( fetchablePath, lhsTableGroup, null, creationState );
		}

		assert !selected;
		if ( fetchTiming == FetchTiming.IMMEDIATE ) {
			return new EntityFetchSelectImpl(
					fetchParent,
					this,
					lockMode,
					isNullable,
					fetchablePath,
					keyResult,
					creationState
			);
		}

		return new EntityFetchDelayedImpl(
				fetchParent,
				this,
				lockMode,
				isNullable,
				fetchablePath,
				keyResult
		);
	}

	@Override
	public int getNumberOfFetchables() {
		return getEntityMappingType().getNumberOfFetchables();
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAstJoinType sqlAstJoinType,
			LockMode lockMode,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		final String aliasRoot = explicitSourceAlias == null ? sqlAliasStem : explicitSourceAlias;
		final SqlAliasBase sqlAliasBase = aliasBaseGenerator.createSqlAliasBase( aliasRoot );

		final TableReference primaryTableReference = getEntityMappingType().createPrimaryTableReference(
				sqlAliasBase,
				sqlExpressionResolver,
				creationContext
		);

		final TableGroup tableGroup = new StandardTableGroup(
				navigablePath,
				this,
				lockMode,
				primaryTableReference,
				sqlAliasBase,
				(tableExpression) -> getEntityMappingType().containsTableReference( tableExpression ),
				(tableExpression, tg) -> getEntityMappingType().createTableReferenceJoin(
						tableExpression,
						sqlAliasBase,
						primaryTableReference,
						false,
						sqlExpressionResolver,
						creationContext
				),
				creationContext.getSessionFactory()
		);

		final TableReference lhsTableReference = lhs.resolveTableReference( identifyingColumnsTableExpression );

		final TableGroupJoin tableGroupJoin = new TableGroupJoin(
				navigablePath,
				sqlAstJoinType,
				tableGroup,
				foreignKeyDescriptor.generateJoinPredicate(
						lhsTableReference,
						primaryTableReference,
						sqlAstJoinType,
						sqlExpressionResolver,
						creationContext
				)
		);

		lhs.addTableGroupJoin( tableGroupJoin );

		return tableGroupJoin;
	}

	@Override
	public String getSqlAliasStem() {
		return sqlAliasStem;
	}

	public boolean isNullable() {
		return isNullable;
	}

	public boolean isUnwrapProxy() {
		return unwrapProxy;
	}

	@Override
	public EntityMappingType getAssociatedEntityMappingType() {
		return getEntityMappingType();
	}

	@Override
	public ModelPart getKeyTargetMatchPart() {
		return foreignKeyDescriptor;
	}

	@Override
	public String toString() {
		return "SingularAssociationAttributeMapping {" + navigableRole + "}";
	}
}
