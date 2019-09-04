/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.function.Consumer;

import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class ArgumentDomainResult<A> implements DomainResult<A> {
	private final DomainResult realDomainResult;

	public ArgumentDomainResult(DomainResult realDomainResult) {
		this.realDomainResult = realDomainResult;
	}

	@Override
	public String getResultVariable() {
		return realDomainResult.getResultVariable();
	}

	@Override
	public JavaTypeDescriptor getResultJavaTypeDescriptor() {
		return realDomainResult.getResultJavaTypeDescriptor();
	}

	@Override
	public ArgumentReader<A> createResultAssembler(
			Consumer<Initializer> initializerCollector,
			AssemblerCreationState creationState) {
		return null;
	}
}
