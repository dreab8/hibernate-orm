/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal.metadata;

import java.util.Properties;

import org.hibernate.envers.configuration.internal.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.mapper.SimpleMapperBuilder;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Value;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.sql.spi.IntegerSqlDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

import org.dom4j.Element;

/**
 * Generates metadata for basic properties: immutable types (including enums).
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public final class BasicMetadataGenerator {

	private static final String ENUM = "enumClass";
	private static final String NAMED = "useNamed";
	private static final String TYPE = "type";

	boolean addBasic(
			Element parent,
			PropertyAuditingData propertyAuditingData,
			Value value,
			SimpleMapperBuilder mapper,
			boolean insertable,
			boolean key) {

		if ( value instanceof BasicValue ) {
			final BasicValue basicValue = (BasicValue) value;
			if ( parent != null ) {
				final Element propMapping = buildProperty(
						parent,
						propertyAuditingData,
						basicValue,
						insertable,
						key
				);

				if ( isAddNestedType( basicValue ) ) {
					applyNestedType( basicValue, propMapping );
				}
			}

			// A null mapper means that we only want to add xml mappings
			if ( mapper != null ) {
				final PropertyData propertyData = propertyAuditingData.resolvePropertyData( value.getType() );
				mapper.add( propertyData );
			}

			return true;
		}

		return false;
	}

	private void mapEnumerationType(Element parent, BasicValue value, Properties parameters) {
		final String enumClass;
		if ( parameters.getProperty( ENUM ) != null ) {
			enumClass = parameters.getProperty( ENUM );
		}
		else {
			enumClass = value.getType().getName();
		}
		parent.addElement( "param" ).addAttribute( "name", ENUM ).setText( enumClass );

		final String useNamed;
		if ( parameters.getProperty( NAMED ) != null ) {
			useNamed = parameters.getProperty( NAMED );
		}
		else {
			final SqlTypeDescriptor descriptor = value.resolveType().getSqlTypeDescriptor();
			useNamed = descriptor.equals( IntegerSqlDescriptor.INSTANCE ) ? "false" : "true";
		}
		parent.addElement( "param" ).addAttribute( "name", NAMED ).setText( useNamed );
	}

	private boolean isAddNestedType(BasicValue value) {
		return value.getTypeParameters() != null;
	}

	private Element buildProperty(
			Element parent,
			PropertyAuditingData propertyAuditingData,
			BasicValue value,
			boolean insertable,
			boolean key) {
		final Element propMapping = MetadataTools.addProperty(
				parent,
				propertyAuditingData.getName(),
				isAddNestedType( value ) ? null : getBasicTypeName( value.getType() ),
				propertyAuditingData.isForceInsertable() || insertable,
				key
		);

		MetadataTools.addColumns( propMapping, value.getColumnIterator() );

		return propMapping;
	}

	private void applyNestedType(BasicValue value, Element propertyMapping) {
		final Properties typeParameters = value.getTypeParameters();
		final Element typeMapping = propertyMapping.addElement( "type" );
		final String typeName = getBasicTypeName( value.getType() );

		typeMapping.addAttribute( "name", typeName );

		if ( javax.persistence.EnumType.class.getName().equals( typeName ) ) {
			// Proper handling of enumeration type
			mapEnumerationType( typeMapping, value, typeParameters );
		}
		else {
			// By default copying all Hibernate properties
			for ( Object object : typeParameters.keySet() ) {
				final String keyType = (String) object;
				final String property = typeParameters.getProperty( keyType );
				if ( property != null ) {
					typeMapping.addElement( "param" ).addAttribute( "name", keyType ).setText( property );
				}
			}
		}
	}

	private String getBasicTypeName(Type type) {
		String typeName = type.getName();
		if ( typeName == null ) {
			typeName = type.getClass().getName();
		}
		return typeName;
	}
}
