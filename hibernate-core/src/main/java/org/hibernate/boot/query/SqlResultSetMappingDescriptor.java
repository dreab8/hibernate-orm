/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.SqlResultSetMapping;

import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.RuntimeMetamodels;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.internal.FetchMementoBasicStandard;
import org.hibernate.query.internal.FetchMementoEntityStandard;
import org.hibernate.query.internal.ModelPartResultMementoBasicImpl;
import org.hibernate.query.internal.NamedResultSetMappingMementoImpl;
import org.hibernate.query.internal.ResultMementoBasicStandard;
import org.hibernate.query.internal.ResultMementoEntityJpa;
import org.hibernate.query.internal.ResultMementoInstantiationStandard;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.named.FetchMemento;
import org.hibernate.query.named.FetchMementoBasic;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.named.ResultMemento;
import org.hibernate.query.named.ResultMementoInstantiation.ArgumentMemento;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class SqlResultSetMappingDescriptor implements NamedResultSetMappingDescriptor {

	// todo (6.0) : we can probably reuse the NamedResultSetMappingDefinition
	//  		implementation between HBM and annotation handling.  We'd
	// 			just need different "builders" for each source and handle the
	//			variances in those builders.  But once we have a
	//			NamedResultSetMappingDefinition and all of its sub-parts,
	//			resolving to a memento is the same
	// 			-
	//			additionally, consider having the sub-parts (the return
	//			representations) be what is used and handed to the
	//			NamedResultSetMappingMemento directly.  They simply need
	//			to be capable of resolving themselves into ResultBuilders
	//			(`org.hibernate.query.results.ResultBuilder`) as part of the
	//			memento for its resolution

	@SuppressWarnings("ForLoopReplaceableByForEach")
	public static SqlResultSetMappingDescriptor from(
			SqlResultSetMapping mappingAnnotation,
			MetadataBuildingContext context) {

		final EntityResult[] entityResults = mappingAnnotation.entities();
		final ConstructorResult[] constructorResults = mappingAnnotation.classes();
		final ColumnResult[] columnResults = mappingAnnotation.columns();

		final List<ResultDescriptor> resultDescriptors = CollectionHelper.arrayList(
				entityResults.length + columnResults.length + columnResults.length
		);

		for ( int i = 0; i < entityResults.length; i++ ) {
			final EntityResult entityResult = entityResults[i];
			resultDescriptors.add(
					new EntityResultDescriptor( entityResult, mappingAnnotation, context )
			);
		}

		for ( int i = 0; i < constructorResults.length; i++ ) {
			final ConstructorResult constructorResult = constructorResults[i];
			resultDescriptors.add(
					new ConstructorResultDescriptor( constructorResult, mappingAnnotation )
			);
		}

		for ( int i = 0; i < columnResults.length; i++ ) {
			final ColumnResult columnResult = columnResults[i];
			resultDescriptors.add(
					new JpaColumnResultDescriptor( columnResult, mappingAnnotation )
			);
		}

		return new SqlResultSetMappingDescriptor(
				mappingAnnotation.name(),
				resultDescriptors,
				context
		);
	}

	private final String mappingName;
	private final List<ResultDescriptor> resultDescriptors;

	private SqlResultSetMappingDescriptor(
			String mappingName,
			List<ResultDescriptor> resultDescriptors,
			MetadataBuildingContext context) {
		this.mappingName = mappingName;
		this.resultDescriptors = resultDescriptors;
	}

	@Override
	public String getRegistrationName() {
		return mappingName;
	}

	@Override
	public NamedResultSetMappingMemento resolve(ResultSetMappingResolutionContext resolutionContext) {
		final List<ResultMemento> resultMementos = CollectionHelper.arrayList( resultDescriptors.size() );

		resultDescriptors.forEach(
				resultDescriptor -> resultMementos.add( resultDescriptor.resolve( resolutionContext ) )
		);

		return new NamedResultSetMappingMementoImpl( mappingName, resultMementos );
	}


	/**
	 * @see jakarta.persistence.ColumnResult
	 */
	private static class JpaColumnResultDescriptor implements ResultDescriptor {
		private final ColumnResult columnResult;
		private final String mappingName;

		public JpaColumnResultDescriptor(
				ColumnResult columnResult,
				SqlResultSetMapping mappingAnnotation) {
			this.columnResult = columnResult;
			this.mappingName = mappingAnnotation.name();
		}

		@Override
		public ResultMemento resolve(ResultSetMappingResolutionContext resolutionContext) {
			BootQueryLogging.LOGGER.debugf(
					"Generating ScalarResultMappingMemento for JPA ColumnResult(%s) for ResultSet mapping `%s`",
					columnResult.name(),
					mappingName
			);

			return new ResultMementoBasicStandard( columnResult, resolutionContext );
		}
	}

	/**
	 * @see jakarta.persistence.ConstructorResult
	 */
	private static class ConstructorResultDescriptor implements ResultDescriptor {
		private static class ArgumentDescriptor {
			private final JpaColumnResultDescriptor argumentResultDescriptor;

			private ArgumentDescriptor(JpaColumnResultDescriptor argumentResultDescriptor) {
				this.argumentResultDescriptor = argumentResultDescriptor;
			}

			ArgumentMemento resolve(ResultSetMappingResolutionContext resolutionContext) {
				return new ArgumentMemento( argumentResultDescriptor.resolve( resolutionContext ) );
			}
		}

		private final String mappingName;

		private final Class<?> targetJavaType;
		private final List<ArgumentDescriptor> argumentResultDescriptors;

		public ConstructorResultDescriptor(
				ConstructorResult constructorResult,
				SqlResultSetMapping mappingAnnotation) {
			this.mappingName = mappingAnnotation.name();
			this.targetJavaType = constructorResult.targetClass();

			final ColumnResult[] columnResults = constructorResult.columns();
			if ( columnResults.length == 0 ) {
				throw new IllegalArgumentException( "ConstructorResult did not define any ColumnResults" );
			}

			this.argumentResultDescriptors = CollectionHelper.arrayList( columnResults.length );
			//noinspection ForLoopReplaceableByForEach
			for ( int i = 0; i < columnResults.length; i++ ) {
				final ColumnResult columnResult = columnResults[i];
				final JpaColumnResultDescriptor argumentResultDescriptor = new JpaColumnResultDescriptor(
						columnResult,
						mappingAnnotation
				);
				argumentResultDescriptors.add( new ArgumentDescriptor( argumentResultDescriptor ) );
			}
		}

		@Override
		public ResultMemento resolve(ResultSetMappingResolutionContext resolutionContext) {
			BootQueryLogging.LOGGER.debugf(
					"Generating InstantiationResultMappingMemento for JPA ConstructorResult(%s) for ResultSet mapping `%s`",
					targetJavaType.getName(),
					mappingName
			);

			final List<ArgumentMemento> argumentResultMementos = new ArrayList<>( argumentResultDescriptors.size() );

			argumentResultDescriptors.forEach(
					(mapping) -> argumentResultMementos.add( mapping.resolve( resolutionContext ) )
			);

			final SessionFactoryImplementor sessionFactory = resolutionContext.getSessionFactory();
			final JavaType<?> targetJtd = sessionFactory.getTypeConfiguration()
					.getJavaTypeDescriptorRegistry()
					.getDescriptor( targetJavaType );

			return new ResultMementoInstantiationStandard( targetJtd, argumentResultMementos );
		}
	}

	/**
	 * @see jakarta.persistence.EntityResult
	 */
	public static class EntityResultDescriptor implements ResultDescriptor {
		@SuppressWarnings( { "FieldCanBeLocal", "FieldMayBeFinal", "unused" } )
		private String resultSetMappingName;

		private final NavigablePath navigablePath;
		private final String entityName;
		private final String discriminatorColumn;

		private final Map<String, AttributeFetchDescriptor> explicitFetchMappings;

		public EntityResultDescriptor(
				EntityResult entityResult,
				SqlResultSetMapping mappingAnnotation,
				MetadataBuildingContext context) {
			this.resultSetMappingName = mappingAnnotation.name();
			this.entityName = entityResult.entityClass().getName();
			this.discriminatorColumn = entityResult.discriminatorColumn();

			this.navigablePath = new NavigablePath( entityName );

			this.explicitFetchMappings = new HashMap<>();
			for ( int i = 0; i < entityResult.fields().length; i++ ) {
				final FieldResult fieldResult = entityResult.fields()[ i ];
				final AttributeFetchDescriptor existing = explicitFetchMappings.get( fieldResult.name() );
				if ( existing != null ) {
					existing.addColumn( fieldResult );
				}
				else {
					explicitFetchMappings.put(
							fieldResult.name(),
							AttributeFetchDescriptor.from( navigablePath, entityName, fieldResult, context )
					);
				}
			}
		}

		@Override
		public ResultMemento resolve(ResultSetMappingResolutionContext resolutionContext) {
			final RuntimeMetamodels runtimeMetamodels = resolutionContext.getSessionFactory().getRuntimeMetamodels();
			final EntityMappingType entityDescriptor = runtimeMetamodels.getEntityMappingType( entityName );

			final FetchMementoBasic discriminatorMemento = resolveDiscriminatorMemento(
					entityDescriptor,
					discriminatorColumn,
					navigablePath
			);

			final Map<String, FetchMemento> fetchMementos = new HashMap<>();
			explicitFetchMappings.forEach(
					(relativePath, fetchDescriptor) -> fetchMementos.put(
							relativePath,
							fetchDescriptor.resolve( resolutionContext )
					)
			);

			return new ResultMementoEntityJpa(
					entityDescriptor,
					LockMode.READ,
					discriminatorMemento,
					fetchMementos
			);
		}

		private static FetchMementoBasic resolveDiscriminatorMemento(
				EntityMappingType entityMapping,
				String discriminatorColumn,
				NavigablePath entityPath) {
			final EntityDiscriminatorMapping discriminatorMapping = entityMapping.getDiscriminatorMapping();
			if ( discriminatorMapping == null ) {
				return null;
			}

			if ( discriminatorColumn == null ) {
				return null;
			}

			return new FetchMementoBasicStandard(
					entityPath.append( EntityDiscriminatorMapping.ROLE_NAME ),
					discriminatorMapping,
					discriminatorColumn
			);
		}
	}

	private static class AttributeFetchDescriptor implements FetchDescriptor {

		private static AttributeFetchDescriptor from(
				NavigablePath entityPath,
				String entityName,
				FieldResult fieldResult,
				MetadataBuildingContext context) {
			return new AttributeFetchDescriptor(
					entityPath,
					entityName,
					fieldResult.name(),
					fieldResult.column()
			);
		}

		private final NavigablePath navigablePath;

		private final String entityName;
		private final String propertyPath;
		private final String[] propertyPathParts;
		private final List<String> columnNames;

		private AttributeFetchDescriptor(
				NavigablePath entityPath,
				String entityName,
				String propertyPath,
				String columnName) {
			this.entityName = entityName;
			this.propertyPath = propertyPath;
			this.propertyPathParts = propertyPath.split( "\\." );
			this.navigablePath = entityPath;
			this.columnNames = new ArrayList<>();
			columnNames.add( columnName );
		}

		private void addColumn(FieldResult fieldResult) {
			if ( ! propertyPath.equals( fieldResult.name() ) ) {
				throw new IllegalArgumentException(
						String.format(
								Locale.ROOT,
								"Passed FieldResult [%s, %s] does not match AttributeFetchMapping [%s]",
								fieldResult.name(),
								fieldResult.column(),
								propertyPath
						)
				);
			}

			columnNames.add( fieldResult.column() );
		}

		@Override
		public ResultMemento asResultMemento(
				NavigablePath path,
				ResultSetMappingResolutionContext resolutionContext) {
			final RuntimeMetamodels runtimeMetamodels = resolutionContext.getSessionFactory().getRuntimeMetamodels();
			final EntityMappingType entityMapping = runtimeMetamodels.getEntityMappingType( entityName );

			final ModelPart subPart = entityMapping.findSubPart( propertyPath, null );

			//noinspection StatementWithEmptyBody
			if ( subPart == null ) {
				// throw an exception
			}

			if ( subPart instanceof BasicValuedModelPart ) {
				assert columnNames.size() == 1;
				final BasicValuedModelPart basicPart = (BasicValuedModelPart) subPart;

				return new ModelPartResultMementoBasicImpl( path, basicPart, columnNames.get( 0 ) );
			}

			throw new NotYetImplementedFor6Exception(
					"Only support for basic-valued model-parts have been implemented : " + propertyPath
					+ " [" + subPart + "]"
			);
		}

		@Override
		public FetchMemento resolve(ResultSetMappingResolutionContext resolutionContext) {
			final RuntimeMetamodels runtimeMetamodels = resolutionContext.getSessionFactory().getRuntimeMetamodels();
			final EntityMappingType entityMapping = runtimeMetamodels.getEntityMappingType( entityName );

			NavigablePath navigablePath = this.navigablePath.append( propertyPathParts[ 0 ] );
			ModelPart subPart = entityMapping.findSubPart(
					propertyPathParts[ 0 ],
					null
			);

			for ( int i = 1; i < propertyPathParts.length; i++ ) {
				if ( ! ( subPart instanceof ModelPartContainer ) ) {
					throw new MappingException(
							"Non-terminal property path [" + navigablePath.getFullPath()
									+ " did not reference FetchableContainer"
					);
				}
				navigablePath = navigablePath.append( propertyPathParts[ i ] );
				subPart = ( (ModelPartContainer) subPart ).findSubPart( propertyPathParts[i], null );
			}

			return getFetchMemento( navigablePath, subPart );
		}

		private FetchMemento getFetchMemento(NavigablePath navigablePath, ModelPart subPart) {
			if ( subPart instanceof BasicValuedModelPart ) {
				assert columnNames.size() == 1;
				final BasicValuedModelPart basicPart = (BasicValuedModelPart) subPart;

				return new FetchMementoBasicStandard( navigablePath, basicPart, columnNames.get( 0 ) );
			}
			else if ( subPart instanceof EntityValuedFetchable ) {
				return new FetchMementoEntityStandard( navigablePath, (EntityValuedFetchable) subPart, columnNames );
			}
			else if( subPart instanceof EmbeddedAttributeMapping ){
				final ModelPart subPart1 = ( (EmbeddedAttributeMapping) subPart ).findSubPart( propertyPath.substring(
						propertyPath.indexOf( "." ) + 1), null );
				return getFetchMemento( navigablePath,subPart1 );
			}
			throw new NotYetImplementedFor6Exception(
					"Only support for basic-valued, entity-valued and embedded model-parts have been implemented : " + propertyPath
							+ " [" + subPart + "]"
			);
		}
	}
}
