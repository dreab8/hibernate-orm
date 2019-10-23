/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.basic;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.engine.spi.SelfDirtinessTracker;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@RunWith( BytecodeEnhancerRunner.class )
public class InlineDirtyCheckingTest {

	@Test
	public void testSetDefaultValueTriggersInlineDirtyChecking(){
		TestEntity testEntity = new TestEntity(  );
		assertThat(testEntity, instanceOf( SelfDirtinessTracker.class ));
		testEntity.setaBoolean( false );
		SelfDirtinessTracker selfDirtinessTracker = (SelfDirtinessTracker) testEntity;
		assertTrue( selfDirtinessTracker.$$_hibernate_hasDirtyAttributes() );
	}

	@Test
	public void testSetValueTriggersInlineDirtyChecking(){
		TestEntity testEntity = new TestEntity(  );
		assertThat(testEntity, instanceOf( SelfDirtinessTracker.class ));
		testEntity.setaBoolean( true );
		SelfDirtinessTracker selfDirtinessTracker = (SelfDirtinessTracker) testEntity;
		assertTrue( selfDirtinessTracker.$$_hibernate_hasDirtyAttributes() );
	}


	@Entity
	public static class TestEntity {
		@Id
		@GeneratedValue
		private long id;
		public String surname;
		private String name;
		private boolean aBoolean;
		private int anInt;
		private Integer anInteger;

		public TestEntity() {
		}

		public TestEntity(String name) {
			this.name = name;
		}

		public long getId() {
			return this.id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean isaBoolean() {
			return this.aBoolean;
		}

		public void setaBoolean(boolean aBoolean) {
			this.aBoolean = aBoolean;
		}

		public int getAnInt() {
			return this.anInt;
		}

		public void setAnInt(int anInt) {
			this.anInt = anInt;
		}

		public Integer getAnInteger() {
			return this.anInteger;
		}

		public void setAnInteger(Integer anInteger) {
			this.anInteger = anInteger;
		}
	}
}
