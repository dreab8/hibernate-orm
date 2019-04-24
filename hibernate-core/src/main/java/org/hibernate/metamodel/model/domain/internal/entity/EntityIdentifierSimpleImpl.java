/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.entity;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.engine.internal.UnsavedValueFactory;
import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.RepresentationMode;
import org.hibernate.metamodel.model.domain.spi.AbstractSingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.BasicTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.BasicValueMapper;
import org.hibernate.metamodel.model.domain.spi.BasicValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierSimple;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class EntityIdentifierSimpleImpl<O, J>
		extends AbstractSingularPersistentAttribute<O, J>
		implements EntityIdentifierSimple<O, J>, BasicValuedNavigable<J> {

	private final String name;
	private final Column column;
	private final BasicValueMapper<J> valueMapper;
	private final IdentifierGenerator identifierGenerator;
	private final IdentifierValue unsavedValue;

	@SuppressWarnings({ "unchecked", "WeakerAccess" })
	public EntityIdentifierSimpleImpl(
			EntityTypeDescriptor entityTypeDescriptor,
			PersistentClass bootModelEntity,
			RuntimeModelCreationContext creationContext) {
		super(
				entityTypeDescriptor,
				bootModelEntity.getIdentifierAttributeMapping(),
				entityTypeDescriptor.getRepresentationStrategy().generatePropertyAccess(
						bootModelEntity,
						bootModelEntity.getIdentifierAttributeMapping(),
						entityTypeDescriptor,
						creationContext.getSessionFactory().getSessionFactoryOptions().getBytecodeProvider()
				),
				Disposition.ID
		);

		this.name = bootModelEntity.getIdentifierAttributeMapping().getName();

		final BasicValueMapping<J> basicValueMapping = (BasicValueMapping<J>) bootModelEntity
				.getIdentifierAttributeMapping().getValueMapping();
		this.column = entityTypeDescriptor.getColumn( creationContext.getDatabaseObjectResolver()
															  .resolvePhysicalColumnName( basicValueMapping.getMappedColumn() ) );

		this.valueMapper = basicValueMapping.getResolution().getValueMapper();
		this.identifierGenerator = creationContext.getSessionFactory()
				.getIdentifierGenerator( bootModelEntity.getEntityName() );

		unsavedValue = UnsavedValueFactory.getUnsavedIdentifierValue(
				bootModelEntity.getIdentifier().getNullValue(),
				getPropertyAccess().getGetter(),
				basicValueMapping.getJavaTypeMapping().getJavaTypeDescriptor(),
				getConstructor( bootModelEntity )
		);
	}

	@Override
	public BasicTypeDescriptor<J> getNavigableType() {
		return this;
	}

	@Override
	public BasicTypeDescriptor<J> getAttributeType() {
		return (BasicTypeDescriptor<J>) super.getAttributeType();
	}

	@Override
	public boolean hasSingleIdAttribute() {
		return true;
	}

	@Override
	public List<Column> getColumns() {
		return Collections.singletonList( column );
	}

	@Override
	public IdentifierValue getUnsavedValue() {
		return unsavedValue;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SingularPersistentAttribute asAttribute(Class javaType) {
		return this;
	}

	@Override
	public IdentifierGenerator getIdentifierValueGenerator() {
		return identifierGenerator;
	}

	@Override
	public Column getBoundColumn() {
		return column;
	}

	@Override
	public BasicValueMapper<J> getValueMapper() {
		return valueMapper;
	}

	@Override
	public SqlExpressableType getSqlExpressableType() {
		return valueMapper.getSqlExpressableType();
	}

	@Override
	public BasicJavaDescriptor<J> getJavaTypeDescriptor() {
		return (BasicJavaDescriptor<J>) super.getJavaTypeDescriptor();
	}

	@Override
	public BasicTypeDescriptor<J> getType() {
		return getAttributeType();
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.BASIC;
	}

	@Override
	public String asLoggableText() {
		return "IdentifierSimple(" + getContainer().asLoggableText() + ")";
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.BASIC;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitSimpleIdentifier( this );
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public BasicTypeDescriptor<?> getValueGraphType() {
		return getAttributeType();
	}

	@Override
	public SimpleTypeDescriptor<?> getKeyGraphType() {
		return null;
	}

	@Override
	public boolean isOptional() {
		return false;
	}

	@Override
	public Object resolveHydratedState(
			Object hydratedForm,
			ExecutionContext executionContext,
			SharedSessionContractImplementor session,
			Object containerInstance) {
		return hydratedForm;
	}

	@Override
	public void visitJdbcTypes(
			Consumer action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		action.accept( getBoundColumn().getExpressableType() );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitColumns(
			BiConsumer action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		action.accept( getBoundColumn().getExpressableType(), getBoundColumn() );
	}

	private static Constructor getConstructor(PersistentClass persistentClass) {
		if ( persistentClass == null || persistentClass.getExplicitRepresentationMode() != RepresentationMode.POJO ) {
			return null;
		}

		try {
			return ReflectHelper.getDefaultConstructor( persistentClass.getMappedClass() );
		}
		catch (Throwable t) {
			return null;
		}
	}
}
