/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.hql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.hql.model.Human;
import org.hibernate.orm.test.hql.model.Name;
import org.hibernate.query.Query;

import org.hibernate.testing.TestForIssue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hibernate.orm.test.support.domains.retail.ModelClasses.applyRetailModel;

/**
 * Isolated test for various usages of parameters
 *
 * @author Steve Ebersole
 */
public class ParameterTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected boolean exportSchema() {
		return true;
	}

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		metadataSources.addResource( "org/hibernate/orm/test/hql/model/Animal.hbm.xml" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9154")
	public void testClassAsParameter() {
		sessionFactoryScope().inTransaction(
				session -> {
					session.createQuery( "from Human h where h.name = :class" )
							.setParameter( "class", new Name() )
							.list();
					session.createQuery( "from Human where name = :class" )
							.setParameter( "class", new Name() )
							.list();
					session.createQuery( "from Human h where :class = h.name" )
							.setParameter( "class", new Name() )
							.list();
					session.createQuery( "from Human h where :class <> h.name" )
							.setParameter( "class", new Name() )
							.list();
				} );
	}

	@Test
	@Disabled
	@TestForIssue(jiraKey = "HHH-7705")
	public void testSetPropertiesMapWithNullValues() {
		sessionFactoryScope().inTransaction(
				session -> {
					Human human = new Human();
					human.setNickName( "nick" );
					session.save( human );

					Map parameters = new HashMap();
					parameters.put( "nickName", null );

					Query q = session.createQuery(
							"from Human h where h.nickName = :nickName or (h.nickName is null and :nickName is null)" );
					q.setProperties( ( parameters ) );
					assertThat( q.list().size(), is( 0 ) );

					Human human1 = new Human();
					human1.setNickName( null );
					session.save( human1 );

					parameters = new HashMap();

					parameters.put( "nickName", null );
					q = session.createQuery(
							"from Human h where h.nickName = :nickName or (h.nickName is null and :nickName is null)" );
					q.setProperties( ( parameters ) );
					assertThat( q.list().size(), is( 1 ) );
					Human found = (Human) q.list().get( 0 );
					assertThat( found.getId(), is( human1.getId() ) );

					parameters = new HashMap();
					parameters.put( "nickName", "nick" );

					q = session.createQuery(
							"from Human h where h.nickName = :nickName or (h.nickName is null and :nickName is null)" );
					q.setProperties( ( parameters ) );
					assertThat( q.list().size(), is( 1 ) );
					found = (Human) q.list().get( 0 );
					assertThat( found.getId(), is( human.getId() ) );

					session.delete( human );
					session.delete( human1 );
				} );
	}

	@Test
	@Disabled
	@TestForIssue(jiraKey = "HHH-10796")
	public void testSetPropertiesMapNotContainingAllTheParameters() {
		sessionFactoryScope().inTransaction(
				session -> {
					Human human = new Human();
					human.setNickName( "nick" );
					human.setIntValue( 1 );
					session.save( human );

					Map parameters = new HashMap();
					parameters.put( "nickNames", "nick" );

					List<Integer> intValues = new ArrayList<>();
					intValues.add( 1 );
					Query q = session.createQuery(
							"from Human h where h.nickName in (:nickNames) and h.intValue in (:intValues)" );
					q.setParameterList( "intValues", intValues );
					q.setProperties( ( parameters ) );
					assertThat( q.list().size(), is( 1 ) );

					session.delete( human );
				} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9154")
	public void testObjectAsParameter() {
		sessionFactoryScope().inTransaction(
				session -> {
					session.createQuery( "from Human h where h.name = :OBJECT" )
							.setParameter( "OBJECT", new Name() )
							.list();
					session.createQuery( "from Human where name = :OBJECT" )
							.setParameter( "OBJECT", new Name() )
							.list();
					session.createQuery( "from Human h where :OBJECT = h.name" )
							.setParameter( "OBJECT", new Name() )
							.list();
					session.createQuery( "from Human h where :OBJECT <> h.name" )
							.setParameter( "OBJECT", new Name() )
							.list();
				} );
	}
}
