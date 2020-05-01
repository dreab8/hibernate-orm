package org.hibernate.sql.results.graph.entity.internal;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AbstractFetchParent;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.graph.entity.EntityResultGraphNode;

/**
 * @author Andrea Boriero
 */
public class EntityFetchResultImpl extends AbstractFetchParent implements EntityResultGraphNode, EntityResult {
	private final EntityValuedModelPart referencedModelPart;
	private final DomainResult identifierResult;
	private final DomainResult discriminatorResult;
	private final DomainResult versionResult;
	private final LockMode lockMode;

	public EntityFetchResultImpl(
			NavigablePath navigablePath,
			EntityValuedModelPart entityValuedModelPart,
			DomainResultCreationState creationState) {
		super( entityValuedModelPart.getEntityMappingType(), navigablePath );
		referencedModelPart = entityValuedModelPart;
		lockMode = creationState.getSqlAstCreationState().determineLockMode( null );
		final EntityMappingType entityDescriptor = referencedModelPart.getEntityMappingType();

		final TableGroup entityTableGroup = creationState.getSqlAstCreationState().getFromClauseAccess().findTableGroup( navigablePath );

		identifierResult = entityDescriptor.getIdentifierMapping().createDomainResult(
				navigablePath.append( EntityIdentifierMapping.ROLE_LOCAL_NAME ),
				entityTableGroup,
				null,
				creationState
		);

		final EntityDiscriminatorMapping discriminatorMapping = getDiscriminatorMapping( entityDescriptor, entityTableGroup );
		if ( discriminatorMapping != null ) {
			discriminatorResult = discriminatorMapping.createDomainResult(
					navigablePath.append( EntityDiscriminatorMapping.ROLE_NAME ),
					entityTableGroup,
					null,
					creationState
			);
		}
		else {
			discriminatorResult = null;
		}

		final EntityVersionMapping versionDescriptor = entityDescriptor.getVersionMapping();
		if ( versionDescriptor == null ) {
			versionResult = null;
		}
		else {
			versionResult = versionDescriptor.createDomainResult(
					navigablePath.append( versionDescriptor.getFetchableName() ),
					entityTableGroup,
					null,
					creationState
			);
		}
	}

	protected EntityDiscriminatorMapping getDiscriminatorMapping(
			EntityMappingType entityDescriptor,
			TableGroup entityTableGroup) {
		return entityDescriptor.getDiscriminatorMapping();
	}

	public DomainResult getIdentifierResult() {
		return identifierResult;
	}

	public DomainResult getDiscriminatorResult() {
		return discriminatorResult;
	}

	public DomainResult getVersionResult() {
		return versionResult;
	}

	@Override
	public String getResultVariable() {
		return null;
	}

	@Override
	public DomainResultAssembler createResultAssembler(
			Consumer initializerCollector, AssemblerCreationState creationState) {
		final EntityResultInitializer initializer = new EntityResultInitializer(
				this,
				getNavigablePath(),
				lockMode,
				getIdentifierResult(),
				getDiscriminatorResult(),
				getVersionResult(),
				initializerCollector,
				creationState
		);

		return new EntityAssembler( getResultJavaTypeDescriptor(), initializer );
	}

	@Override
	public FetchableContainer getReferencedMappingType() {
		return getEntityValuedModelPart().getEntityMappingType();
	}

	@Override
	public ModelPart getReferencedModePart() {
		return referencedModelPart;
	}


	@Override
	public EntityValuedModelPart getEntityValuedModelPart() {
		return referencedModelPart;
	}

	@Override
	public EntityMappingType getReferencedMappingContainer() {
		return getEntityValuedModelPart().getEntityMappingType();
	}

	@Override
	public String toString() {
		return "EntityFetchResultImpl {" + getNavigablePath() + "}";
	}
}
