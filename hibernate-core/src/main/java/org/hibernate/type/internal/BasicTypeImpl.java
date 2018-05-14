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
import org.hibernate.type.descriptor.spi.ValueBinder;
import org.hibernate.type.descriptor.spi.ValueExtractor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicType;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class BasicTypeImpl<T> extends AbstractStandardBasicType<T> {
	private final SqlTypeDescriptor sqlTypeDescriptor;
	private BasicJavaDescriptor<T> javaTypeDescriptor;
	private VersionSupport<T> versionSupport;

	public BasicTypeImpl(SqlTypeDescriptor sqlTypeDescriptor, BasicJavaDescriptor javaTypeDescriptor) {
		super(sqlTypeDescriptor.getJdbcTypeCode());
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.sqlTypeDescriptor = sqlTypeDescriptor;
		this.versionSupport = javaTypeDescriptor.getVersionSupport();
	}

	public BasicTypeImpl(BasicJavaDescriptor javaTypeDescriptor, SqlTypeDescriptor sqlTypeDescriptor) {
		super(sqlTypeDescriptor.getJdbcTypeCode());
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.sqlTypeDescriptor = sqlTypeDescriptor;
		this.versionSupport = javaTypeDescriptor.getVersionSupport();
	}

	public BasicTypeImpl setVersionSupport(VersionSupport<T> versionSupport){
		// todo (6.0) : not sure this is the best place to define this...
		// 		the purpose of this is to account for cases where the proper
		//		VersionSupport to use is not the same as the JTD's
		//		VersionSupport.  This only(?) happens when we have a
		//		`byte[]` mapped to T-SQL ROWVERSION/TIMESTAMP data-type -
		//		which is represented as a `byte[]`, but with a very
		//		specific comparison algorithm.
		//
		//		the alternative is to handle this distinction when building
		//		the VersionDescriptor - if the JTD is a `byte[]`, we'd use
		//		a specialized VersionSupport
		this.versionSupport = versionSupport;
		return this;
	}

	public BasicType<T> getBasicType() {
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public BasicJavaDescriptor<T> getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return sqlTypeDescriptor;
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

	public void setJavaTypeDescriptor(BasicJavaDescriptor javaTypeDescriptor){
		this.javaTypeDescriptor = javaTypeDescriptor;
	}

	public ValueBinder getValueBinder() {
		return getSqlTypeDescriptor().getBinder( getJavaTypeDescriptor() );
	}

	public ValueExtractor<T> getValueExtractor() {
		return getSqlTypeDescriptor().getExtractor( getJavaTypeDescriptor() );
	}
}
