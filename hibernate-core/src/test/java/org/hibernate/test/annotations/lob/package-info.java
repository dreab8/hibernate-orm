/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Test package for metatata facilities
 * It contains an example of filter metadata
 */
@TypeDef(
		name = "wrapped_char_text",
		typeClass = CharacterArrayTextTypeImpl.class
		)
@TypeDef(
		name = "char_text",
		typeClass = PrimitiveCharacterArrayTextTypeImpl.class
)
@TypeDef(
		name = "wrapped_image",
		typeClass = WrappedImageTypeImpl.class
)
@TypeDef(
		name = "serializable_image",
		typeClass = SerializableToImageTypeImpl.class
)
package org.hibernate.test.annotations.lob;

import org.hibernate.annotations.TypeDef;

