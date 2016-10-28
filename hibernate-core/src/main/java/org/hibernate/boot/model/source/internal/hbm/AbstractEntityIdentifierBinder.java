/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.Properties;

import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.SimpleValue;

/**
 * @author Andrea Boriero
 */
public class AbstractEntityIdentifierBinder {

	final Database database;
	final ObjectNameNormalizer objectNameNormalizer;

	public AbstractEntityIdentifierBinder(
			Database database,
			ObjectNameNormalizer objectNameNormalizer) {
		this.database = database;
		this.objectNameNormalizer = objectNameNormalizer;
	}

	void makeIdentifier(
			final MappingDocument sourceDocument,
			IdentifierGeneratorDefinition generator,
			String unsavedValue,
			SimpleValue identifierValue) {
		if ( generator != null ) {
			addGenerator( sourceDocument, generator, identifierValue );
		}

		identifierValue.getTable().setIdentifierValue( identifierValue );

		if ( StringHelper.isNotEmpty( unsavedValue ) ) {
			identifierValue.setNullValue( unsavedValue );
		}
		else {
			if ( "assigned".equals( identifierValue.getIdentifierGeneratorStrategy() ) ) {
				identifierValue.setNullValue( "undefined" );
			}
			else {
				identifierValue.setNullValue( null );
			}
		}
	}

	private void addGenerator(
			MappingDocument sourceDocument,
			IdentifierGeneratorDefinition generator,
			SimpleValue identifierValue) {
		String generatorName = generator.getStrategy();
		Properties params = new Properties();

		// see if the specified generator name matches a registered <identifier-generator/>
		IdentifierGeneratorDefinition generatorDef = sourceDocument.getMetadataCollector().getIdentifierGenerator(
				generatorName );
		if ( generatorDef != null ) {
			generatorName = generatorDef.getStrategy();
			params.putAll( generatorDef.getParameters() );
		}

		identifierValue.setIdentifierGeneratorStrategy( generatorName );

		// YUCK!  but cannot think of a clean way to do this given the string-config based scheme
		params.put( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER, objectNameNormalizer );

		if ( database.getDefaultNamespace().getPhysicalName().getSchema() != null ) {
			params.setProperty(
					PersistentIdentifierGenerator.SCHEMA,
					database.getDefaultNamespace().getPhysicalName().getSchema().render( database.getDialect() )
			);
		}
		if ( database.getDefaultNamespace().getPhysicalName().getCatalog() != null ) {
			params.setProperty(
					PersistentIdentifierGenerator.CATALOG,
					database.getDefaultNamespace().getPhysicalName().getCatalog().render( database.getDialect() )
			);
		}

		params.putAll( generator.getParameters() );

		identifierValue.setIdentifierGeneratorProperties( params );
	}
}
