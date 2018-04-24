/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.mapping;

import javax.persistence.AttributeConverter;

import org.hibernate.MappingException;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.type.spi.BasicTypeResolver;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.BasicTypeResolverConvertibleSupport;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.type.Type;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public class BasicValue extends SimpleValue {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( BasicValue.class );

	private boolean isNationalized;
	private boolean isLob;
	private ConverterDescriptor attributeConverterDescriptor;
	private BasicTypeResolver basicTypeResolver;
	private BasicType basicType;

	public BasicValue(MetadataBuildingContext buildingContext, Table table) {
		super( buildingContext,table );
	}

	public ConverterDescriptor getAttributeConverterDescriptor() {
		return attributeConverterDescriptor;
	}

	public boolean isNationalized() {
		return isNationalized;
	}

	public boolean isLob() {
		return isLob;
	}

	public void setJpaAttributeConverterDescriptor(ConverterDescriptor attributeConverterDescriptor) {
		this.attributeConverterDescriptor = attributeConverterDescriptor;
	}

	public void setBasicTypeResolver(BasicTypeResolver basicTypeResolver) {
		this.basicTypeResolver = basicTypeResolver;
	}

	public void makeNationalized() {
		this.isNationalized = true;
	}

	public void makeLob() {
		this.isLob = true;
	}

	@Override
	public void addColumn(Column column) {
		if ( getColumnSpan() > 0 ) {
			throw new MappingException( "Attempt to add additional MappedColumn to BasicValueMapping " + column.getName() );
		}
		super.addColumn( column );
	}

	@Override
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	public class BasicValueTypeDescriptorResolver implements TypeDescriptorResolver {
		@Override
		public SqlTypeDescriptor resolveSqlTypeDescriptor() {
			return resolveType().getSqlTypeDescriptor();
		}

		@Override
		public JavaTypeDescriptor resolveJavaTypeDescriptor() {
			return resolveType().getJavaTypeDescriptor();
		}
	}

	@Override
	public void addFormula(Formula formula) {
		if ( getColumnSpan() > 0 ) {
			throw new MappingException( "Attempt to add additional MappedColumn to BasicValueMapping" );
		}
		super.addFormula( formula );
	}

	public void setTypeName(String typeName) {
		if ( typeName != null && typeName.startsWith( AttributeConverterDefinition.EXPLICIT_TYPE_NAME_PREFIX ) ) {
			final String converterClassName = typeName.substring( AttributeConverterDefinition.EXPLICIT_TYPE_NAME_PREFIX.length() );
			final ClassLoaderService cls = getMetadataBuildingContext()
					.getMetadataCollector()
					.getMetadataBuildingOptions()
					.getServiceRegistry()
					.getService( ClassLoaderService.class );
			try {
				final Class<? extends AttributeConverter> converterClass = cls.classForName( converterClassName );
				this.attributeConverterDescriptor = new ClassBasedConverterDescriptor(
						converterClass,
						false,
						getMetadataBuildingContext().getBootstrapContext()
				);
				return;
			}
			catch (Exception e) {
				log.logBadHbmAttributeConverterType( typeName, e.getMessage() );
			}
		}
		super.setTypeName( typeName );
	}

	public BasicType resolveType() {
		if ( basicType == null ) {
			basicType = basicTypeResolver.resolveBasicType();
		}
		return basicType;
	}

	@Override
	public boolean isTypeSpecified() {
		// We mandate a BasicTypeResolver, so this is always true.
		return true;
	}

	@Override
	public void setTypeUsingReflection(String className, String propertyName) throws MappingException {
		// todo (6.0) - this check seems sillyo
		//		Several places call this method and its possible there are situations where the className
		//		is null because we're dealing with non-pojo cases.  In those cases, we cannot use reflection
		//		to determine type, so we don't overwrite the BasicTypeResolver that is already set.
		//
		//		Ideally can we remove this method call and somehow bake this into `#setType` ?
		if ( className != null ) {
			this.basicTypeResolver = new BasicTypeResolverUsingReflection(
					getMetadataBuildingContext(),
					getAttributeConverterDescriptor(),
					className,
					propertyName,
					isLob(),
					isNationalized()
			);
		}
	}

	public static class BasicTypeResolverUsingReflection extends BasicTypeResolverConvertibleSupport {
		private final JavaTypeDescriptor javaTypeDescriptor;
		private final SqlTypeDescriptor sqlTypeDescriptor;
		private final boolean isLob;
		private final boolean isNationalized;

		public BasicTypeResolverUsingReflection(
				MetadataBuildingContext buildingContext,
				ConverterDescriptor converterDefinition,
				String className,
				String propertyName,
				boolean isLob,
				boolean isNationalized) {
			super( buildingContext, converterDefinition );
			this.isLob = isLob;
			this.isNationalized = isNationalized;

			if ( converterDefinition == null ) {
				final Class attributeType = ReflectHelper.reflectedPropertyClass(
						className,
						propertyName,
						buildingContext.getBootstrapContext().getServiceRegistry().getService( ClassLoaderService.class )
				);
				javaTypeDescriptor = buildingContext.getBootstrapContext()
						.getTypeConfiguration()
						.getJavaTypeDescriptorRegistry()
						.getDescriptor( attributeType );
				sqlTypeDescriptor = javaTypeDescriptor
						.getJdbcRecommendedSqlType(
								buildingContext.getBootstrapContext()
										.getTypeConfiguration()
										.getBasicTypeRegistry()
										.getBaseJdbcRecommendedSqlTypeMappingContext()
						);
			}
			else {
				javaTypeDescriptor = converterDefinition.getDomainType();
				sqlTypeDescriptor = converterDefinition
						.getJdbcType()
						.getJdbcRecommendedSqlType(
								buildingContext.getBootstrapContext()
										.getTypeConfiguration()
										.getBasicTypeRegistry()
										.getBaseJdbcRecommendedSqlTypeMappingContext()
						);
			}
		}

		@Override
		public BasicJavaDescriptor getJavaTypeDescriptor() {
			return (BasicJavaDescriptor) javaTypeDescriptor;
		}

		@Override
		public SqlTypeDescriptor getSqlTypeDescriptor() {
			return sqlTypeDescriptor;
		}

		@Override
		public boolean isNationalized() {
			return isNationalized;
		}

		@Override
		public boolean isLob() {
			return isLob;
		}

		@Override
		public int getPreferredSqlTypeCodeForBoolean() {
			return ConfigurationHelper
					.getPreferredSqlTypeCodeForBoolean(
							getBuildingContext().getBootstrapContext().getServiceRegistry()
					);
		}
	}

	public Type getType() throws MappingException {
		if ( basicType == null ) {
			basicType = resolveType();
		}
		return basicType;
	}

	public void copyTypeFrom( SimpleValue sourceValue ) {
		super.copyTypeFrom( sourceValue );
		basicTypeResolver = ((BasicValue)sourceValue).basicTypeResolver;
	}
}
