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
					.addAnnotatedClass( PricePoint.class )
					.addAnnotatedClass( Product.class )
					.addAnnotatedClass( Operator.class ).buildMetadata()
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
	public static class Operator {

		@Id
		private String operatorId;

//		private String name;

	}

	@Embeddable
	public static class OperatorPK implements Serializable {
		String operatorId;
	}

	@Entity(name = "SpecialPricePoint")
	@Table(name = "OPERATOR_PRICES_POINTS")
	@IdClass(PricePointPK.class)
	public static class PricePoint {

		@ManyToOne
		@MapsId
		private Operator operator;

		@Id
		String wholesalePrice;

		@OneToOne
		private Product product;
	}

	@Embeddable
	public static class PricePointPK implements Serializable {
		@Embedded
		OperatorPK operator;

		String wholesalePrice;
	}

	@Table(name = "SPECIAL_PRODUCTS")
	@Entity(name = "SpecialProduct")
	@IdClass(ProductPK.class)
	public static class Product {

		@Id
		private String productId;

		@OneToOne(optional = false, mappedBy = "product")
		@MapsId
		private PricePoint wholesalePrice;
	}
	/*
	PRODUCT(productId , operatorId,  wholesalePrice) ID ProductPK

	Fk PricePoint.product ----> producId + wholesalePrice ( operatorId, wholesalePrice )

	FK Product.wholesalePrice -----> operatorId, wholesalePrice <-- toOne

	PRICEPOINT(operatorId,  wholesalePrice, productId)

	 */

	@Embeddable
	public static class ProductPK implements Serializable {

		String productId;

		@Embedded
		PricePointPK wholesalePrice;
	}
}
