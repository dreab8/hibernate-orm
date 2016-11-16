/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.mapper.spi.basic;

import java.util.Locale;

import org.hibernate.type.descriptor.spi.java.LocaleTypeDescriptor;
import org.hibernate.type.descriptor.spi.sql.VarcharTypeDescriptor;
import org.hibernate.type.mapper.spi.JdbcLiteralFormatter;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and @link Locale}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class LocaleType extends BasicTypeImpl<Locale> {

	public static final LocaleType INSTANCE = new LocaleType();

	public LocaleType() {
		super( LocaleTypeDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "locale";
	}

	@Override
	public JdbcLiteralFormatter<Locale> getJdbcLiteralFormatter() {
		return LocaleTypeDescriptor.INSTANCE.getJdbcLiteralFormatter();
	}
}
