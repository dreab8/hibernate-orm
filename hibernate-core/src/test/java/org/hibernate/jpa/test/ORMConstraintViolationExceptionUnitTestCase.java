/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test;

import java.sql.SQLException;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceException;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.exception.ConstraintViolationException;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using its built-in unit test framework.
 * Although ORMStandaloneTestCase is perfectly acceptable as a reproducer, usage of this class is much preferred.
 * Since we nearly always include a regression test with bug fixes, providing your reproducer using this method
 * simplifies the process.
 *
 * What's even better?  Fork hibernate-orm itself, add your test case directly to a module's unit tests, then
 * submit it as a PR!
 */
public class ORMConstraintViolationExceptionUnitTestCase extends BaseCoreFunctionalTestCase {

	// Add your entities here.
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				A.class,
				AInfo.class
		};
	}

	// If you use *.hbm.xml mappings, instead of annotations, add the mappings here.
	@Override
	protected String[] getMappings() {
		return new String[] {
//				"Foo.hbm.xml",
//				"Bar.hbm.xml"
		};
	}
	// If those mappings reside somewhere other than resources/org/hibernate/test, change this.
	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/test/";
	}

	// Add in any settings that are specific to your test.  See resources/hibernate.properties for the defaults.
	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );

		configuration.setProperty( AvailableSettings.SHOW_SQL, Boolean.TRUE.toString() );
		configuration.setProperty( AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString() );
	}

	@Test
	public void testConstraintViolationOnSave() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		AInfo aInfo = new AInfo();
		aInfo.uniqueString = "unique";
		s.persist( aInfo );
		s.flush();
		s.clear();
		try {
			AInfo anotherAInfo = new AInfo();
			anotherAInfo.uniqueString = "unique";
			s.save( anotherAInfo );
			fail( "should have thrown an exception" );
		}
		catch (PersistenceException expected) {
			// works in 5.1 and 5.3
			assertTrue( ConstraintViolationException.class.isInstance( expected ) );
			assertTrue( SQLException.class.isInstance( expected.getCause() ) );
		}
		finally {
			tx.rollback();
			s.close();
		}
	}

	@Test
	public void testConstraintViolationOnSaveOrUpdate() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		AInfo aInfo = new AInfo();
		aInfo.uniqueString = "unique";
		s.persist( aInfo );
		s.flush();
		s.clear();
		try {
			AInfo anotherAInfo = new AInfo();
			anotherAInfo.uniqueString = "unique";
			s.saveOrUpdate( anotherAInfo );
			fail( "should have thrown an exception" );
		}
		catch (PersistenceException expected) {
			// works in 5.1 and 5.3
			assertTrue( ConstraintViolationException.class.isInstance( expected ) );
			assertTrue( SQLException.class.isInstance( expected.getCause() ) );
		}
		finally {
			tx.rollback();
			s.close();
		}
	}

	@Test
	public void testConstraintViolationOnPersist() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		AInfo aInfo = new AInfo();
		aInfo.uniqueString = "unique";
		s.persist( aInfo );
		s.flush();
		s.clear();
		try {
			AInfo anotherAInfo = new AInfo();
			anotherAInfo.uniqueString = "unique";
			s.persist( anotherAInfo );
			fail( "should have thrown an exception" );
		}
		catch (PersistenceException expected) {
			// works in 5.1; does not work in 5.3
			assertTrue( ConstraintViolationException.class.isInstance( expected ) );
			assertTrue( SQLException.class.isInstance( expected.getCause() ) );
		}
//		catch (PersistenceException ex) {
//			// works in 5.3
//			// ConstraintViolationException is wrapped even though it is a PersistenceException
//			assertTrue( ConstraintViolationException.class.isInstance( ex.getCause() ) );
//			assertTrue( PersistenceException.class.isInstance( ex.getCause() ) );
//			assertTrue( SQLException.class.isInstance( ex.getCause().getCause() ) );
//		}
		finally {
			tx.rollback();
			s.close();
		}
	}

	@Test
	public void testConstraintViolationOnMerge() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		AInfo aInfo = new AInfo();
		aInfo.uniqueString = "unique";
		s.persist( aInfo );
		s.flush();
		s.clear();
		try {
			AInfo anotherAInfo = new AInfo();
			anotherAInfo.uniqueString = "unique";
			s.merge( anotherAInfo );
			fail( "should have thrown an exception" );
		}
		catch (PersistenceException expected) {
			// works in 5.1; does not work in 5.3
			assertTrue( ConstraintViolationException.class.isInstance( expected ) );
			assertTrue( SQLException.class.isInstance( expected.getCause() ) );
		}
//		catch (PersistenceException ex) {
//			// works in 5.3
//			// ConstraintViolationException is wrapped even though it is a PersistenceException
//			assertTrue( ConstraintViolationException.class.isInstance( ex.getCause() ) );
//			assertTrue( PersistenceException.class.isInstance( ex.getCause() ) );
//			assertTrue( SQLException.class.isInstance( ex.getCause().getCause() ) );
//		}
		finally {
			tx.rollback();
			s.close();
		}
	}

	@Test
	public void testConstraintViolationUpdateFlush() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		AInfo aInfo = new AInfo();
		aInfo.uniqueString = "unique";
		s.persist( aInfo );
		AInfo aInfo1 = new AInfo();
		s.persist( aInfo1 );
		s.flush();
		s.clear();
		try {
			aInfo1 = s.get( AInfo.class, aInfo1.id );
			aInfo1.uniqueString = "unique";
			s.flush();
		}
		catch (PersistenceException expected) {
			// works in 5.1; does not work in 5.3
			assertTrue( ConstraintViolationException.class.isInstance( expected ) );
			assertTrue( SQLException.class.isInstance( expected.getCause() ) );
		}
//		catch (PersistenceException ex) {
//			// works in 5.3
//			// ConstraintViolationException is wrapped even though it is a PersistenceException
//			assertTrue( ConstraintViolationException.class.isInstance( ex.getCause() ) );
//			assertTrue( PersistenceException.class.isInstance( ex.getCause() ) );
//			assertTrue( SQLException.class.isInstance( ex.getCause().getCause() ) );
//		}
		finally {
			tx.rollback();
			s.close();
		}
	}

	@Entity(name = "A")
	public static class A {
		@Id
		private long id;

		@ManyToOne(optional = false)
		private AInfo aInfo;
	}

	@Entity(name = "AInfo")
	public static class AInfo {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private long id;

		@Column(unique = true)
		private String uniqueString;
	}
}
