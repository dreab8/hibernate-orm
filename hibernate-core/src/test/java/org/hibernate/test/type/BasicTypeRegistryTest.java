/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.CompositeCustomType;
import org.hibernate.type.CustomType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.internal.StringJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.VarcharSqlDescriptor;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.BasicTypeRegistry;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * @author Steve Ebersole
 */
public class BasicTypeRegistryTest extends BaseUnitTestCase {
	@Test
	public void testOverriding() {
		BasicTypeRegistry registry = new BasicTypeRegistry( new TypeConfiguration() );

		BasicType type = registry.getBasicType( "uuid-binary" );
		assertSame( StandardSpiBasicTypes.UUID_BINARY, type );
		type = registry.getBasicType( UUID.class.getName() );
		assertSame( StandardSpiBasicTypes.UUID_BINARY, type );

		BasicType override = StandardSpiBasicTypes.UUID_CHAR;

		registry.register( override );
		type = registry.getBasicType( UUID.class.getName() );
		assertNotSame( StandardSpiBasicTypes.UUID_BINARY, type );
		assertSame( override, type );
	}

	@Test
	public void testExpanding() {
		BasicTypeRegistry registry = new BasicTypeRegistry( new TypeConfiguration() );

		BasicType type = registry.getBasicType( SomeNoopType.INSTANCE.getName() );
		assertNull( type );

		registry.register( SomeNoopType.INSTANCE );
		type = registry.getBasicType( SomeNoopType.INSTANCE.getName() );
		assertNotNull( type );
		assertSame( SomeNoopType.INSTANCE, type );
	}

	@Test
	public void testRegisteringUserTypes() {
		BasicTypeRegistry registry = new BasicTypeRegistry( new TypeConfiguration() );

		registry.register( new TotallyIrrelevantUserType(), new String[] { "key" } );
		BasicType type = registry.getBasicType( "key" );
		assertNotNull( type );
		assertEquals( CustomType.class, type.getClass() );
		assertEquals( TotallyIrrelevantUserType.class, ( (CustomType) type ).getUserType().getClass() );

		registry.register( new TotallyIrrelevantCompositeUserType(), new String[] { "key" } );
		type = registry.getBasicType( "key" );
		assertNotNull( type );
		assertEquals( CompositeCustomType.class, type.getClass() );
		assertEquals( TotallyIrrelevantCompositeUserType.class, ( (CompositeCustomType) type ).getUserType().getClass() );

		type = registry.getBasicType( UUID.class.getName() );
		assertSame( StandardSpiBasicTypes.UUID_BINARY, type );
		registry.register( new TotallyIrrelevantUserType(), new String[] { UUID.class.getName() } );
		type = registry.getBasicType( UUID.class.getName() );
		assertNotSame( StandardSpiBasicTypes.UUID_BINARY, type );
		assertEquals( CustomType.class, type.getClass() );
	}

	public static class SomeNoopType extends BasicTypeImpl<String> {
		public static final SomeNoopType INSTANCE = new SomeNoopType();

		public SomeNoopType() {
			super( VarcharSqlDescriptor.INSTANCE, StringJavaDescriptor.INSTANCE );
		}

		public String getName() {
			return "noop";
		}

		@Override
		protected boolean registerUnderJavaType() {
			return false;
		}
	}

	public static class TotallyIrrelevantUserType implements UserType {
		@Override
		public int[] sqlTypes() {
			return new int[0];
		}

		@Override
		public Class returnedClass() {
			return null;
		}

		@Override
		public boolean equals(Object x, Object y) throws HibernateException {
			return false;
		}

		@Override
		public int hashCode(Object x) throws HibernateException {
			return 0;
		}

		@Override
		public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
			return null;
		}

		@Override
		public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
		}

		@Override
		public Object deepCopy(Object value) throws HibernateException {
			return null;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public Serializable disassemble(Object value) throws HibernateException {
			return null;
		}

		@Override
		public Object assemble(Serializable cached, Object owner) throws HibernateException {
			return null;
		}

		@Override
		public Object replace(Object original, Object target, Object owner) throws HibernateException {
			return null;
		}
	}

	public static class TotallyIrrelevantCompositeUserType implements CompositeUserType {
		@Override
		public String[] getPropertyNames() {
			return new String[0];
		}

		@Override
		public Type[] getPropertyTypes() {
			return new Type[0];
		}

		@Override
		public Object getPropertyValue(Object component, int property) throws HibernateException {
			return null;
		}

		@Override
		public void setPropertyValue(Object component, int property, Object value) throws HibernateException {
		}

		@Override
		public Class returnedClass() {
			return null;
		}

		@Override
		public boolean equals(Object x, Object y) throws HibernateException {
			return false;
		}

		@Override
		public int hashCode(Object x) throws HibernateException {
			return 0;
		}

		@Override
		public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
				throws HibernateException, SQLException {
			return null;
		}

		@Override
		public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
				throws HibernateException, SQLException {
		}

		@Override
		public Object deepCopy(Object value) throws HibernateException {
			return null;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public Serializable disassemble(Object value, SharedSessionContractImplementor session) throws HibernateException {
			return null;
		}

		@Override
		public Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner)
				throws HibernateException {
			return null;
		}

		@Override
		public Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner)
				throws HibernateException {
			return null;
		}
	}
}
