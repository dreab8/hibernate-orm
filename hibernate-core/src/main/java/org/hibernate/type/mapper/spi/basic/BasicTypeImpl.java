/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.mapper.spi.basic;

import java.util.Comparator;

import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.descriptor.spi.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.spi.sql.SqlTypeDescriptor;
import org.hibernate.type.mapper.spi.JdbcLiteralFormatterConvertedImpl;
import org.hibernate.type.mapper.spi.ColumnMapping;
import org.hibernate.type.mapper.spi.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.spi.MutabilityPlan;

/**
 * AbstractBasicTypeImpl implementation based on construction binding of the delegates
 *
 * @author Steve Ebersole
 */
public class BasicTypeImpl<T> extends AbstractBasicTypeImpl<T> {
	private final ColumnMapping columnMapping;

	private final JavaTypeDescriptor<T> domainJavaType;

	private final MutabilityPlan<T> mutabilityPlan;
	private final Comparator<T> comparator;

	private final AttributeConverterDefinition<T,?> attributeConverterDefinition;

	private final JdbcLiteralFormatter<T> jdbcLiteralFormatter;

	/**
	 * Constructor form for building a basic type without an AttributeConverter
	 *
	 * @param domainJavaType The descriptor for the domain model Java type.
	 * @param sqlType The descriptor for the JDBC type.
	 */
	public BasicTypeImpl(JavaTypeDescriptor<T> domainJavaType, SqlTypeDescriptor sqlType) {
		this( domainJavaType, sqlType, null );
	}

	/**
	 * Constructor form for building a basic type without an AttributeConverter
	 *
	 * @param domainJavaType The descriptor for the domain model Java type.
	 * @param sqlType The descriptor for the JDBC type.
	 */
	public BasicTypeImpl(JavaTypeDescriptor<T> domainJavaType, SqlTypeDescriptor sqlType, JdbcLiteralFormatter<T> jdbcLiteralFormatter) {
		this( domainJavaType, sqlType, null, null, null, jdbcLiteralFormatter );
	}

	/**
	 * Constructor form for building a basic type without an AttributeConverter
	 *
	 * @param domainJavaType The descriptor for the domain model Java type.
	 * @param sqlType The descriptor for the JDBC type.
	 * @param mutabilityPlan The Type-specific MutabilityPlan.  May be {@code null} indicating to
	 * use the MutabilityPlan as defined by {@link JavaTypeDescriptor#getMutabilityPlan()}
	 * @param comparator The Type-specific Comparator.  May be {@code null} indicating to
	 * use the Comparator as defined by {@link JavaTypeDescriptor#getComparator()} ()}
	 */
	public BasicTypeImpl(
			JavaTypeDescriptor<T> domainJavaType,
			SqlTypeDescriptor sqlType,
			MutabilityPlan<T> mutabilityPlan,
			Comparator<T> comparator) {
		this( domainJavaType, sqlType, mutabilityPlan, comparator, null );
	}

	/**
	 * Constructor form for building a basic type with an AttributeConverter.
	 * <p/>
	 * Notice that 2 different JavaTypeDescriptor instances are passed in here.  {@code domainJavaType} represents
	 * the Java type in the user's domain model.  {@code intermediateJavaType} represents the Java type expressed
	 * by the AttributeConverter as the "database type".  We will read the database value initially using the
	 * {@code sqlType} + {@code intermediateJavaType}.  We then pass that value along to the AttributeConverter
	 * to convert to the domain Java type.
	 *
	 * @param domainJavaType The descriptor for the domain model Java type.
	 * @param sqlType The descriptor for the JDBC type.
	 * @param mutabilityPlan The Type-specific MutabilityPlan.  May be {@code null} indicating to
	 * use the MutabilityPlan as defined by {@link JavaTypeDescriptor#getMutabilityPlan()}
	 * @param comparator The Type-specific Comparator.  May be {@code null} indicating to
	 * use the Comparator as defined by {@link JavaTypeDescriptor#getComparator()} ()}
	 * @param attributeConverterDefinition The AttributeConverterDefinition to apply
	 */
	public BasicTypeImpl(
			JavaTypeDescriptor<T> domainJavaType,
			SqlTypeDescriptor sqlType,
			MutabilityPlan<T> mutabilityPlan,
			Comparator<T> comparator,
			AttributeConverterDefinition<T,?> attributeConverterDefinition) {
		this( domainJavaType, sqlType, mutabilityPlan, comparator, attributeConverterDefinition, null );
	}

