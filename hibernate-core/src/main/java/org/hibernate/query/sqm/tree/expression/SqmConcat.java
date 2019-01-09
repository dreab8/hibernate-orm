/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmConcat implements SqmExpression {
	private final SqmExpression lhsOperand;
	private final SqmExpression rhsOperand;

	private final BasicValuedExpressableType resultType;

	public SqmConcat(SqmExpression lhsOperand, SqmExpression rhsOperand) {
		this( lhsOperand, rhsOperand, (BasicValuedExpressableType) lhsOperand.getExpressableType() );
	}

	public SqmConcat(SqmExpression lhsOperand, SqmExpression rhsOperand, BasicValuedExpressableType resultType) {
		this.lhsOperand = lhsOperand;
		this.rhsOperand = rhsOperand;
		this.resultType = resultType;
	}

	public SqmExpression getLeftHandOperand() {
		return lhsOperand;
	}

	public SqmExpression getRightHandOperand() {
		return rhsOperand;
	}

	@Override
	public BasicValuedExpressableType getExpressableType() {
		return getInferableType();
	}


	@SuppressWarnings("unchecked")
	private BasicValuedExpressableType getInferableType() {
		// check LHS
		final ExpressableType lshExpressableType = lhsOperand.getExpressableType();
		if ( lshExpressableType != null ) {
			return (BasicValuedExpressableType) lshExpressableType;
		}

		// check RHS
		final ExpressableType rhsExpressableType = rhsOperand.getExpressableType();
		if ( rhsExpressableType != null ) {
			return (BasicValuedExpressableType) rhsExpressableType;
		}

		return resultType;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitConcatExpression( this );
	}

	@Override
	public String asLoggableText() {
		return "<concat>";
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getExpressableType().getJavaTypeDescriptor();
	}
}
