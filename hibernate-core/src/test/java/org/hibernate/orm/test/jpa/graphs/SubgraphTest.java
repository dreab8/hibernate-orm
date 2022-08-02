package org.hibernate.orm.test.jpa.graphs;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.SpecHints;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@Jpa(
		annotatedClasses = {
				SubgraphTest.Block.class,
				SubgraphTest.BlockUnit.class,
				SubgraphTest.Resident.class,
				SubgraphTest.User.class,
				SubgraphTest.ResidentBlockUnitMapping.class,
		},
		settingProviders = @SettingProvider(
				settingName = AvailableSettings.STATEMENT_INSPECTOR,
				provider = SubgraphTest.StatementInspectorProvider.class
		)
)
@TestForIssue(jiraKey = "HHH-15427")
public class SubgraphTest {

	public static class StatementInspectorProvider implements SettingProvider.Provider<Class> {

		@Override
		public Class getSetting() {
			return SQLStatementInspector.class;
		}
	}

	@Test
	public void testQueryFetchGraph(EntityManagerFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();

		scope.inTransaction(
				entityManager -> {
					TypedQuery<Resident> query = entityManager.createQuery(
							"select r from Resident r",
							Resident.class
					);
					query.setHint( SpecHints.HINT_SPEC_FETCH_GRAPH, entityManager.getEntityGraph( "resident.all" ) );
					query.getResultList();

					checkExecutedSatetement( statementInspector );
				}
		);
	}

	@Test
	public void testQueryLoadGraph(EntityManagerFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();

		scope.inTransaction(
				entityManager -> {
					TypedQuery<Resident> query = entityManager.createQuery(
							"select r from Resident r",
							Resident.class
					);
					query.setHint( SpecHints.HINT_SPEC_LOAD_GRAPH, entityManager.getEntityGraph( "resident.all" ) );
					query.getResultList();

					checkExecutedSatetement( statementInspector );
				}
		);
	}

	@Test
	public void testFindFetchGraph(EntityManagerFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();

		scope.inTransaction(
				entityManager -> {
					entityManager.find(
							Resident.class,
							"id",
							Collections.singletonMap(
									SpecHints.HINT_SPEC_FETCH_GRAPH,
									entityManager.getEntityGraph( "resident.all" )
							)
					);

					checkExecutedSatetement( statementInspector );
				}
		);
	}

	@Test
	public void testFindLoadGraph(EntityManagerFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();

		scope.inTransaction(
				entityManager -> {
					entityManager.find(
							Resident.class,
							"id",
							Collections.singletonMap(
									SpecHints.HINT_SPEC_LOAD_GRAPH,
									entityManager.getEntityGraph( "resident.all" )
							)
					);

					checkExecutedSatetement( statementInspector );
				}
		);
	}

	private static void checkExecutedSatetement(SQLStatementInspector statementInspector) {
		statementInspector.assertExecutedCount( 1 );
		assertThat( statementInspector.getNumberOfJoins( 0 ) ).isEqualTo( 4 );

		statementInspector.assertNumberOfOccurrenceInQuery( 0, "RESIDENT_TABLE", 1 );
		statementInspector.assertNumberOfOccurrenceInQuery( 0, "USER_TABLE", 1 );
		statementInspector.assertNumberOfOccurrenceInQuery( 0, "RESIDENT_BLOCK_UNIT_TABLE", 1 );
		statementInspector.assertNumberOfOccurrenceInQuery( 0, "BLOCK_UNIT_TABLE", 1 );
		statementInspector.assertNumberOfOccurrenceInQuery( 0, "BLOCK_TABLE", 1 );
	}

	@Entity(name = "EntityBase")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static class EntityBase implements Serializable {

		@Id
		@GeneratedValue(generator = "system-uuid")
		private String id;

		@Column(name = "created_by")
		private String createdBy;

	}

	@NamedEntityGraph(name = "resident.all", attributeNodes = {
			@NamedAttributeNode(value = "user"),
			@NamedAttributeNode(value = "residentBlockUnitMappings", subgraph = "residentBlockUnitMappings")
	}, subgraphs = {
			@NamedSubgraph(name = "residentBlockUnitMappings", attributeNodes = @NamedAttributeNode(value = "blockUnit", subgraph = "blockUnit")),
			@NamedSubgraph(name = "blockUnit", attributeNodes = @NamedAttributeNode("block"))
	})
	@Entity(name = "Resident")
	@Table(name = "RESIDENT_TABLE")
	public static class Resident extends EntityBase {

		@ManyToOne(fetch = FetchType.LAZY)
		private User user;

		@OneToMany(mappedBy = "resident")
		private Set<ResidentBlockUnitMapping> residentBlockUnitMappings;

	}

	@Entity(name = "User")
	@Table(name = "USER_TABLE")
	public static class User extends EntityBase {
		private String name;
	}

	@Entity(name = "ResidentBlockUnitMapping")
	@Table(name = "RESIDENT_BLOCK_UNIT_TABLE")
	public static class ResidentBlockUnitMapping extends EntityBase {

		@ManyToOne(fetch = FetchType.LAZY)
		private Resident resident;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "Block_unit_id")
		private BlockUnit blockUnit;

	}

	@Entity(name = "BlockUnit")
	@Table(name = "BLOCK_UNIT_TABLE")
	public static class BlockUnit extends EntityBase {

		@ManyToOne(fetch = FetchType.LAZY)
		private Block block;

	}

	@Entity(name = "Block")
	@Table(name = "BLOCK_TABLE")
	public static class Block extends EntityBase {
		private String number;
	}
}
