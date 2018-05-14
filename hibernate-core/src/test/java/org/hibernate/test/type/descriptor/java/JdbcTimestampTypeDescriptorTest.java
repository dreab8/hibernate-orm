/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.descriptor.java;

import java.util.Date;

import org.hibernate.type.descriptor.java.internal.JdbcTimestampJavaDescriptor;

/**
 * @author Owen Farrell
 */
public class JdbcTimestampTypeDescriptorTest extends AbstractDescriptorTest<Date> {
	final Date original = new Date();
	final Date copy = new Date( original.getTime() );
	final Date different = new Date( original.getTime() + 500L);

	public JdbcTimestampTypeDescriptorTest() {
		super( JdbcTimestampJavaDescriptor.INSTANCE );
	}

	@Override
	protected Data<Date> getTestData() {
		return new Data<Date>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return true;
	}
}
