/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.source.spi.AttributeSource;
import org.hibernate.boot.model.source.spi.EmbeddableSource;
import org.hibernate.boot.model.source.spi.LocalMetadataBuildingContext;
import org.hibernate.boot.model.source.spi.PluralAttributeSource;
import org.hibernate.boot.model.source.spi.SingularAttributeSourceAny;
import org.hibernate.boot.model.source.spi.SingularAttributeSourceBasic;
import org.hibernate.boot.model.source.spi.SingularAttributeSourceEmbedded;
import org.hibernate.boot.model.source.spi.SingularAttributeSourceManyToOne;
import org.hibernate.boot.model.source.spi.SingularAttributeSourceOneToOne;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.Property;

/**
 * @author Andrea Boriero
 */
public class ComponentBinder {
	private void bindComponent(
			MappingDocument sourceDocument,
			String role,
			EmbeddableSource embeddableSource,
			Component componentBinding,
			String explicitComponentClassName,
			String containingClassName,
			String propertyName,
			boolean isVirtual,
			boolean isDynamic,
			String xmlNodeName) {

		componentBinding.setMetaAttributes( embeddableSource.getToolingHintContext().getMetaAttributeMap() );

		componentBinding.setRoleName( role );

		componentBinding.setEmbedded( isVirtual );

		// todo : better define the conditions in this if/else
		if ( isDynamic ) {
			// dynamic is represented as a Map
			log.debugf( "Binding dynamic-component [%s]", role );
			componentBinding.setDynamic( true );
		}
		else if ( isVirtual ) {
			// virtual (what used to be called embedded) is just a conceptual composition...
			// <properties/> for example
			if ( componentBinding.getOwner().hasPojoRepresentation() ) {
				log.debugf( "Binding virtual component [%s] to owner class [%s]", role, componentBinding.getOwner().getClassName() );
				componentBinding.setComponentClassName( componentBinding.getOwner().getClassName() );
			}
			else {
				log.debugf( "Binding virtual component [%s] as dynamic", role );
				componentBinding.setDynamic( true );
			}
		}
		else {
			log.debugf( "Binding component [%s]", role );
			if ( StringHelper.isNotEmpty( explicitComponentClassName ) ) {
				log.debugf( "Binding component [%s] to explicitly specified class", role, explicitComponentClassName );
				componentBinding.setComponentClassName( explicitComponentClassName );
			}
			else if ( componentBinding.getOwner().hasPojoRepresentation() ) {
				log.tracef( "Attempting to determine component class by reflection %s", role );
				final Class reflectedComponentClass;
				if ( StringHelper.isNotEmpty( containingClassName ) && StringHelper.isNotEmpty( propertyName ) ) {
					reflectedComponentClass = Helper.reflectedPropertyClass(
							sourceDocument,
							containingClassName,
							propertyName
					);
				}
				else {
					reflectedComponentClass = null;
				}

				if ( reflectedComponentClass == null ) {
					log.debugf(
							"Unable to determine component class name via reflection, and explicit " +
									"class name not given; role=[%s]",
							role
					);
				}
				else {
					componentBinding.setComponentClassName( reflectedComponentClass.getName() );
				}
			}
			else {
				componentBinding.setDynamic( true );
			}
		}

		String nodeName = xmlNodeName;
		if ( StringHelper.isNotEmpty( nodeName ) ) {
			DeprecationLogger.DEPRECATION_LOGGER.logDeprecationOfDomEntityModeSupport();
		}

		// todo : anything else to pass along?
		bindAllCompositeAttributes(
				sourceDocument,
				embeddableSource,
				componentBinding
		);

		if ( embeddableSource.getParentReferenceAttributeName() != null ) {
			componentBinding.setParentProperty( embeddableSource.getParentReferenceAttributeName() );
		}

		if ( embeddableSource.isUnique() ) {
			final ArrayList<Column> cols = new ArrayList<Column>();
			final Iterator itr = componentBinding.getColumnIterator();
			while ( itr.hasNext() ) {
				final Object selectable = itr.next();
				// skip formulas.  ugh, yes terrible naming of these methods :(
				if ( !Column.class.isInstance( selectable ) ) {
					continue;
				}
				cols.add( (Column) selectable );
			}
			// todo : we may need to delay this
			componentBinding.getOwner().getTable().createUniqueKey( cols );
		}

		if ( embeddableSource.getTuplizerClassMap() != null ) {
			if ( embeddableSource.getTuplizerClassMap().size() > 1 ) {
				DeprecationLogger.DEPRECATION_LOGGER.logDeprecationOfMultipleEntityModeSupport();
			}
			for ( Map.Entry<EntityMode,String> tuplizerEntry : embeddableSource.getTuplizerClassMap().entrySet() ) {
				componentBinding.addTuplizer(
						tuplizerEntry.getKey(),
						tuplizerEntry.getValue()
				);
			}
		}
	}

