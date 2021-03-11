package org.hibernate.test.mapping;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.MappedSuperclass;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class MappedSuperclassWithGenericsTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{
				AbstractSnapshotEventEntry.class,
				SnapshotEventEntry.class,
				AbstractEventEntry.class,
		};
	}

	@Test
	public void testIt(){

	}

	@MappedSuperclass
	public static abstract class AbstractEventEntry<T> {
		private T payload;
	}

	@MappedSuperclass
	@IdClass(AbstractSnapshotEventEntry.PK.class)
	public static abstract class AbstractSnapshotEventEntry<T> extends AbstractEventEntry<T> {

		@Id
		private String aggregateIdentifier;
		@Id
		private long sequenceNumber;
		@Id
		private String type;

		/**
		 * Default constructor required by JPA
		 */
		protected AbstractSnapshotEventEntry() {
		}

		/**
		 * Primary key definition of the AbstractEventEntry class. Is used by JPA to support composite primary keys.
		 */
		@SuppressWarnings("UnusedDeclaration")
		public class PK implements Serializable {

			private String aggregateIdentifier;
			private long sequenceNumber;
			private String type;

			/**
			 * Constructor for JPA. Not to be used directly
			 */
			public PK() {
			}

			@Override
			public boolean equals(Object o) {
				if (this == o) {
					return true;
				}
				if (o == null || getClass() != o.getClass()) {
					return false;
				}
				PK pk = (PK) o;
				return sequenceNumber == pk.sequenceNumber && Objects.equals( aggregateIdentifier, pk.aggregateIdentifier) &&
						Objects.equals(type, pk.type);
			}

			@Override
			public int hashCode() {
				return Objects.hash(aggregateIdentifier, type, sequenceNumber);
			}

			@Override
			public String toString() {
				return "PK{type='" + type + '\'' + ", aggregateIdentifier='" + aggregateIdentifier + '\'' +
						", sequenceNumber=" + sequenceNumber + '}';
			}
		}

	}

	@Entity(name = "SnapshotEventEntry")
	public static class SnapshotEventEntry<T> extends AbstractSnapshotEventEntry<byte[]> {

		String aString;
		/**
		 * Default constructor required by JPA
		 */
		protected SnapshotEventEntry() {
		}
	}

}
