/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.proxy.ProxyConfiguration;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * This template demonstrates how to develop a test case for Hibernate ORM,
 * using the Java Persistence API.
 */
public class JPAUnitTestCase extends BaseEntityManagerFunctionalTestCase {
	private static final String PROXY_NAMING_SUFFIX = Environment.useLegacyProxyClassnames() ? "HibernateBasicProxy$" : "HibernateBasicProxy";


	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Adult.class, Person.class };
	}

	// Entities are auto-discovered, so just add them anywhere on class-path
	// Add your tests, using standard JUnit.
	@Test
	public void hhh12786Test() throws Exception {
		final EntityManager entityManager = entityManagerFactory().createEntityManager();
		{
			entityManager.getTransaction().begin();
			Adult adult = new Adult();
			adult.setName("Arjun Kumar");
			entityManager.persist(adult);
			entityManager.getTransaction().commit();
		}
		{
			entityManager.getTransaction().begin();
			final List<Adult> adultsCalledArjun = entityManager
					.createQuery("SELECT a from Adult a WHERE a.name = :name", Adult.class)
					.setParameter("name", "Arjun Kumar").getResultList();
			final Adult adult = adultsCalledArjun.iterator().next();
			entityManager.remove(adult);
			entityManager.getTransaction().commit();
		}
		entityManager.close();
		Class clazz = Person.class;
		Class loaded = new ByteBuddy().
		with( new NamingStrategy.SuffixingRandom( PROXY_NAMING_SUFFIX, new NamingStrategy.SuffixingRandom.BaseNameResolver.ForFixedValue( clazz.getName() ) ) )
				.subclass( clazz )
				.implement( new Class[0] )
		.defineField( ProxyConfiguration.INTERCEPTOR_FIELD_NAME, ProxyConfiguration.Interceptor.class, Visibility.PRIVATE )
//				.method( ElementMatchers.isVirtual().and( ElementMatchers.not( ElementMatchers.isFinalizer() ) ) )
//				.intercept( MethodDelegation.toField( ProxyConfiguration.INTERCEPTOR_FIELD_NAME ) )


				.implement( ProxyConfiguration.class )
				.intercept( FieldAccessor.ofField( ProxyConfiguration.INTERCEPTOR_FIELD_NAME ).withAssigner( Assigner.DEFAULT, Assigner.Typing.DYNAMIC ) )
				.make()
				.load( clazz.getClassLoader(), ByteBuddyState.resolveClassLoadingStrategy( clazz ) )
				.getLoaded();

		loaded.newInstance();

	}
}
