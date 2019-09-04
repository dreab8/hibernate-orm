/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import java.util.List;

import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.sql.ast.spi.SqlAstWalker;

/**
 * @author Steve Ebersole
 */
public class SqlTuple implements Expression {
	private final List<? extends Expression> expressions;
	private final MappingModelExpressable valueMapping;

	public SqlTuple(List<? extends Expression> expressions, MappingModelExpressable valueMapping) {
		this.expressions = expressions;
		this.valueMapping = valueMapping;
	}

	@Override
	public MappingModelExpressable getExpressionType() {
		return valueMapping;
	}

	public List<? extends Expression> getExpressions(){
		return expressions;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitTuple( this );
	}
}
