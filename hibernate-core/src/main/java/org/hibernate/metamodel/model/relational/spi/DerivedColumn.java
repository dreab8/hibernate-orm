/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class DerivedColumn implements Column {
	private final Table table;
	private final String expression;

	private final TypeConfiguration typeConfiguration;

	private SqlTypeDescriptor sqlTypeDescriptor;
	private BasicJavaDescriptor javaTypeDescriptor;

	private SqlExpressableType sqlExpressableType;

	private boolean isInsertable;
	private boolean isUpdatable;

	public DerivedColumn(
			Table table,
			String expression,
			SqlTypeDescriptor sqlTypeDescriptor,
			TypeConfiguration typeConfiguration,
			boolean insertable,
			boolean updatable) {
		this.table = table;
		this.expression = expression;
		this.sqlTypeDescriptor = sqlTypeDescriptor;
		this.typeConfiguration = typeConfiguration;
		this.isInsertable = insertable;
		this.isUpdatable = updatable;
	}

	public String getExpression() {
		return expression;
	}

	@Override
	public Table getSourceTable() {
		return table;
	}

	@Override
	public String toLoggableString() {
		return "DerivedColumn( " + expression + ")";
	}

	@Override
	public String toString() {
		return toLoggableString();
	}

	@Override
	public String render(String identificationVariable) {
		return expression;
	}

	@Override
	public String render() {
		return expression;
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return sqlTypeDescriptor;
	}

	@Override
	public BasicJavaDescriptor getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	@Override
	public SqlExpressableType getExpressableType() {
		if ( sqlExpressableType == null ) {
			sqlExpressableType = getSqlTypeDescriptor().getSqlExpressableType(
					getJavaTypeDescriptor(),
					typeConfiguration
			);
		}

		return sqlExpressableType;
	}

	@Override
	public Size getSize() {
		throw new UnsupportedOperationException( );
	}

	@Override
	public boolean isInsertable() {
		return isInsertable;
	}

	@Override
	public boolean isUpdatable() {
		return isUpdatable;
	}

	@Override
	public void setInsertable(boolean isInsertable) {
		this.isInsertable = isInsertable;
	}

	@Override
	public void setUpdatable(boolean isUpdatable) {
		this.isUpdatable = isUpdatable;
	}

	@Override
	public Column clone() {
		return new DerivedColumn(
				this.table,
				this.expression,
				this.sqlTypeDescriptor,
				this.typeConfiguration,
				this.isInsertable,
				this.isUpdatable
		);
	}
}
