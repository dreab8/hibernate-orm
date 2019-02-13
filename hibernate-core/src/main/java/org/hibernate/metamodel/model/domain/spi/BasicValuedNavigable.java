/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.function.BiConsumer;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public interface BasicValuedNavigable<J> extends BasicValuedExpressableType<J>, Navigable<J>, SimpleTypeDescriptor<J>, BasicTypeDescriptor<J> {
	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	Column getBoundColumn();

	BasicValueMapper<J> getValueMapper();

	@Override
	default BasicJavaDescriptor<J> getJavaTypeDescriptor() {
		return getValueMapper().getDomainJavaDescriptor();
	}

	@Override
	default SqlExpressableType getSqlExpressableType() {
		return getValueMapper().getSqlExpressableType();
	}

	@Override
	default Object unresolve(Object value, SharedSessionContractImplementor session) {
		return value;
	}

	@Override
	default void visitColumns(
			BiConsumer<SqlExpressableType, Column> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		// todo (6.0) - formula based navigables have no bound column.
		//		this is a simple fix for now to avoid NPE
		//		we should more than likely make sure the boundColumn instance is a DerivedColumn?
		if ( getBoundColumn() != null ) {
			action.accept( getBoundColumn().getExpressableType(), getBoundColumn() );
		}
	}

	@Override
	default void dehydrate(
			Object value,
			JdbcValueCollector jdbcValueCollector,
			Clause clause,
			SharedSessionContractImplementor session) {
		// todo (6.0) - formula based navigables have no bound column.
		//		this is a simple fix for now to avoid NPE
		//		we should more than likely make sure the boundColumn instance is a DerivedColumn?
		if ( getBoundColumn() != null ) {
			jdbcValueCollector.collect( value, getBoundColumn().getExpressableType(), getBoundColumn() );
		}
	}
}
