/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.procedure;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.procedure.ProcedureParameter;
import org.hibernate.result.Output;
import org.hibernate.result.ResultSetOutput;
import org.hibernate.type.NumericBooleanType;
import org.hibernate.type.YesNoType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.test.procedure.Person;
import org.hibernate.test.procedure.Phone;
import org.hibernate.test.procedure.Vote;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.NamedStoredProcedureQueries;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.StoredProcedureQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Vlad Mihalcea
 */
@Jpa(
		annotatedClasses = {
				Person.class,
				Phone.class,
				OracleStoredProcedureTest.IdHolder.class,
				Vote.class
		}
)
@RequiresDialect(value = OracleDialect.class, version = 800)
public class OracleStoredProcedureTest {

	@NamedStoredProcedureQueries({
			@NamedStoredProcedureQuery(
					name = "singleRefCursor",
					procedureName = "singleRefCursor",
					parameters = {
							@StoredProcedureParameter(mode = ParameterMode.REF_CURSOR, type = void.class)
					}
			),
			@NamedStoredProcedureQuery(
					name = "outAndRefCursor",
					procedureName = "outAndRefCursor",
					parameters = {
							@StoredProcedureParameter(mode = ParameterMode.REF_CURSOR, type = void.class),
							@StoredProcedureParameter(mode = ParameterMode.OUT, type = Long.class),
					}
			)
	})
	@Entity(name = "IdHolder")
	public static class IdHolder {

		@Id
		Long id;
	}

	@BeforeEach
	public void init(EntityManagerFactoryScope scope) {
		EntityManager entityManager = scope.getEntityManagerFactory().createEntityManager();
		entityManager.getTransaction().begin();

		try {
			Session session = entityManager.unwrap( Session.class );

			session.doWork( connection -> {
				Statement statement = null;
				try {
					statement = connection.createStatement();
					statement.executeUpdate(
							"CREATE OR REPLACE PROCEDURE sp_count_phones (  " +
									"   personId IN NUMBER,  " +
									"   phoneCount OUT NUMBER )  " +
									"AS  " +
									"BEGIN  " +
									"    SELECT COUNT(*) INTO phoneCount  " +
									"    FROM phone  " +
									"    WHERE person_id = personId; " +
									"END;"
					);
					statement.executeUpdate(
							"CREATE OR REPLACE PROCEDURE sp_person_phones ( " +
									"   personId IN NUMBER, " +
									"   personPhones OUT SYS_REFCURSOR ) " +
									"AS  " +
									"BEGIN " +
									"    OPEN personPhones FOR " +
									"    SELECT *" +
									"    FROM phone " +
									"    WHERE person_id = personId; " +
									"END;"
					);
					statement.executeUpdate(
							"CREATE OR REPLACE FUNCTION fn_count_phones ( " +
									"    personId IN NUMBER ) " +
									"    RETURN NUMBER " +
									"IS " +
									"    phoneCount NUMBER; " +
									"BEGIN " +
									"    SELECT COUNT(*) INTO phoneCount " +
									"    FROM phone " +
									"    WHERE person_id = personId; " +
									"    RETURN( phoneCount ); " +
									"END;"
					);
					statement.executeUpdate(
							"CREATE OR REPLACE FUNCTION fn_person_and_phones ( " +
									"    personId IN NUMBER ) " +
									"    RETURN SYS_REFCURSOR " +
									"IS " +
									"    personAndPhones SYS_REFCURSOR; " +
									"BEGIN " +
									"   OPEN personAndPhones FOR " +
									"        SELECT " +
									"            pr.id AS \"pr.id\", " +
									"            pr.name AS \"pr.name\", " +
									"            pr.nickName AS \"pr.nickName\", " +
									"            pr.address AS \"pr.address\", " +
									"            pr.createdOn AS \"pr.createdOn\", " +
									"            pr.version AS \"pr.version\", " +
									"            ph.id AS \"ph.id\", " +
									"            ph.person_id AS \"ph.person_id\", " +
									"            ph.phone_number AS \"ph.phone_number\", " +
									"            ph.valid AS \"ph.valid\" " +
									"       FROM person pr " +
									"       JOIN phone ph ON pr.id = ph.person_id " +
									"       WHERE pr.id = personId; " +
									"   RETURN personAndPhones; " +
									"END;"
					);
					statement.executeUpdate(
							"CREATE OR REPLACE " +
									"PROCEDURE singleRefCursor(p_recordset OUT SYS_REFCURSOR) AS " +
									"  BEGIN " +
									"    OPEN p_recordset FOR " +
									"    SELECT 1 as id " +
									"    FROM dual; " +
									"  END; "
					);
					statement.executeUpdate(
							"CREATE OR REPLACE " +
									"PROCEDURE outAndRefCursor(p_recordset OUT SYS_REFCURSOR, p_value OUT NUMBER) AS " +
									"  BEGIN " +
									"    OPEN p_recordset FOR " +
									"    SELECT 1 as id " +
									"    FROM dual; " +
									"	 SELECT 1 INTO p_value FROM dual; " +
									"  END; "
					);
					statement.executeUpdate(
							"CREATE OR REPLACE PROCEDURE sp_phone_validity ( " +
									"   validity IN NUMBER, " +
									"   personPhones OUT SYS_REFCURSOR ) " +
									"AS  " +
									"BEGIN " +
									"    OPEN personPhones FOR " +
									"    SELECT phone_number " +
									"    FROM phone " +
									"    WHERE valid = validity; " +
									"END;"
					);
					statement.executeUpdate(
							"CREATE OR REPLACE PROCEDURE sp_votes ( " +
									"   validity IN CHAR, " +
									"   votes OUT SYS_REFCURSOR ) " +
									"AS  " +
									"BEGIN " +
									"    OPEN votes FOR " +
									"    SELECT id " +
									"    FROM vote " +
									"    WHERE vote_choice = validity; " +
									"END;"
					);
				}
				finally {
					if ( statement != null ) {
						statement.close();
					}
				}
			} );
		}
		finally {
			entityManager.getTransaction().rollback();
			entityManager.close();
		}

		entityManager = scope.getEntityManagerFactory().createEntityManager();
		entityManager.getTransaction().begin();

		try {
			Person person1 = new Person( "John Doe" );
			person1.setNickName( "JD" );
			person1.setAddress( "Earth" );
			person1.setCreatedOn( Timestamp.from( LocalDateTime.of( 2000, 1, 1, 0, 0, 0 )
														  .toInstant( ZoneOffset.UTC ) ) );

			entityManager.persist( person1 );

			Phone phone1 = new Phone( "123-456-7890" );
			phone1.setId( 1L );
			phone1.setValid( true );

			person1.addPhone( phone1 );

			Phone phone2 = new Phone( "098_765-4321" );
			phone2.setId( 2L );
			phone2.setValid( false );

			person1.addPhone( phone2 );

			entityManager.getTransaction().commit();
		}
		finally {
			entityManager.close();
		}
	}

