/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;
import org.hibernate.type.descriptor.java.internal.CharacterArrayJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.VarcharSqlDescriptor;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and {@link Character Character[]}
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class CharacterArrayType extends AbstractSingleColumnStandardBasicType<Character[]> {
	public static final CharacterArrayType INSTANCE = new CharacterArrayType();

	public CharacterArrayType() {
		super( VarcharSqlDescriptor.INSTANCE, CharacterArrayJavaDescriptor.INSTANCE );
	}

	public String getName() {
		return "wrapper-characters";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), Character[].class.getName(), "Character[]" };
	}
}
