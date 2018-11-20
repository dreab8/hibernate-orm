/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemacreation.idgenerator;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class SequenceGenerationTest extends AbstractGenerationTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { TestEntity.class };
	}

	@SchemaTest
	public void testSequenceIsGenerated(SchemaScope scope) throws Exception {
		List<String> commands = getSqlScriptOutputFileLines();

		assertThat(
				isCommandGenerated( commands, "create table test_entity \\(id .*, primary key \\(id\\)\\)" ),
				is( true )
		);

		assertThat(
				isCommandGenerated( commands, "create sequence sequence_generator start with 5 increment by 3" ),
				is( true )
		);
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	public static class TestEntity {
		Long id;

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQUENCEGENERATOR")
		@SequenceGenerator(name = "SEQUENCEGENERATOR", allocationSize = 3, initialValue = 5, sequenceName = "SEQUENCE_GENERATOR")
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}