	/**
	 * The full constructor form.
	 *
	 * Constructor form for building a basic type with an AttributeConverter.
	 * <p/>
	 * Notice that 2 different JavaTypeDescriptor instances are passed in here.  {@code domainJavaType} represents
	 * the Java type in the user's domain model.  {@code intermediateJavaType} represents the Java type expressed
	 * by the AttributeConverter as the "database type".  We will read the database value initially using the
	 * {@code sqlType} + {@code intermediateJavaType}.  We then pass that value along to the AttributeConverter
	 * to convert to the domain Java type.
	 *
	 * @param domainJavaType The descriptor for the domain model Java type.
	 * @param sqlType The descriptor for the JDBC type.
	 * @param mutabilityPlan The Type-specific MutabilityPlan.  May be {@code null} indicating to
	 * use the MutabilityPlan as defined by {@link JavaTypeDescriptor#getMutabilityPlan()}
	 * @param comparator The Type-specific Comparator.  May be {@code null} indicating to
	 * use the Comparator as defined by {@link JavaTypeDescriptor#getComparator()} ()}
	 * @param attributeConverterDefinition The AttributeConverterDefinition to apply
	 */
	public BasicTypeImpl(
			JavaTypeDescriptor<T> domainJavaType,
			SqlTypeDescriptor sqlType,
			MutabilityPlan<T> mutabilityPlan,
			Comparator<T> comparator,
			AttributeConverterDefinition<T,?> attributeConverterDefinition,
			JdbcLiteralFormatter<T> jdbcLiteralFormatter) {
		this.domainJavaType = domainJavaType;

		this.columnMapping = new ColumnMapping( sqlType );

		this.mutabilityPlan = mutabilityPlan == null ? domainJavaType.getMutabilityPlan() : mutabilityPlan;
		this.comparator = comparator == null ? domainJavaType.getComparator() : comparator;

		this.attributeConverterDefinition = attributeConverterDefinition;

		this.jdbcLiteralFormatter = resolveJdbcLiteralFormatter(
				jdbcLiteralFormatter,
				attributeConverterDefinition,
				domainJavaType,
				sqlType
		);
	}

	@SuppressWarnings("unchecked")
	protected JdbcLiteralFormatter<T> resolveJdbcLiteralFormatter(
			JdbcLiteralFormatter<T> jdbcLiteralFormatter,
			AttributeConverterDefinition<T, ?> attributeConverterDefinition,
			JavaTypeDescriptor<T> domainJavaType,
			SqlTypeDescriptor sqlType) {
		// if there is an AttributeConverter applied we will need special handling
		if ( attributeConverterDefinition != null ) {
			return new JdbcLiteralFormatterConvertedImpl<T>( (AttributeConverterDefinition<T, Object>) attributeConverterDefinition );
		}

		if ( jdbcLiteralFormatter != null ) {
			return jdbcLiteralFormatter;
		}

		return sqlType.getJdbcLiteralFormatter( domainJavaType );
	}

	@Override
	public JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return domainJavaType;
	}

	@Override
	public ColumnMapping getColumnMapping() {
		return columnMapping;
	}

	public AttributeConverterDefinition<T,?> getAttributeConverterDefinition() {
		return attributeConverterDefinition;
	}

	@Override
	public JdbcLiteralFormatter<T> getJdbcLiteralFormatter() {
		return jdbcLiteralFormatter;
	}

	@Override
	public MutabilityPlan<T> getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public Comparator<T> getComparator() {
		return comparator;
	}
}
