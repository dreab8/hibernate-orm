/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import org.hibernate.Incubating;
import org.hibernate.sql.sqm.convert.spi.ParameterSpec;
import org.hibernate.type.mapper.spi.Type;

/**
 * Represents a parameter defined in the source (HQL/JPQL or criteria) query.
 *
 * NOTE: Consider this contract (and its sub-contracts) as incubating as we transition to 6.0 and SQM
 *
 * @author Steve Ebersole
 */
@Incubating
public interface QueryParameter<T> extends javax.persistence.Parameter<T> {
	boolean allowsMultiValuedBinding();

	void allowMultiValuedBinding(boolean allow);

	/**
	 * Get the Hibernate Type associated with this parameter.
	 *
	 * @return The Hibernate Type.
	 */
	Type getType();

	/**
	 * JPA has a different definition of positional parameters than what legacy Hibernate HQL had.  In JPA,
	 * the parameter holders are labelled (named :/).  At any rate the semantics are different and we often
	 * need to understand which we are dealing with (and applications might too).
	 *
	 * @return {@code true} if this is a JPA-style positional parameter; {@code false} would indicate
	 * we have either a named parameter ({@link #getName()} would return a non-{@code null} value) or a native
	 * Hibernate positional parameter.
	 *
	 * @deprecated (since 6.0) support for legacy positional parameters has been dropped, so
	 * by design any positional parameter is now a "jpa positional parameter".
	 */
	@Deprecated
	boolean isJpaPositionalParameter();

}
