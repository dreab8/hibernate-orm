/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.cascade.circle;

import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;

/**
 * The test case uses the following model:
 * <p>
 * <-    ->
 * -- (N : 0,1) -- Tour
 * |    <-   ->
 * | -- (1 : N) -- (pickup) ----
 * ->     | |                          |
 * Route -- (1 : N) -- Node                      Transport
 * |  <-   ->                |
 * -- (1 : N) -- (delivery) --
 * <p>
 * Arrows indicate the direction of cascade-merge, cascade-save, and cascade-save-or-update
 * <p>
 * It reproduced the following issues:
 * http://opensource.atlassian.com/projects/hibernate/browse/HHH-3046
 * http://opensource.atlassian.com/projects/hibernate/browse/HHH-3810
 * <p/>
 * This tests that cascades are done properly from each entity.
 *
 * @author Pavol Zibrita, Gail Badner
 */
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/cascade/circle/MultiPathCircleCascade.hbm.xml"
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@ServiceRegistry.Setting(name = Environment.GENERATE_STATISTICS, value = "true"),
				@ServiceRegistry.Setting(name = Environment.STATEMENT_BATCH_SIZE, value = "0"),
		}
)
public class MultiPathCircleCascadeTest extends AbstractMultiPathCircleCascadeTest {
}
