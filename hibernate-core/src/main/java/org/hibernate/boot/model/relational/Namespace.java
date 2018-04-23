/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.relational;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.Table;
import org.hibernate.naming.NamespaceName;

/**
 * Represents a namespace (named schema/catalog pair) with a Database and manages objects defined within.
 *
 * @author Steve Ebersole
 *
 * @deprecated since 6.0 use {@link MappedNamespace} instead.
 */
@Deprecated
public class Namespace extends MappedNamespace {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( Namespace.class );

	private final Database database;
	private final Name name;

	private Map<Identifier, Table> tables = new TreeMap<Identifier, Table>();
	private Map<Identifier, Sequence> sequences = new TreeMap<Identifier, Sequence>();

	public Namespace(Database database, Name name) {
		super(new NamespaceName( name.getCatalog(), name.getSchema() ) );
		this.database = database;
		this.name = name;

	}

	public Name getName() {
		return name;
	}

//	public Collection<Table> getTables() {
//		return tables.values();
//	}

	/**
	 * Returns the table with the specified logical table name.
	 *
	 * @param logicalTableName - the logical name of the table
	 *
	 * @return the table with the specified table name,
	 *         or null if there is no table with the specified
	 *         table name.
	 */
	public Table locateTable(Identifier logicalTableName) {
		return tables.get( logicalTableName );
	}

	/**
	 * Creates a mapping Table instance.
	 *
	 * @param logicalTableName The logical table name
	 *
	 * @return the created table.
	 */
	public Table createTable(Identifier logicalTableName, boolean isAbstract) {
		return (Table) super.createTable( logicalTableName, isAbstract );
	}

	public DenormalizedTable createDenormalizedTable(Identifier logicalTableName, boolean isAbstract, Table includedTable) {
		return (DenormalizedTable) super.createDenormalizedTable( logicalTableName, isAbstract, includedTable );
	}

	public Sequence locateSequence(Identifier name) {
		return (Sequence) super.locateSequence( name );
	}

	public Sequence createSequence(Identifier logicalName, int initialValue, int increment) {
		return (Sequence) super.createSequence( logicalName, initialValue, increment );
	}

	@Override
	public String toString() {
		return super.toString();
	}

	@Override
	public boolean equals(Object o) {
		return super.equals( o );
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	public static class Name extends NamespaceName {

		public Name(Identifier catalog, Identifier schema) {
			super(catalog,schema);
		}

		public Identifier getCatalog() {
			return (Identifier)super.getCatalog();
		}

		public Identifier getSchema() {
			return (Identifier)super.getSchema();
		}

		@Override
		public String toString() {
			return "Name" + "{catalog=" + getCatalog() + ", schema=" + getSchema() + '}';
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			final Name that = (Name) o;

			return EqualsHelper.equals( getSchema(), getCatalog() )
					&& EqualsHelper.equals( getSchema(), getCatalog() );
		}

		@Override
		public int hashCode() {
			int result = getCatalog() != null ? getCatalog().hashCode() : 0;
			result = 31 * result + (getSchema() != null ? getSchema().hashCode() : 0);
			return result;
		}

		public int compareTo(Name that) {
			// per Comparable, the incoming Name cannot be null.  However, its catalog/schema might be
			// so we need to account for that.
			int catalogCheck = ComparableHelper.compare( this.getCatalog(), that.getCatalog() );
			if ( catalogCheck != 0 ) {
				return catalogCheck;
			}

			return ComparableHelper.compare( this.getSchema(), that.getSchema() );
		}
	}

	public static class ComparableHelper {
		public static <T extends Comparable<T>> int compare(T first, T second) {
			if ( first == null ) {
				if ( second == null ) {
					return 0;
				}
				else {
					return 1;
				}
			}
			else {
				if ( second == null ) {
					return -1;
				}
				else {
					return first.compareTo( second );
				}
			}
		}
	}
}
