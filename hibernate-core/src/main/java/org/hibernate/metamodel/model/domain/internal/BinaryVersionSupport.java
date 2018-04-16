/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.VersionSupport;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * @author Andrea Boriero
 */
public class BinaryVersionSupport implements VersionSupport<byte[]> {

	public static final BinaryVersionSupport INSTANCE = new BinaryVersionSupport();

	@Override
	public byte[] seed(SharedSessionContractImplementor session) {
		return null;
	}

	@Override
	public byte[] next(byte[] current, SharedSessionContractImplementor session) {
		return current;
	}

	@Override
	public String toLoggableString(Object value) {
		return StandardSpiBasicTypes.BINARY.toLoggableString( value );
	}

	@Override
	public boolean isEqual(byte[] x, byte[] y) throws HibernateException {
		return StandardSpiBasicTypes.BINARY.areEqual( x,y );
	}
}