	@AfterEach
	public void destroy(EntityManagerFactoryScope scope) {
		EntityManager entityManager = scope.getEntityManagerFactory().createEntityManager();
		entityManager.getTransaction().begin();

		try {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( connection -> {
				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( "DROP PROCEDURE sp_count_phones" );
				}
				catch (SQLException ignore) {
				}
			} );
		}
		finally {
			entityManager.getTransaction().rollback();
			entityManager.close();
		}

		entityManager = scope.getEntityManagerFactory().createEntityManager();
		entityManager.getTransaction().begin();

		try {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( connection -> {
				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( "DROP PROCEDURE sp_person_phones" );
				}
				catch (SQLException ignore) {
				}
			} );
		}
		finally {
			entityManager.getTransaction().rollback();
			entityManager.close();
		}

		entityManager = scope.getEntityManagerFactory().createEntityManager();
		entityManager.getTransaction().begin();

		try {
			Session session = entityManager.unwrap( Session.class );
			session.doWork( connection -> {
				try (Statement statement = connection.createStatement()) {
					statement.executeUpdate( "DROP FUNCTION fn_count_phones" );
				}
				catch (SQLException ignore) {
				}
			} );
		}
		finally {
			entityManager.getTransaction().rollback();
			entityManager.close();
		}
		scope.releaseEntityManagerFactory();
	}

	@Test
	public void testStoredProcedureOutParameter(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_count_phones" );
					query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN );
					query.registerStoredProcedureParameter( 2, Long.class, ParameterMode.OUT );

					query.setParameter( 1, 1L );

					query.execute();
					Long phoneCount = (Long) query.getOutputParameterValue( 2 );
					assertEquals( Long.valueOf( 2 ), phoneCount );
				}
		);
	}

	@Test
	public void testStoredProcedureRefCursor(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_person_phones" );
					query.registerStoredProcedureParameter( 1, Long.class, ParameterMode.IN );
					query.registerStoredProcedureParameter( 2, Class.class, ParameterMode.REF_CURSOR );
					query.setParameter( 1, 1L );

					query.execute();
					List<Object[]> postComments = query.getResultList();
					assertNotNull( postComments );
				}
		);
	}

	@Test
	public void testHibernateProcedureCallRefCursor(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Session session = entityManager.unwrap( Session.class );

					ProcedureCall call = session.createStoredProcedureCall( "sp_person_phones" );
					final ProcedureParameter<Long> inParam = call.registerParameter(
							1,
							Long.class,
							ParameterMode.IN
					);
					call.setParameter( inParam, 1L );
					call.registerParameter( 2, Class.class, ParameterMode.REF_CURSOR );

					Output output = call.getOutputs().getCurrent();
					List<Object[]> postComments = ( (ResultSetOutput) output ).getResultList();
					assertEquals( 2, postComments.size() );
				}
		);
	}

	@Test
	public void testStoredProcedureReturnValue(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					BigDecimal phoneCount = (BigDecimal) entityManager
							.createNativeQuery( "SELECT fn_count_phones(:personId) FROM DUAL" )
							.setParameter( "personId", 1 )
							.getSingleResult();
					assertEquals( BigDecimal.valueOf( 2 ), phoneCount );
				}
		);
	}

	@Test
	public void testNamedNativeQueryStoredProcedureRefCursor(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					List<Object[]> postAndComments = entityManager
							.createNamedQuery(
									"fn_person_and_phones" )
							.setParameter( 1, 1L )
							.getResultList();
					Object[] postAndComment = postAndComments.get( 0 );
					Person person = (Person) postAndComment[0];
					Phone phone = (Phone) postAndComment[1];
					assertEquals( 2, postAndComments.size() );
				}
		);
	}

	@Test
	public void testNamedNativeQueryStoredProcedureRefCursorWithJDBC(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Session session = entityManager.unwrap( Session.class );
					session.doWork( connection -> {
						try (CallableStatement function = connection.prepareCall(
								"{ ? = call fn_person_and_phones( ? ) }" )) {
							try {
								function.registerOutParameter( 1, Types.REF_CURSOR );
							}
							catch (SQLException e) {
								//OracleTypes.CURSOR
								function.registerOutParameter( 1, -10 );
							}
							function.setInt( 2, 1 );
							function.execute();
							try (ResultSet resultSet = (ResultSet) function.getObject( 1 );) {
								while ( resultSet.next() ) {
									Long postCommentId = resultSet.getLong( 1 );
									String review = resultSet.getString( 2 );
								}
							}
						}
					} );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11863")
	public void testSysRefCursorAsOutParameter(EntityManagerFactoryScope scope) {

		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery function = entityManager.createNamedStoredProcedureQuery( "singleRefCursor" );

					function.execute();

					assertFalse( function.hasMoreResults() );
					Long value = null;

					try (ResultSet resultSet = (ResultSet) function.getOutputParameterValue( 1 )) {
						while ( resultSet.next() ) {
							value = resultSet.getLong( 1 );
						}
					}
					catch (SQLException e) {
						fail( e.getMessage() );
					}

					assertEquals( Long.valueOf( 1 ), value );
				} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11863")
	public void testOutAndSysRefCursorAsOutParameter(EntityManagerFactoryScope scope) {

		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery function = entityManager.createNamedStoredProcedureQuery( "outAndRefCursor" );

					function.execute();

					assertFalse( function.hasMoreResults() );
					Long value = null;

					try (ResultSet resultSet = (ResultSet) function.getOutputParameterValue( 1 )) {
						while ( resultSet.next() ) {
							value = resultSet.getLong( 1 );
						}
					}
					catch (SQLException e) {
						fail( e.getMessage() );
					}

					assertEquals( value, function.getOutputParameterValue( 2 ) );
				} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12661")
	public void testBindParameterAsHibernateType(EntityManagerFactoryScope scope) {

		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_phone_validity" )
							.registerStoredProcedureParameter( 1, NumericBooleanType.class, ParameterMode.IN )
							.registerStoredProcedureParameter( 2, Class.class, ParameterMode.REF_CURSOR )
							.setParameter( 1, true );

					query.execute();
					List phones = query.getResultList();
					assertEquals( 1, phones.size() );
					assertEquals( "123-456-7890", phones.get( 0 ) );
				} );

		scope.inTransaction(
				entityManager -> {
					Vote vote1 = new Vote();
					vote1.setId( 1L );
					vote1.setVoteChoice( true );

					entityManager.persist( vote1 );

					Vote vote2 = new Vote();
					vote2.setId( 2L );
					vote2.setVoteChoice( false );

					entityManager.persist( vote2 );
				} );

		scope.inTransaction(
				entityManager -> {
					StoredProcedureQuery query = entityManager.createStoredProcedureQuery( "sp_votes" )
							.registerStoredProcedureParameter( 1, YesNoType.class, ParameterMode.IN )
							.registerStoredProcedureParameter( 2, Class.class, ParameterMode.REF_CURSOR )
							.setParameter( 1, true );

					query.execute();
					List votes = query.getResultList();
					assertEquals( 1, votes.size() );
					assertEquals( 1, ( (Number) votes.get( 0 ) ).intValue() );
				} );
	}
}
