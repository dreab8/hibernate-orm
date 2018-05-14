/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicType;

/**
 * Convenience base class for {@link BasicType} implementations
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public abstract class AbstractStandardBasicType<T>
		implements BasicType<T>, ProcedureParameterExtractionAware<T>, ProcedureParameterNamedBinder {

	private static final Size DEFAULT_SIZE = new Size( 19, 2, 255, Size.LobMultiplier.NONE ); // to match legacy behavior
	private final Size dictatedSize = new Size();

	// Don't use final here.  Need to initialize after-the-fact
	// by DynamicParameterizedTypes.

	// sqlTypes need always to be in sync with sqlTypeDescriptor
	private int[] sqlTypes;

	public AbstractStandardBasicType() {
		this.sqlTypes = new int[] { getSqlTypeDescriptor().getJdbcTypeCode() };
	}


	protected T getReplacement(T original, T target, SharedSessionContractImplementor session) {
		if ( original == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
			return target;
		}
		else if ( !isMutable() ||
					( target != LazyPropertyInitializer.UNFETCHED_PROPERTY && isEqual( original, target ) ) ) {
			return original;
		}
		else {
			return deepCopy( original );
		}
	}

	@Override
	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		return value == null ? ArrayHelper.FALSE : ArrayHelper.TRUE;
	}

	protected boolean registerUnderJavaType() {
		return false;
	}

	protected static Size getDefaultSize() {
		return DEFAULT_SIZE;
	}

	protected Size getDictatedSize() {
		return dictatedSize;
	}
	
	// final implementations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

//	public final void setSqlTypeDescriptor( SqlTypeDescriptor sqlTypeDescriptor ) {
//		this.sqlTypeDescriptor = sqlTypeDescriptor;
//		this.sqlTypes = new int[] { sqlTypeDescriptor.getSqlType() };
//	}

	@Override
	public final Class getReturnedClass() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	public final int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public final int[] sqlTypes(Mapping mapping) throws MappingException {
		return sqlTypes;
	}

	@Override
	public Size[] dictatedSizes(Mapping mapping) throws MappingException {
		return new Size[] { getDictatedSize() };
	}

	@Override
	public Size[] defaultSizes(Mapping mapping) throws MappingException {
		return new Size[] { getDefaultSize() };
	}

	@Override
	public final boolean isAssociationType() {
		return false;
	}

	@Override
	public final boolean isCollectionType() {
		return false;
	}

	@Override
	public final boolean isComponentType() {
		return false;
	}

	@Override
	public final boolean isEntityType() {
		return false;
	}

	@Override
	public final boolean isAnyType() {
		return false;
	}

	public final boolean isXMLElement() {
		return false;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public final boolean isSame(Object x, Object y) {
		return isEqual( x, y );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public final boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) {
		return isEqual( x, y );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public final boolean isEqual(Object one, Object another) {
		return getJavaTypeDescriptor().areEqual( (T) one, (T) another );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public final int getHashCode(Object x) {
		return getJavaTypeDescriptor().extractHashCode( (T) x );
	}

	@Override
	public final int getHashCode(Object x, SessionFactoryImplementor factory) {
		return getHashCode( x );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public final int compare(Object x, Object y) {
		return getJavaTypeDescriptor().getComparator().compare( (T) x, (T) y );
	}

	@Override
	public final boolean isDirty(Object old, Object current, SharedSessionContractImplementor session) {
		return isDirty( old, current );
	}

	@Override
	public final boolean isDirty(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session) {
		return checkable[0] && isDirty( old, current );
	}

	protected final boolean isDirty(Object old, Object current) {
		return !isSame( old, current );
	}

	@Override
	public final boolean isModified(
			Object oldHydratedState,
			Object currentState,
			boolean[] checkable,
			SharedSessionContractImplementor session) {
		return isDirty( oldHydratedState, currentState );
	}

	@Override
	public final Object nullSafeGet(
			ResultSet rs,
			String[] names,
			SharedSessionContractImplementor session,
			Object owner) throws SQLException {
		return nullSafeGet( rs, names[0], session );
	}

	@Override
	public final Object nullSafeGet(ResultSet rs, String name, SharedSessionContractImplementor session, Object owner)
			throws SQLException {
		return nullSafeGet( rs, name, session );
	}

	public final T nullSafeGet(ResultSet rs, String name, final SharedSessionContractImplementor session) throws SQLException {
		return nullSafeGet( rs, name, (WrapperOptions) session );
	}

	protected final T nullSafeGet(ResultSet rs, String name, WrapperOptions options) throws SQLException {
		return remapSqlTypeDescriptor( options ).getExtractor( getJavaTypeDescriptor() ).extract( rs, name, options );
	}

	public Object get(ResultSet rs, String name, SharedSessionContractImplementor session) throws HibernateException, SQLException {
		return nullSafeGet( rs, name, session );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public final void nullSafeSet(
			PreparedStatement st,
			Object value,
			int index,
			final SharedSessionContractImplementor session) throws SQLException {
		nullSafeSet( st, value, index, (WrapperOptions) session );
	}

	@SuppressWarnings({ "unchecked" })
	protected final void nullSafeSet(PreparedStatement st, Object value, int index, WrapperOptions options) throws SQLException {
		remapSqlTypeDescriptor( options ).getBinder( getJavaTypeDescriptor() ).bind( st, ( T ) value, index, options );
	}

	protected SqlTypeDescriptor remapSqlTypeDescriptor(WrapperOptions options) {
		return options.remapSqlTypeDescriptor( getSqlTypeDescriptor() );
	}

	public void set(PreparedStatement st, T value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
		nullSafeSet( st, value, index, session );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public final String toLoggableString(Object value, SessionFactoryImplementor factory) {
		if ( value == LazyPropertyInitializer.UNFETCHED_PROPERTY || !Hibernate.isInitialized( value ) ) {
			return  "<uninitialized>";
		}
		return getJavaTypeDescriptor().extractLoggableRepresentation( (T) value );
	}

	@Override
	public final boolean isMutable() {
		return getJavaTypeDescriptor().getMutabilityPlan().isMutable();
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public final Object deepCopy(Object value, SessionFactoryImplementor factory) {
		return deepCopy( (T) value );
	}

	protected final T deepCopy(T value) {
		return getJavaTypeDescriptor().getMutabilityPlan().deepCopy( value );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public final Serializable disassemble(Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return getJavaTypeDescriptor().getMutabilityPlan().disassemble( (T) value );
	}

	@Override
	public final Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return getJavaTypeDescriptor().getMutabilityPlan().assemble( cached );
	}

	@Override
	public final void beforeAssemble(Serializable cached, SharedSessionContractImplementor session) {
	}

	@Override
	public final Object hydrate(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException {
		return nullSafeGet(rs, names, session, owner);
	}


	@Override
	@SuppressWarnings({ "unchecked" })
	public final Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner, Map copyCache) {
		return getReplacement( (T) original, (T) target, session );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public Object replace(
			Object original,
			Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map copyCache,
			ForeignKeyDirection foreignKeyDirection) {
		return ForeignKeyDirection.FROM_PARENT == foreignKeyDirection
				? getReplacement( (T) original, (T) target, session )
				: target;
	}

	@Override
	public boolean canDoExtraction() {
		return true;
	}

	@Override
	public T extract(CallableStatement statement, int startIndex, final SharedSessionContractImplementor session) throws SQLException {
		return remapSqlTypeDescriptor( session ).getExtractor( getJavaTypeDescriptor() ).extract(
				statement,
				startIndex,
				session
		);
	}

	@Override
	public T extract(CallableStatement statement, String[] paramNames, final SharedSessionContractImplementor session) throws SQLException {
		return remapSqlTypeDescriptor( session ).getExtractor( getJavaTypeDescriptor() ).extract(
				statement,
				paramNames,
				session
		);
	}

	@Override
	public void nullSafeSet(CallableStatement st, Object value, String name, SharedSessionContractImplementor session) throws SQLException {
		nullSafeSet( st, value, name, (WrapperOptions) session );
	}

	@SuppressWarnings("unchecked")
	protected final void nullSafeSet(CallableStatement st, Object value, String name, WrapperOptions options) throws SQLException {
		remapSqlTypeDescriptor( options ).getBinder( getJavaTypeDescriptor() ).bind( st, (T) value, name, options );
	}

	@Override
	public boolean canDoSetting() {
		return true;
	}
}
