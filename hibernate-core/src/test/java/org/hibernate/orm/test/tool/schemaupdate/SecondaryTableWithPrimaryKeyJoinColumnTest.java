package org.hibernate.orm.test.tool.schemaupdate;

import java.io.IOException;
import java.util.EnumSet;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.Table;

import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
public class SecondaryTableWithPrimaryKeyJoinColumnTest extends BaseSchemaUnitTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}


	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@SchemaTest
	public void testPrimaryKeyJoinColum(SchemaScope schemaScope) throws IOException {
		schemaScope.withSchemaExport( schemaExport -> schemaExport.setHaltOnError( true )
				.setFormat( false ).create( EnumSet.of(
						TargetType.DATABASE,
						TargetType.SCRIPT
				) ) );
		boolean found = false;
		for ( String sqlStatement : getSqlScriptOutputFileLines() ) {
			if ( sqlStatement.toLowerCase()
					.contains( "alter table person2 add constraint" ) ) {
					found = true;
			}
			if ( found ) {
				assertThat( "The created foreign key references the wrong column", sqlStatement.toLowerCase().contains( "person_id" ), is(true) );
			}
		}
		assertThat(
				"The foreign key on the secondary table has not been created",
				found,
				is( true )
		);
	}

	@Entity(name = "Person")
	@Table(name = "person")
	@SecondaryTables({
			@SecondaryTable(name = "person2", pkJoinColumns = {
					@PrimaryKeyJoinColumn(name = "id", referencedColumnName = "person_id")
			})
	})
	public static class Person {
		@Id
		@Column(name = "person_id")
		public Integer id;
		@Column(table = "person2")
		public String name;
	}
}

