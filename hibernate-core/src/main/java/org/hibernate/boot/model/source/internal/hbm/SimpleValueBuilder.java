/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.List;
import java.util.Properties;

import org.hibernate.boot.model.source.spi.ColumnSource;
import org.hibernate.boot.model.source.spi.HibernateTypeSource;
import org.hibernate.boot.model.source.spi.RelationalValueSource;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;

/**
 * @author Andrea Boriero
 */
public class SimpleValueBuilder {

	private final RelationalObjectBinder relationalObjectBinder;

	public SimpleValueBuilder(RelationalObjectBinder relationalObjectBinder) {
		this.relationalObjectBinder = relationalObjectBinder;
	}

	public SimpleValue buildSimpleValue(
			MappingDocument mappingDocument,
			Table table,
			HibernateTypeSource typeSource,
			List<RelationalValueSource> relationalValueSources,
			boolean areColumnsNullableByDefault,
			RelationalObjectBinder.ColumnNamingDelegate columnNamingDelegate) {
		final SimpleValue value = new SimpleValue(
				mappingDocument.getMetadataCollector(),
				table
		);
		bindSimpleValueType( mappingDocument, typeSource, value );

		relationalObjectBinder.bindColumnsAndFormulas(
				mappingDocument,
				relationalValueSources,
				value,
				areColumnsNullableByDefault,
				columnNamingDelegate
		);
		return value;
	}

	public SimpleValue buildIdentifierValue(
			MappingDocument mappingDocument,
			Table table,
			HibernateTypeSource typeSource,
			ColumnSource columnSource,
			RelationalObjectBinder.ColumnNamingDelegate columnNamingDelegate) {
		final SimpleValue value = new SimpleValue(
				mappingDocument.getMetadataCollector(),
				table
		);
		bindSimpleValueType( mappingDocument, typeSource, value );

		relationalObjectBinder.bindColumn(
				mappingDocument,
				columnSource,
				value,
				false,
				columnNamingDelegate
		);
		return value;
	}

	public SimpleValue buildVersionValue(
			MappingDocument mappingDocument,
			Table table,
			HibernateTypeSource typeSource) {
		final SimpleValue value = new SimpleValue(
				mappingDocument.getMetadataCollector(),
				table
		);
		bindSimpleValueType( mappingDocument, typeSource, value );
		value.makeVersion();
		return value;
	}

	private void bindSimpleValueType(
			MappingDocument mappingDocument,
			HibernateTypeSource typeSource,
			SimpleValue simpleValue) {
		if ( mappingDocument.getBuildingOptions().useNationalizedCharacterData() ) {
			simpleValue.makeNationalized();
		}

		final ModelBinder.TypeResolution typeResolution = ModelBinder.resolveType( mappingDocument, typeSource );
		if ( typeResolution == null ) {
			// no explicit type info was found
			return;
		}

		final Properties parameters = typeResolution.getParameters();
		if ( CollectionHelper.isNotEmpty( parameters ) ) {
			simpleValue.setTypeParameters( parameters );
		}

		final String typeName = typeResolution.getTypeName();
		if ( typeName != null ) {
			simpleValue.setTypeName( typeName );
		}
	}
}
