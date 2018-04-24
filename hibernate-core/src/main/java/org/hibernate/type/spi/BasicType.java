/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.io.Serializable;
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
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.internal.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Marker interface for basic types.
 *
 * @author Steve Ebersole
 */
public interface BasicType<T> extends Type<T> {

	Size DEFAULT_SIZE = new Size( 19, 2, 255, Size.LobMultiplier.NONE ); // to match legacy behavior

	BasicJavaDescriptor<T> getJavaTypeDescriptor();

	/**
	 * Get the Java type handled by this Hibernate mapping Type.  May return {@code null}
	 * in the case of non-basic types in dynamic domain models.
	 */
	default Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	/**
	 * The descriptor of the SQL type part of this basic-type
	 */
	SqlTypeDescriptor getSqlTypeDescriptor();

	default boolean areEqual(T x, T y) throws HibernateException {
		return EqualsHelper.areEqual( x, y );
	}

	@Override
	default boolean isAssociationType() {
		return false;
	}

	@Override
	default boolean isCollectionType() {
		return false;
	}

	@Override
	default boolean isComponentType() {
		return false;
	}

	@Override
	default boolean isEntityType() {
		return false;
	}

	@Override
	default boolean isAnyType() {
		return false;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	default boolean isSame(Object x, Object y) {
		return isEqual( x, y );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	default boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) {
		return isEqual( x, y );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	default boolean isEqual(Object one, Object another) {
		return getJavaTypeDescriptor().areEqual( (T) one, (T) another );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	default Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner, Map copyCache) {
		return null;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	default Object replace(
			Object original,
			Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map copyCache,
			ForeignKeyDirection foreignKeyDirection) {
		return null;
	}

	@Override
	default int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	default boolean[] toColumnNullness(Object value, Mapping mapping) {
		return value == null ? ArrayHelper.FALSE : ArrayHelper.TRUE;
	}

	@Override
	default Object resolve(Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return value;
	}

	@Override
	default Object semiResolve(Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return value;
	}

	@Override
	default boolean isDirty(Object old, Object current, SharedSessionContractImplementor session) {
		return !isSame( old, current );
	}

	@Override
	default boolean isDirty(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session) {
		return checkable[0] && !isSame( old, current );
	}

	@Override
	default Type getSemiResolvedType(SessionFactoryImplementor factory) {
		return this;
	}

	@Override
	default Size[] defaultSizes(Mapping mapping) throws MappingException {
		return new Size[] { DEFAULT_SIZE };
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	default Object deepCopy(Object value, SessionFactoryImplementor factory) {
		return getJavaTypeDescriptor().getMutabilityPlan().deepCopy( (T) value );
	}

	@Override
	default boolean isModified(
			Object oldHydratedState,
			Object currentState,
			boolean[] checkable,
			SharedSessionContractImplementor session) {
		return !isSame( oldHydratedState, currentState );
	}

	@Override
	default String toLoggableString(Object value) {
		if ( value == LazyPropertyInitializer.UNFETCHED_PROPERTY || !Hibernate.isInitialized( value ) ) {
			return  "<uninitialized>";
		}
		return getJavaTypeDescriptor().extractLoggableRepresentation( (T) value );

	}

	@Override
	default int[] sqlTypes(Mapping mapping) throws MappingException {
		return new int[] { getSqlTypeDescriptor().getJdbcTypeCode() };
	}

	@Override
	default Size[] dictatedSizes(Mapping mapping) throws MappingException{
		return new Size[] { new Size()};
	}

	@Override
	default Class getReturnedClass(){
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	default int getHashCode(Object x) throws HibernateException{
		return getJavaTypeDescriptor().extractHashCode( (T) x );
	}

	@Override
	default int getHashCode(Object x, SessionFactoryImplementor factory) throws HibernateException{
		return getHashCode( x );
	}

	@Override
	default int compare(T x, T y){
		return getJavaTypeDescriptor().getComparator().compare( (T) x, (T) y );
	}

	@Override
	default Object nullSafeGet(
			ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException{
		return session.remapSqlTypeDescriptor( getSqlTypeDescriptor() ).getExtractor( getJavaTypeDescriptor() ).extract( rs, names[0], session );
	}

	T nullSafeGet(ResultSet rs, String name, final SharedSessionContractImplementor session) throws SQLException;

	@Override
	default Object nullSafeGet(
			ResultSet rs, String name, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException{
		return session.remapSqlTypeDescriptor( getSqlTypeDescriptor() ).getExtractor( getJavaTypeDescriptor() ).extract( rs, name, session );
	}

	@Override
	default void nullSafeSet(
			PreparedStatement st, Object value, int index, boolean[] settable, SharedSessionContractImplementor session)
			throws HibernateException, SQLException {
		if ( settable[0] ) {
			nullSafeSet( st, value, index, session );
		}
	}

	@Override
	default void nullSafeSet(
			PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
			throws HibernateException, SQLException{
		session.remapSqlTypeDescriptor( getSqlTypeDescriptor() ).getBinder( getJavaTypeDescriptor() ).bind( st, ( T ) value, index, session );
	}

	@Override
	default String toLoggableString(Object value, SessionFactoryImplementor factory) throws HibernateException{
		if ( value == LazyPropertyInitializer.UNFETCHED_PROPERTY || !Hibernate.isInitialized( value ) ) {
			return  "<uninitialized>";
		}
		return getJavaTypeDescriptor().extractLoggableRepresentation( (T) value );
	}

	@Override
	default String getName(){
		return getJavaTypeDescriptor().getTypeName();
	}

	@Override
	default boolean isMutable(){
		return getJavaTypeDescriptor().getMutabilityPlan().isMutable();
	}

	@Override
	default Serializable disassemble(Object value, SharedSessionContractImplementor session, Object owner)
			throws HibernateException{
		return getJavaTypeDescriptor().getMutabilityPlan().disassemble( (T) value );
	}

	@Override
	default Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner)
			throws HibernateException{
		return getJavaTypeDescriptor().getMutabilityPlan().assemble( cached );
	}

	@Override
	default void beforeAssemble(
			Serializable cached, SharedSessionContractImplementor session){
	}

	@Override
	default Object hydrate(
			ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException, SQLException{
		return session.remapSqlTypeDescriptor( getSqlTypeDescriptor() ).getExtractor( getJavaTypeDescriptor() ).extract( rs, names[0], session );
	}


}