	private void bindAllCompositeAttributes(
			MappingDocument sourceDocument,
			EmbeddableSource embeddableSource,
			Component component) {

		for ( AttributeSource attributeSource : embeddableSource.attributeSources() ) {
			Property attribute = null;

			if ( SingularAttributeSourceBasic.class.isInstance( attributeSource ) ) {
				final SingularAttributeSourceBasic singularAttributeSource = (SingularAttributeSourceBasic) attributeSource;
				attribute = createBasicAttribute(
						sourceDocument,
						singularAttributeSource,
						simpleValueBuilder.buildSimpleValue(
								sourceDocument,
								component.getTable(),
								attributeSource.getTypeInformation(),
								singularAttributeSource.getRelationalValueSources(),
								singularAttributeSource.areValuesNullableByDefault(),
								new RelationalObjectBinder.ColumnNamingDelegate() {
									@Override
									public Identifier determineImplicitName(LocalMetadataBuildingContext context) {
										return implicitNamingStrategy.determineBasicColumnName( singularAttributeSource );
									}
								}
						),
						component.getComponentClassName()
				);
			}
			else if ( SingularAttributeSourceEmbedded.class.isInstance( attributeSource ) ) {
				attribute = createEmbeddedAttribute(
						sourceDocument,
						(SingularAttributeSourceEmbedded) attributeSource,
						new Component( sourceDocument.getMetadataCollector(), component ),
						component.getComponentClassName()
				);
			}
			else if ( SingularAttributeSourceManyToOne.class.isInstance( attributeSource ) ) {
				attribute = createManyToOneAttribute(
						sourceDocument,
						(SingularAttributeSourceManyToOne) attributeSource,
						new ManyToOne( sourceDocument.getMetadataCollector(), component.getTable() ),
						component.getComponentClassName()
				);
			}
			else if ( SingularAttributeSourceOneToOne.class.isInstance( attributeSource ) ) {
				attribute = createOneToOneAttribute(
						sourceDocument,
						(SingularAttributeSourceOneToOne) attributeSource,
						new OneToOne( sourceDocument.getMetadataCollector(), component.getTable(), component.getOwner() ),
						component.getComponentClassName()
				);
			}
			else if ( SingularAttributeSourceAny.class.isInstance( attributeSource ) ) {
				attribute = createAnyAssociationAttribute(
						sourceDocument,
						(SingularAttributeSourceAny) attributeSource,
						new Any( sourceDocument.getMetadataCollector(), component.getTable() ),
						component.getComponentClassName()
				);
			}
			else if ( PluralAttributeSource.class.isInstance( attributeSource ) ) {
				attribute = createPluralAttribute(
						sourceDocument,
						(PluralAttributeSource) attributeSource,
						component.getOwner()
				);
			}
			else {
				throw new AssertionFailure(
						String.format(
								Locale.ENGLISH,
								"Unexpected AttributeSource sub-type [%s] as part of composite [%s]",
								attributeSource.getClass().getName(),
								attributeSource.getAttributeRole().getFullPath()
						)

				);
			}

			component.addProperty( attribute );
		}
	}
}
