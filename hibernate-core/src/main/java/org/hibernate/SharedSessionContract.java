/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;

import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.QueryProducer;

/**
 * Contract methods shared between {@link Session} and {@link StatelessSession}.
 * <p/>
 * NOTE : Poorly named.  "shared" simply indicates that its a unified contract between {@link Session} and
 * {@link StatelessSession}.
 * 
 * @author Steve Ebersole
 */
public interface SharedSessionContract extends QueryProducer, Serializable {
	/**
	 * Obtain the tenant identifier associated with this session.
	 *
	 * @return The tenant identifier associated with this session, or {@code null}
	 */
	String getTenantIdentifier();

	/**
	 * End the session by releasing the JDBC connection and cleaning up.
	 *
	 * @throws HibernateException Indicates problems cleaning up.
	 */
	void close() throws HibernateException;

	/**
	 * Check if the session is still open.
	 *
	 * @return boolean
	 */
	boolean isOpen();

	/**
	 * Check if the session is currently connected.
	 *
	 * @return boolean
	 */
	boolean isConnected();

	/**
	 * Begin a unit of work and return the associated {@link Transaction} object.  If a new underlying transaction is
	 * required, begin the transaction.  Otherwise continue the new work in the context of the existing underlying
	 * transaction.
	 *
	 * @return a Transaction instance
	 *
	 * @see #getTransaction
	 */
	Transaction beginTransaction();

	/**
	 * Get the {@link Transaction} instance associated with this session.  The concrete type of the returned
	 * {@link Transaction} object is determined by the {@code hibernate.transaction_factory} property.
	 *
	 * @return a Transaction instance
	 */
	Transaction getTransaction();

	@Override
	org.hibernate.query.Query createQuery(String queryString);

	@Override
	org.hibernate.query.Query getNamedQuery(String queryName);

	/**
	 * Gets a ProcedureCall based on a named template
	 *
	 * @param name The name given to the template
	 *
	 * @return The ProcedureCall
	 *
	 * @see javax.persistence.NamedStoredProcedureQuery
	 */
	ProcedureCall getNamedProcedureCall(String name);

	/**
	 * Creates a call to a stored procedure.
	 *
	 * @param procedureName The name of the procedure.
	 *
	 * @return The representation of the procedure call.
	 */
	ProcedureCall createStoredProcedureCall(String procedureName);

	/**
	 * Creates a call to a stored procedure with specific result set entity mappings.  Each class named
	 * is considered a "root return".
	 *
	 * @param procedureName The name of the procedure.
	 * @param resultClasses The entity(s) to map the result on to.
	 *
	 * @return The representation of the procedure call.
	 */
	ProcedureCall createStoredProcedureCall(String procedureName, Class... resultClasses);

	/**
	 * Creates a call to a stored procedure with specific result set entity mappings.
	 *
	 * @param procedureName The name of the procedure.
	 * @param resultSetMappings The explicit result set mapping(s) to use for mapping the results
	 *
	 * @return The representation of the procedure call.
	 */
	ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings);

	/**
	 * Get the Session-level JDBC batch size for the current Session.
	 * Overrides the SessionFactory JDBC batch size defined by the {@code hibernate.default_batch_fetch_size} configuration property for the scope of the current {@code Session}.
	 *
	 * @return Session-level JDBC batch size
	 *
	 * @since 5.2
	 *
	 * @see org.hibernate.boot.spi.SessionFactoryOptions#getJdbcBatchSize
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyJdbcBatchSize
	 */
	Integer getJdbcBatchSize();

	/**
	 * Set the Session-level JDBC batch size.
	 * Overrides the SessionFactory JDBC batch size defined by the {@code hibernate.default_batch_fetch_size} configuration property for the scope of the current {@code Session}.
	 *
	 * @param jdbcBatchSize Session-level JDBC batch size
	 *
	 * @since 5.2
	 *
	 * @see org.hibernate.boot.spi.SessionFactoryOptions#getJdbcBatchSize
	 * @see org.hibernate.boot.SessionFactoryBuilder#applyJdbcBatchSize
	 */
	void setJdbcBatchSize(Integer jdbcBatchSize);

	/**
	 * Return an instance of {@link CriteriaBuilder}
	 *
	 * @return an instance of CriteriaBuilder
	 * @throws IllegalStateException if the StatelessSession has been closed
	 */
	CriteriaBuilder getCriteriaBuilder();

	@Override
	<T> org.hibernate.query.Query<T> createQuery(String queryString, Class<T> resultType);

	<T> org.hibernate.query.Query<T> createQuery(CriteriaQuery<T> criteriaQuery);

	org.hibernate.query.Query createQuery(CriteriaUpdate updateQuery);

	org.hibernate.query.Query createQuery(CriteriaDelete deleteQuery);

	<T> org.hibernate.query.Query<T> createNamedQuery(String name, Class<T> resultType);
}
