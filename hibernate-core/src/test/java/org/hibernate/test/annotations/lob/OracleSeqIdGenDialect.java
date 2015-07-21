/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.lob;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.id.SequenceIdentityGenerator;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class OracleSeqIdGenDialect extends Oracle10gDialect {
	@Override
	public Class<?> getNativeIdentifierGeneratorClass() {
		return SequenceIdentityGenerator.class;
	}

	@Override
	public boolean supportsIdentityColumns() {
		return true;
	}

	protected void registerDefaultProperties() {
		super.registerDefaultProperties();
		getDefaultProperties().setProperty( Environment.USE_GET_GENERATED_KEYS, "true" );
	}

}
