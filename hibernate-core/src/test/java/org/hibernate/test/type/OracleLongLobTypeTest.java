/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import java.sql.Blob;
import java.sql.Clob;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.dialect.Oracle12cDialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.type.Type;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * A test asserting LONG/LONGRAW versus CLOB/BLOB resolution for various Oracle Dialects
 *
 * @author Steve Ebersole
 */
public class OracleLongLobTypeTest extends BaseUnitTestCase {
	@Test
	public void testOracle8() {
		check(
				Oracle8iDialect.class,
				Primitives.class,
				StandardSpiBasicTypes.BINARY,
				StandardSpiBasicTypes.CHARACTER_ARRAY
		);
		check(
				Oracle8iDialect.class,
				LobPrimitives.class,
				StandardSpiBasicTypes.MATERIALIZED_BLOB,
				StandardSpiBasicTypes.MATERIALIZED_CLOB_CHAR_ARRAY
		);
		check( Oracle8iDialect.class, LobLocators.class, StandardSpiBasicTypes.BLOB, StandardSpiBasicTypes.CLOB );
	}

	@Test
	public void testOracle9() {
		check(
				Oracle9iDialect.class,
				Primitives.class,
				StandardSpiBasicTypes.BINARY,
				StandardSpiBasicTypes.CHAR_ARRAY
		);
		check(
				Oracle9iDialect.class,
				LobPrimitives.class,
				StandardSpiBasicTypes.MATERIALIZED_BLOB,
				StandardSpiBasicTypes.MATERIALIZED_CLOB_CHAR_ARRAY
		);
		check( Oracle9iDialect.class, LobLocators.class, StandardSpiBasicTypes.BLOB, StandardSpiBasicTypes.CLOB );
	}

	@Test
	public void testOracle10() {
		check(
				Oracle10gDialect.class,
				Primitives.class,
				StandardSpiBasicTypes.BINARY,
				StandardSpiBasicTypes.CHAR_ARRAY
		);
		check(
				Oracle10gDialect.class,
				LobPrimitives.class,
				StandardSpiBasicTypes.MATERIALIZED_BLOB,
				StandardSpiBasicTypes.MATERIALIZED_CLOB_CHAR_ARRAY
		);
		check( Oracle10gDialect.class, LobLocators.class, StandardSpiBasicTypes.BLOB, StandardSpiBasicTypes.CLOB );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10345" )
	public void testOracle12() {
		check(
				Oracle12cDialect.class,
				Primitives.class,
				StandardSpiBasicTypes.MATERIALIZED_BLOB,
				StandardSpiBasicTypes.CHAR_ARRAY
		);
		check(
				Oracle12cDialect.class,
				LobPrimitives.class,
				StandardSpiBasicTypes.MATERIALIZED_BLOB,
				StandardSpiBasicTypes.MATERIALIZED_CLOB_CHAR_ARRAY
		);
		check( Oracle12cDialect.class, LobLocators.class, StandardSpiBasicTypes.BLOB, StandardSpiBasicTypes.CLOB );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10345" )
	public void testOracle12PreferLongRaw() {
		check(
				Oracle12cDialect.class,
				Primitives.class,
				StandardSpiBasicTypes.BINARY,
				StandardSpiBasicTypes.CHAR_ARRAY,
				true
		);
		check(
				Oracle12cDialect.class,
				LobPrimitives.class,
				StandardSpiBasicTypes.MATERIALIZED_BLOB,
				StandardSpiBasicTypes.MATERIALIZED_CLOB_CHAR_ARRAY,
				true
		);
		check(
				Oracle12cDialect.class,
				LobLocators.class,
				StandardSpiBasicTypes.BLOB,
				StandardSpiBasicTypes.CLOB,
				true
		);
	}

	private void check(
			Class<? extends Dialect> dialectClass,
			Class entityClass,
			BasicType binaryTypeClass,
			BasicType charTypeClass) {
		check( dialectClass, entityClass, binaryTypeClass, charTypeClass, false );
	}

	private void check(
			Class<? extends Dialect> dialectClass,
			Class entityClass,
			BasicType binaryTypeClass,
			BasicType charTypeClass,
			boolean preferLongRaw) {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, dialectClass.getName() )
				.applySetting( Oracle12cDialect.PREFER_LONG_RAW, Boolean.toString( preferLongRaw ) )
				.applySetting( "hibernate.temp.use_jdbc_metadata_defaults", false )
				.build();

		try {
			final MetadataImplementor mappings = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( entityClass )
					.buildMetadata();
			mappings.validate();

			final PersistentClass entityBinding = mappings.getEntityBinding( entityClass.getName() );

			assertAreEquals( entityBinding.getProperty( "binaryData" ).getType(), binaryTypeClass );
			assertAreEquals( entityBinding.getProperty( "characterData" ).getType(), charTypeClass );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	private void assertAreEquals(Type actualType, BasicType expectedType){
		assertEquals(
				"incorrect return JavaTypeDescriptor",
				expectedType.getJavaTypeDescriptor(),
				( (BasicType) actualType ).getJavaTypeDescriptor()
		);
		assertEquals( "incorrect return SqlTypeDescriptor",expectedType.getSqlTypeDescriptor(), ( (BasicType) actualType ).getSqlTypeDescriptor() );
	}

	@Entity
	public static class Primitives {
		@Id
		public Integer id;
		public byte[] binaryData;
		public char[] characterData;
	}

	@Entity
	public static class LobPrimitives {
		@Id
		public Integer id;
		@Lob
		public byte[] binaryData;
		@Lob
		public char[] characterData;
	}

	@Entity
	public static class LobLocators {
		@Id
		public Integer id;
		@Lob
		public Blob binaryData;
		@Lob
		public Clob characterData;
	}
}
