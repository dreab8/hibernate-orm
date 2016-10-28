/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.Iterator;

import org.hibernate.boot.model.source.spi.AttributeSource;
import org.hibernate.boot.model.source.spi.CascadeStyleSource;
import org.hibernate.boot.model.source.spi.SingularAttributeSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.tuple.GeneratedValueGeneration;
import org.hibernate.tuple.GenerationTiming;

/**
 * @author Andrea Boriero
 */
public class PropertyBinder {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( PropertyBinder.class );

	public static void bind(
			MappingDocument mappingDocument,
			AttributeSource propertySource,
			Property property) {
		property.setName( propertySource.getName() );

		if ( StringHelper.isNotEmpty( propertySource.getXmlNodeName() ) ) {
			DeprecationLogger.DEPRECATION_LOGGER.logDeprecationOfDomEntityModeSupport();
		}

		property.setPropertyAccessorName(
				StringHelper.isNotEmpty( propertySource.getPropertyAccessorName() )
						? propertySource.getPropertyAccessorName()
						: mappingDocument.getMappingDefaults().getImplicitPropertyAccessorName()
		);

		if ( propertySource instanceof CascadeStyleSource ) {
			final CascadeStyleSource cascadeStyleSource = (CascadeStyleSource) propertySource;

			property.setCascade(
					StringHelper.isNotEmpty( cascadeStyleSource.getCascadeStyleName() )
							? cascadeStyleSource.getCascadeStyleName()
							: mappingDocument.getMappingDefaults().getImplicitCascadeStyleName()
			);
		}

		property.setOptimisticLocked( propertySource.isIncludedInOptimisticLocking() );

		if ( propertySource.isSingular() ) {
			final SingularAttributeSource singularAttributeSource = (SingularAttributeSource) propertySource;

			property.setInsertable( singularAttributeSource.isInsertable() );
			property.setUpdateable( singularAttributeSource.isUpdatable() );

			// NOTE : Property#is refers to whether a property is lazy via bytecode enhancement (not proxies)
			property.setLazy( singularAttributeSource.isBytecodeLazy() );

			final GenerationTiming generationTiming = singularAttributeSource.getGenerationTiming();
			if ( generationTiming == GenerationTiming.ALWAYS || generationTiming == GenerationTiming.INSERT ) {
				// we had generation specified...
				//   	HBM only supports "database generated values"
				property.setValueGenerationStrategy( new GeneratedValueGeneration( generationTiming ) );

				// generated properties can *never* be insertable...
				if ( property.isInsertable() ) {
					log.debugf(
							"Property [%s] specified %s generation, setting insertable to false : %s",
							propertySource.getName(),
							generationTiming.name(),
							mappingDocument.getOrigin()
					);
					property.setInsertable( false );
				}

				// properties generated on update can never be updatable...
				if ( property.isUpdateable() && generationTiming == GenerationTiming.ALWAYS ) {
					log.debugf(
							"Property [%s] specified ALWAYS generation, setting updateable to false : %s",
							propertySource.getName(),
							mappingDocument.getOrigin()
					);
					property.setUpdateable( false );
				}
			}
		}

		property.setMetaAttributes( propertySource.getToolingHintContext().getMetaAttributeMap() );

		if ( log.isDebugEnabled() ) {
			final StringBuilder message = new StringBuilder()
					.append( "Mapped property: " )
					.append( propertySource.getName() )
					.append( " -> [" );
			final Iterator itr = property.getValue().getColumnIterator();
			while ( itr.hasNext() ) {
				message.append( ( (Selectable) itr.next() ).getText() );
				if ( itr.hasNext() ) {
					message.append( ", " );
				}
			}
			message.append( "]" );
			log.debug( message.toString() );
		}
	}
}
