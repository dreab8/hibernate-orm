/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.mapper.spi.basic;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.spi.java.CharacterTypeDescriptor;
import org.hibernate.type.descriptor.spi.java.StringTypeDescriptor;
import org.hibernate.type.descriptor.spi.sql.CharTypeDescriptor;
import org.hibernate.type.spi.JdbcLiteralFormatter;

/**
 * A type that maps between {@link java.sql.Types#CHAR CHAR(1)} and {@link Character}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class CharacterType
		extends BasicTypeImpl<Character>
		implements JdbcLiteralFormatter<Character> {

	public static final CharacterType INSTANCE = new CharacterType();

	protected CharacterType() {
		super( CharacterTypeDescriptor.INSTANCE, CharTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "character";
	}

	@Override
	public JdbcLiteralFormatter<Character> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toJdbcLiteral(Character value, Dialect dialect) {
		return StringTypeDescriptor.INSTANCE.toJdbcLiteral( toString( value ), dialect );
	}
}
