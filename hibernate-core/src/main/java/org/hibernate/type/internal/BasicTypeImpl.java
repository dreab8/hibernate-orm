/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.VersionSupport;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.SqlSelectionReader;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.ValueBinder;
import org.hibernate.type.descriptor.spi.ValueExtractor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class BasicTypeImpl<T> extends AbstractStandardBasicType<T> implements SqlSelectionReader<T> {

	private VersionSupport<T> versionSupport;

	@SuppressWarnings("unchecked")
	public BasicTypeImpl(BasicJavaDescriptor javaDescriptor, SqlTypeDescriptor sqlTypeDescriptor) {
		super(sqlTypeDescriptor,javaDescriptor);

		this.versionSupport = javaDescriptor.getVersionSupport();
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

	@Override
	public BasicType<T> getBasicType() {
		return this;
	}

	@Override
	public Optional<VersionSupport<T>> getVersionSupport() {
		return Optional.ofNullable( versionSupport );
	}

	@Override
	public SqlSelectionReader<T> getSqlSelectionReader() {
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T read(
			ResultSet resultSet,
			SharedSessionContractImplementor persistenContext,
			SqlSelection sqlSelection) throws SQLException {
		return getValueExtractor().extract(
				resultSet,
				sqlSelection.getJdbcResultSetIndex(),
				persistenContext
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public T extractParameterValue(
			CallableStatement statement,
			SharedSessionContractImplementor persistenContext,
			int jdbcParameterIndex) throws SQLException {
		return getValueExtractor().extract(
				statement,
				jdbcParameterIndex,
				persistenContext
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public T extractParameterValue(
			CallableStatement statement,
			SharedSessionContractImplementor persistenContext,
			String jdbcParameterName) throws SQLException {
		return getValueExtractor().extract(
				statement,
				jdbcParameterName,
				persistenContext
		);
	}

	@Override
	public ValueBinder getValueBinder() {
		return getSqlTypeDescriptor().getBinder( getJavaTypeDescriptor() );
	}

	@Override
	public ValueExtractor<T> getValueExtractor() {
		return getSqlTypeDescriptor().getExtractor( getJavaTypeDescriptor() );
	}

	@Override
	public void nullSafeSet(
			PreparedStatement st, Object value, int index, boolean[] settable, SharedSessionContractImplementor session)
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
