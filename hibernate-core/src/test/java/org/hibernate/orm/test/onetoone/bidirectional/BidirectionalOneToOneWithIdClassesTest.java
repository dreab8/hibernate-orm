/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.onetoone.bidirectional;

import java.io.Serializable;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * @author Jan Schatteman
 */
@JiraKey(value = "HHH-16908")
public class BidirectionalOneToOneWithIdClassesTest {

	@Test
	public void test() {
		StandardServiceRegistry ssr = null;
		try {
			ssr = new StandardServiceRegistryBuilder( new BootstrapServiceRegistryBuilder().build() ).build();

			new MetadataSources( ssr )
					.addAnnotatedClass( SpecialPricePoint.class )
					.addAnnotatedClass( SpecialProduct.class )
					.addAnnotatedClass( SpecialOperator.class ).buildMetadata()
					.getSessionFactoryBuilder()
					.build();
		}
		finally {
			if ( ssr != null ) {
				( (ServiceRegistryImplementor) ssr ).destroy();
			}
		}

	}

	@Entity(name = "SpecialOperator")
	@Table(name = "SPECIAL_OPERATORS")
	public static class SpecialOperator {

		@Id
		private String operatorId;

//		private String name;

	}

	@Embeddable
	public static class SpecialOperatorPK implements Serializable {
		String operatorId;
	}

	@Entity(name = "SpecialPricePoint")
	@Table(name = "SPECIAL_OPERATOR_PRICES_POINTS")
	@IdClass(SpecialPricePointPK.class)
	public static class SpecialPricePoint {

		@ManyToOne
		@MapsId
		private SpecialOperator operator;

		@Id
		String wholesalePrice;

		@OneToOne
		private SpecialProduct product;
	}

	@Embeddable
	public static class SpecialPricePointPK implements Serializable {
		@Embedded
		SpecialOperatorPK operator;

		String wholesalePrice;
	}

	@Table(name = "SPECIAL_PRODUCTS")
	@Entity(name = "SpecialProduct")
	@IdClass(SpecialProductPK.class)
	public static class SpecialProduct {

		@Id
		private String productId;

		@OneToOne(optional = false, mappedBy = "product")
		@MapsId
		private SpecialPricePoint wholesalePrice;
	}

	@Embeddable
	public static class SpecialProductPK implements Serializable {

		String productId;

		@Embedded
		SpecialPricePointPK wholesalePrice;
	}
}
