/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit5.dynamictests;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * {@link org.junit.jupiter.api.BeforeAll} equivalent.
 *
 * @author Chris Cranford
 */
@Target( METHOD )
@Retention( RUNTIME )
@Inherited
public @interface DynamicBeforeAll {
	/**
	 * Default empty exception.
	 */
	class None extends Throwable {
		private None() {
		}
	}

	/**
	 * An expected {@link Throwable} to cause a test method to succeed, but only if an exception
	 * of the <code>expected</code> type is thrown.
	 */
	Class<? extends Throwable> expected() default None.class;
}
