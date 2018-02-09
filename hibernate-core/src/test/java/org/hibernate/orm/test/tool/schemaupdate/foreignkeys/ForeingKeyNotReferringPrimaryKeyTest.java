/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate.foreignkeys;

import java.io.Serializable;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
public class ForeingKeyNotReferringPrimaryKeyTest extends BaseSchemaUnitTestCase {

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Building.class, BuildingCompany.class };
	}

	@Override
	protected boolean dropSchemaAfterTest() {
		return false;
	}

	@SchemaTest
	public void testForeignKeyHasCorrectName(SchemaScope schemaScope) throws Exception {
		schemaScope.withSchemaExport( schemaExport -> schemaExport
				.setHaltOnError( true )
				.setFormat( false )
				.create( EnumSet.of( TargetType.SCRIPT, TargetType.STDOUT ) ) );

		/*
		Expected is :
		alter table Building add constraint FKhmpix7ro745sk7buocs8knv6p foreign key (company_id) references BuildingCompany(name)
		 */
		checkAlterTableStatement( expectedAlterTableStatement(
				"Building",
				"FKhmpix7ro745sk7buocs8knv6p",
				"company_id",
				"BuildingCompany",
				"name"
		) );
	}

	private void checkAlterTableStatement(String expectedAlterTableStatement)
			throws Exception {
		final List<String> sqlLines = getSqlScriptOutputFileLines();
		boolean found = false;
		for ( String line : sqlLines ) {
			if ( line.toLowerCase().contains( expectedAlterTableStatement.toLowerCase() ) ) {
				found = true;
				return;
			}
		}
		assertThat( "Expected alter table statement not found : " + expectedAlterTableStatement, found, is( true ) );
	}

	private String expectedAlterTableStatement(
			String tableName,
			String fkConstraintName,
			String fkColumnName,
			String referenceTableName,
			String referenceColumnName) {
		return "alter table " + tableName + " add constraint " + fkConstraintName + " foreign key (" + fkColumnName + ") references " + referenceTableName + " (" + referenceColumnName + ")";
	}

	@Entity(name = "Building")
	public static class Building {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		@JoinColumn(name = "company_id", referencedColumnName = "name")
		private BuildingCompany company;

		public BuildingCompany getCompany() {
			return company;
		}

		public void setCompany(BuildingCompany company) {
			this.company = company;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@MappedSuperclass
	public static class Company implements Serializable {
		@Column
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "BuildingCompany")
	public static class BuildingCompany extends Company {
		@Id
		@GeneratedValue
		private Long id;
		private Date foundedIn;

		public Date getFoundedIn() {
			return foundedIn;
		}

		public void setFoundedIn(Date foundedIn) {
			this.foundedIn = foundedIn;
		}

		public Long getId() {
			return id;
		}
	}
}
