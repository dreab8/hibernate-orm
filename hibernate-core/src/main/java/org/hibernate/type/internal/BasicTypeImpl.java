/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.internal;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.VersionSupport;
import org.hibernate.type.AbstractStandardBasicType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicType;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class BasicTypeImpl<T> extends AbstractStandardBasicType<T> {

	private VersionSupport<T> versionSupport;

	public BasicTypeImpl(SqlTypeDescriptor sqlTypeDescriptor, BasicJavaDescriptor javaTypeDescriptor) {
		super( sqlTypeDescriptor, javaTypeDescriptor );
		this.versionSupport = javaTypeDescriptor.getVersionSupport();
	}

	public BasicTypeImpl(BasicJavaDescriptor javaTypeDescriptor, SqlTypeDescriptor sqlTypeDescriptor) {
		super( sqlTypeDescriptor, javaTypeDescriptor );
		this.versionSupport = javaTypeDescriptor.getVersionSupport();
	}

	public final int sqlType() {
		return getSqlTypeDescriptor().getSqlType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public BasicJavaDescriptor<T> getJavaTypeDescriptor() {
		return getJavaTypeDescriptor();
	}

	@Override
	public final void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			boolean[] settable,
			SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		if ( settable[0] ) {
			nullSafeSet( st, value, index, session );
		}
	}

	@Override
	public String getName() {
		return getJavaTypeDescriptor().getTypeName();
	}
}
