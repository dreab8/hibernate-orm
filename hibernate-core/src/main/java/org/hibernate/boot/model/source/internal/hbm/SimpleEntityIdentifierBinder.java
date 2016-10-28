/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitIdentifierColumnNameSource;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.IdentifierSourceSimple;
import org.hibernate.boot.model.source.spi.LocalMetadataBuildingContext;
import org.hibernate.boot.model.source.spi.RelationalValueSourceContainer;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;

/**
 * @author Andrea Boriero
 */
public class SimpleEntityIdentifierBinder extends AbstractEntityIdentifierBinder {
	private final SimpleValueBuilder simpleValueBuilder;

	public SimpleEntityIdentifierBinder(
			Database database,
			SimpleValueBuilder simpleValueBuilder,
			ObjectNameNormalizer objectNameNormalizer) {
		super(database,objectNameNormalizer);
		this.simpleValueBuilder = simpleValueBuilder;
	}

	public void bind(
			MappingDocument sourceDocument,
			final EntityHierarchySourceImpl hierarchySource,
			RootClass rootEntityDescriptor) {
		final IdentifierSourceSimple idSource = (IdentifierSourceSimple) hierarchySource.getIdentifierSource();
		final String propertyName = idSource.getIdentifierAttributeSource().getName();
		final SimpleValue idValue = simpleValueBuilder.buildSimpleValue(
				sourceDocument,
				rootEntityDescriptor.getTable(),
				idSource.getIdentifierAttributeSource().getTypeInformation(),
				( (RelationalValueSourceContainer) idSource.getIdentifierAttributeSource() ).getRelationalValueSources(),
				false,
				createColumnNamingDelegate(
						hierarchySource,
						idSource,
						propertyName
				)
		);

		rootEntityDescriptor.setIdentifier( idValue );

		if ( propertyName == null || !rootEntityDescriptor.hasPojoRepresentation() ) {
			if ( !idValue.isTypeSpecified() ) {
				throw new MappingException(
						"must specify an identifier type: " + rootEntityDescriptor.getEntityName(),
						sourceDocument.getOrigin()
				);
			}
		}
		else {
			idValue.setTypeUsingReflection( rootEntityDescriptor.getClassName(), propertyName );
		}

		if ( propertyName != null ) {
			Property prop = new Property();
			prop.setValue( idValue );
			PropertyBinder.bind(
					sourceDocument,
					idSource.getIdentifierAttributeSource(),
					prop
			);
			rootEntityDescriptor.setIdentifierProperty( prop );
			rootEntityDescriptor.setDeclaredIdentifierProperty( prop );
		}

		makeIdentifier(
				sourceDocument,
				idSource.getIdentifierGeneratorDescriptor(),
				idSource.getUnsavedValue(),
				idValue
		);
	}

	protected RelationalObjectBinder.ColumnNamingDelegate createColumnNamingDelegate(
			final EntityHierarchySourceImpl hierarchySource,
			final IdentifierSourceSimple idSource, final String propertyName) {
		return new RelationalObjectBinder.ColumnNamingDelegate() {
			@Override
			public Identifier determineImplicitName(final LocalMetadataBuildingContext context) {
				context.getBuildingOptions().getImplicitNamingStrategy().determineIdentifierColumnName(
						new ImplicitIdentifierColumnNameSource() {
							@Override
							public EntityNaming getEntityNaming() {
								return hierarchySource.getRoot().getEntityNamingSource();
							}

							@Override
							public AttributePath getIdentifierAttributePath() {
								return idSource.getIdentifierAttributeSource().getAttributePath();
							}

							@Override
							public MetadataBuildingContext getBuildingContext() {
								return context;
							}
						}
				);
				return database.toIdentifier( propertyName );
			}
		};
	}
}
