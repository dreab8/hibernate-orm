/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;
import org.hibernate.type.descriptor.java.internal.PrimitiveCharacterArrayJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.ClobSqlDescriptor;
import org.hibernate.type.internal.BasicTypeImpl;

/**
 * Map a char[] to a Clob
 *
 * @author Emmanuel Bernard
 */
public class PrimitiveCharacterArrayClobTypeImpl extends BasicTypeImpl<char[]> {
	public static final CharacterArrayClobTypeImpl INSTANCE = new CharacterArrayClobTypeImpl();

	public PrimitiveCharacterArrayClobTypeImpl() {
		super( ClobSqlDescriptor.DEFAULT, PrimitiveCharacterArrayJavaDescriptor.INSTANCE );
	}

	public String getName() {
		// todo name these annotation types for addition to the registry
		return null;
	}
}
