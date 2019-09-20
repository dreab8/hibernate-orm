/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import javax.money.MonetaryAmount;

import org.hibernate.boot.MetadataSources;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.expression.SqmParameter;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.domain.helpdesk.HelpDeskDomainModel;
import org.hibernate.testing.orm.domain.helpdesk.Status;
import org.hibernate.testing.orm.domain.retail.RetailDomainModel;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for "type inference specifically limited to the cases JPA
 * says should be supported.
 *
 * @see ExtensionSqmInferenceTests
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class JpaStandardSqmInferenceTests extends SessionFactoryBasedFunctionalTest {
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );

		HelpDeskDomainModel.INSTANCE.applyDomainModel( metadataSources );
		RetailDomainModel.INSTANCE.applyDomainModel( metadataSources );
	}

	@Test
	public void testEnumInference() {
		checkParameters(
				"from Account a where a.loginStatus = :status",
				Status.class
		);

		checkParameters(
				"from Account a where a.loginStatus <> :status",
				Status.class
		);

		checkParameters(
				"from Account a where a.loginStatus != :status",
				Status.class
		);

		checkParameters(
				"from Account a where a.loginStatus > :status",
				Status.class
		);

		checkParameters(
				"from Account a where a.loginStatus >= :status",
				Status.class
		);

		checkParameters(
				"from Account a where a.loginStatus < :status",
				Status.class
		);

		checkParameters(
				"from Account a where a.loginStatus <= :status",
				Status.class
		);
	}

	@Test
	public void testConvertedInference() {
		checkParameters(
				"select l from LineItem l where l.subTotal = :limit",
				MonetaryAmount.class
		);

		checkParameters(
				"select l from LineItem l where l.subTotal <> :limit",
				MonetaryAmount.class
		);

		checkParameters(
				"select l from LineItem l where l.subTotal != :limit",
				MonetaryAmount.class
		);

		checkParameters(
				"select l from LineItem l where l.subTotal > :limit",
				MonetaryAmount.class
		);

		checkParameters(
				"select l from LineItem l where l.subTotal >= :limit",
				MonetaryAmount.class
		);

		checkParameters(
				"select l from LineItem l where l.subTotal < :limit",
				MonetaryAmount.class
		);

		checkParameters(
				"select l from LineItem l where l.subTotal <= :limit",
				MonetaryAmount.class
		);
	}

	private void checkParameters(String query, Class<?>... expecteds) {
		final SqmStatement sqmStatement = sessionFactory().getQueryEngine().getSemanticQueryProducer().interpret( query );

		checkParameterTypes( sqmStatement, expecteds );
	}

	private void checkParameterTypes(SqmStatement<?> sqmStatement, Class<?>[] expectedParameterTypes) {
		assertEquals( expectedParameterTypes.length, sqmStatement.getSqmParameters().size() );

		int count = 0;
		for ( SqmParameter<?> queryParameter : sqmStatement.getSqmParameters() ) {
			assertEquals(
					"Anticipated type for query parameter [" + queryParameter + "]",
					expectedParameterTypes[count++],
					queryParameter.getAnticipatedType().getExpressableJavaTypeDescriptor().getJavaType()
			);

//			assertThat(
//					queryParameter.getAnticipatedType().getJavaType(),
//					AssignableMatcher.assignableTo( expectedParameterTypes[count++] )
//			);
		}
	}
}
