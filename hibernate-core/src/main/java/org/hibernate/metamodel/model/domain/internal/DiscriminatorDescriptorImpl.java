/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorMappings;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorMappingsImplicitImpl;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.results.internal.ScalarQueryResultImpl;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.SqlAstCreationContext;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class DiscriminatorDescriptorImpl<O,J> implements DiscriminatorDescriptor<J> {
	public static final String NAVIGABLE_NAME = "{discriminator}";

	private final EntityHierarchy hierarchy;
	private final BasicType<J> basicType;
	private final Column column;

	private final NavigableRole navigableRole;

	private DiscriminatorMappings discriminatorMappings;

	@SuppressWarnings("WeakerAccess")
	public DiscriminatorDescriptorImpl(
			EntityHierarchy hierarchy,
			BasicValueMapping<J> valueMapping,
			RuntimeModelCreationContext creationContext) {
		this.hierarchy = hierarchy;

		this.basicType = valueMapping.resolveType();
		this.column = creationContext.getDatabaseObjectResolver().resolveColumn( valueMapping.getMappedColumn() );

		this.navigableRole = hierarchy.getRootEntityType().getNavigableRole().append( NAVIGABLE_NAME );
	}

	@Override
	public ManagedTypeDescriptor<O> getContainer() {
		return hierarchy.getRootEntityType();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public String getNavigableName() {
		return NAVIGABLE_NAME;
	}

	@Override
	public DiscriminatorMappings getDiscriminatorMappings() {
		return discriminatorMappings;
	}

	public void setDiscriminatorMappings(DiscriminatorMappings discriminatorMappings) {
		this.discriminatorMappings = discriminatorMappings;
	}

	@Override
	public String asLoggableText() {
		return getContainer().asLoggableText() + '.' + NAVIGABLE_NAME;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public Column getBoundColumn() {
		return column;
	}

	@Override
	public BasicType<J> getBasicType() {
		return basicType;
	}

	@Override
	public QueryResult createQueryResult(
			NavigableReference navigableReference,
			String resultVariable,
			SqlAstCreationContext creationContext) {
		return new ScalarQueryResultImpl(
				resultVariable,
				resolveSqlSelection(
						navigableReference.getSqlExpressionQualifier(),
						creationContext
				),
				getBoundColumn().getExpressableType()
		);
	}

	@Override
	public SqlSelection resolveSqlSelection(
			ColumnReferenceQualifier qualifier,
			SqlAstCreationContext resolutionContext) {
		return resolutionContext.getSqlSelectionResolver().resolveSqlSelection(
				resolutionContext.getSqlSelectionResolver().resolveSqlExpression(
						qualifier,
						getBoundColumn()
				),
				getJavaTypeDescriptor(),
				resolutionContext.getSessionFactory().getTypeConfiguration()
		);
	}
}
