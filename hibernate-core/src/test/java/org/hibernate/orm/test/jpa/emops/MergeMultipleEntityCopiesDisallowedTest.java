/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.emops;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;


/**
 * Tests merging multiple detached representations of the same entity when
 * explicitly disallowed.
 *
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-9106")
@Jpa(
		annotatedClasses = {
				Category.class,
				Hoarder.class,
				Item.class
		},
		integrationSettings = { @Setting(name = AvailableSettings.MERGE_ENTITY_COPY_OBSERVER, value = "disallow") }
)
public class MergeMultipleEntityCopiesDisallowedTest extends MergeMultipleEntityCopiesDisallowedByDefaultTest {
}
