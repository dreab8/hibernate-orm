/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import java.time.Instant;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.internal.BasicSqmPathSource;
import org.hibernate.orm.test.query.sqm.BaseSqmUnitTest;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import org.hibernate.testing.orm.junit.ExpectedException;
import org.hibernate.testing.orm.junit.ExpectedExceptionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
@ExtendWith( ExpectedExceptionExtension.class )
public class ParameterTests extends BaseSqmUnitTest {
	@Test
	@ExpectedException( SemanticException.class )
	public void testInvalidLegacyPositionalParam() {
		// todo (6.0) : should we define the rule with the integer as optional and then give a better exception?
		interpretSelect( "select a.nickName from Person a where a.numberOfToes = ?" );
	}

	@Test
	@ExpectedException( SemanticException.class )
	public void testZeroBasedPositionalParam() {
		interpretSelect( "select a.nickName from Person a where a.numberOfToes = ?0" );
	}

	@Test
	@ExpectedException( SemanticException.class )
	public void testNonContiguousPositionalParams() {
		interpretSelect( "select a.nickName from Person a where a.numberOfToes = ?1 or a.numberOfToes = ?3" );

	}

	@Test
	public void testParameterCollection() {
		final SqmSelectStatement<?> sqm = interpretSelect( "select a.nickName from Person a where a.numberOfToes = ?1" );
		assertThat( sqm.getSqmParameters(), hasSize( 1 ) );
	}

	@Test
	public void testAnticipatedTypeHandling() {
		final SqmSelectStatement<?> sqm = interpretSelect( "select a.nickName from Person a where a.numberOfToes = ?1" );
		final SqmParameter parameter = sqm.getSqmParameters().iterator().next();
		assertThat( parameter.getAnticipatedType(), is( instanceOf( BasicSqmPathSource.class ) ) );
		assertThat( parameter.allowMultiValuedBinding(), is( false ) );
	}

	@Test
	public void testAllowMultiValuedBinding() {
		final SqmSelectStatement<?> sqm = interpretSelect( "select a.nickName from Person a where a.numberOfToes in (?1)" );
		final SqmParameter parameter = sqm.getSqmParameters().iterator().next();

		assertThat( parameter.allowMultiValuedBinding(), is(true) );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Person.class
		};
	}

	@Entity( name = "Person" )
	public static class Person {
		@Embeddable
		public static class Name {
			public String first;
			public String last;
		}

		@Id
		public Integer pk;

		@Embedded
		public Person.Name name;

		public String nickName;

		@ManyToOne
		Person mate;

		@Temporal( TemporalType.TIMESTAMP )
		public Instant dob;

		public int numberOfToes;
	}

}
