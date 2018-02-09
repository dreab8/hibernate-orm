/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.hql.model;
import java.io.Serializable;
import java.util.HashMap;

/**
 * Mimic a JDK 5 enum.
 *
 * @author Steve Ebersole
 */
public class Classification implements Serializable, Comparable {

	public static final Classification COOL = new Classification( "COOL", 0 );
	public static final Classification LAME = new Classification( "LAME", 1 );

	private static final HashMap INSTANCES = new HashMap();
	static {
		INSTANCES.put( COOL.name, COOL );
		INSTANCES.put( LAME.name, LAME );
	}

	private final String name;
	private final int ordinal;
	private final int hashCode;

	private Classification(String name, int ordinal) {
		this.name = name;
		this.ordinal = ordinal;

		int hashCode = name.hashCode();
		hashCode = 29 * hashCode + ordinal;
		this.hashCode = hashCode;
	}

	public String name() {
		return name;
	}

	public int ordinal() {
		return ordinal;
	}

	public boolean equals(Object obj) {
		return compareTo( obj ) == 0;
	}

	public int compareTo(Object o) {
		int otherOrdinal = ( ( Classification ) o ).ordinal;
		if ( ordinal == otherOrdinal ) {
			return 0;
		}
		else if ( ordinal > otherOrdinal ) {
			return 1;
		}
		else {
			return -1;
		}
	}

	public int hashCode() {
		return hashCode;
	}

	public static Classification valueOf(String name) {
		return ( Classification ) INSTANCES.get( name );
	}

	public static Classification valueOf(Integer ordinal) {
		if ( ordinal == null ) {
			return null;
		}
		switch ( ordinal.intValue() ) {
			case 0: return COOL;
			case 1: return LAME;
			default: throw new IllegalArgumentException( "unknown classification ordinal [" + ordinal + "]" );
		}
	}
}
