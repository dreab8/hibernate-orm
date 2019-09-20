/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * @author Steve Ebersole
 */
public class SqlAstTreeHelper {
	/**
	 * Singleton access
	 */
	public static final SqlAstTreeHelper INSTANCE = new SqlAstTreeHelper();

	private SqlAstTreeHelper() {
	}


	public static Predicate combinePredicates(Predicate baseRestriction, Predicate incomingRestriction) {
		if ( baseRestriction == null ) {
			return incomingRestriction;
		}

		final Junction combinedPredicate;

		if ( baseRestriction instanceof Junction ) {
			final Junction junction = (Junction) baseRestriction;
			if ( junction.isEmpty() ) {
				return incomingRestriction;
			}

			if ( junction.getNature() == Junction.Nature.CONJUNCTION ) {
				combinedPredicate = junction;
			}
			else {
				combinedPredicate = new Junction( Junction.Nature.CONJUNCTION );
				combinedPredicate.add( baseRestriction );
			}
		}
		else {
			combinedPredicate = new Junction( Junction.Nature.CONJUNCTION );
		}

		combinedPredicate.add( incomingRestriction );

		return combinedPredicate;
	}
}
