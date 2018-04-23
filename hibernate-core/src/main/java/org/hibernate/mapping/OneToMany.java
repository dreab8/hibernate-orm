/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.JavaTypeMapping;
import org.hibernate.boot.model.relational.ForeignKeyExporter;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * A mapping for a one-to-many association
 *
 * @author Gavin King
 */
public class OneToMany implements ForeignKeyExporter, Value {
	private final MetadataBuildingContext buildingContext;
	private final MappedTable referencingTable;

	private String referencedEntityName;
	private PersistentClass associatedClass;
	private boolean embedded;
	private boolean ignoreNotFound;
	private JavaTypeMapping javaTypeMapping;

	public OneToMany(MetadataBuildingContext buildingContext, PersistentClass owner) throws MappingException {
		this.buildingContext = buildingContext;
		this.referencingTable = ( owner == null ) ? null : owner.getTable();
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return buildingContext.getBuildingOptions().getServiceRegistry();
	}

	public PersistentClass getAssociatedClass() {
		return associatedClass;
	}

	/**
	 * Associated entity on the many side
	 */
	public void setAssociatedClass(PersistentClass associatedClass) {
		this.associatedClass = associatedClass;
	}

	@Override
	public Iterator<Selectable> getColumnIterator() {
		return associatedClass.getKey().getMappedColumns().iterator();
	}

	@Override
	public int getColumnSpan() {
		return associatedClass.getKey().getColumnSpan();
	}

	@Override
	public FetchMode getFetchMode() {
		return FetchMode.JOIN;
	}

	@Override
	public MetadataBuildingContext getMetadataBuildingContext() {
		return buildingContext;
	}

	/**
	 * Table of the owner entity (the "one" side)
	 *
	 * @deprecated since 6.0, use {@link #getMappedTable()}
	 */
	@Deprecated
	public Table getTable() {
		return (Table) referencingTable;
	}

	@Override
	public MappedTable getMappedTable() {
		return referencingTable;
	}

	@Override
	public List<MappedColumn> getMappedColumns() {
		return Collections.unmodifiableList( new ArrayList<>( referencingTable.getMappedColumns() ) );
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public boolean isSimpleValue() {
		return false;
	}

	public boolean isAlternateUniqueKey() {
		return false;
	}

	@Override
	public boolean hasFormula() {
		return false;
	}

	@Override
	public boolean isValid() throws MappingException {
		if ( referencedEntityName == null ) {
			throw new MappingException( "one to many association must specify the referenced entity" );
		}
		return true;
	}

	public String getReferencedEntityName() {
		return referencedEntityName;
	}

	/**
	 * Associated entity on the "many" side
	 */
	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName == null ? null : referencedEntityName.intern();
	}

	@Override
	public void setTypeUsingReflection(String className, String propertyName) {
	}

	@Override
	public Object accept(ValueVisitor visitor) {
		return visitor.accept( this );
	}

	@Override
	public boolean[] getColumnInsertability() {
		//TODO: we could just return all false...
		throw new UnsupportedOperationException();
	}

	public boolean[] getColumnUpdateability() {
		//TODO: we could just return all false...
		throw new UnsupportedOperationException();
	}

	public boolean isIgnoreNotFound() {
		return ignoreNotFound;
	}

	public void setIgnoreNotFound(boolean ignoreNotFound) {
		this.ignoreNotFound = ignoreNotFound;
	}

	@Override
	public JavaTypeMapping getJavaTypeMapping() {
		return javaTypeMapping;
	}

	public void setJavaTypeMapping(JavaTypeMapping javaTypeMapping) {
		this.javaTypeMapping = javaTypeMapping;
	}

	@Override
	public Type getType() throws MappingException {
		return getEntityType();
	}

	private EntityType getEntityType() {
		return buildingContext.getBootstrapContext().getTypeConfiguration()
				.getSessionFactory()
				.getTypeResolver()
				.getTypeFactory()
				.manyToOne(
						getReferencedEntityName(),
						true,
						null,
						false,
						false,
						isIgnoreNotFound(),
						false
				);
	}

	@Override
	public boolean isValid(Mapping mapping) throws MappingException {
		if ( referencedEntityName == null ) {
			throw new MappingException( "one to many association must specify the referenced entity" );
		}
		return true;
	}

	@Override
	public boolean isSame(Value other) {
		return this == other || other instanceof OneToMany && isSame( other );
	}
}
