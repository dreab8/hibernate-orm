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
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.HibernateException;
import org.hibernate.annotations.CollectionType;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserCollectionType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.annotations.persister.CollectionPersister;
import org.junit.runner.RunWith;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11147")
@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(lazyLoading = true)
public class CollectionSharedReferenceExceptionTest extends BaseNonConfigCoreFunctionalTestCase {


	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PPROXY, "true" );
		ssrb.applySetting( AvailableSettings.FORMAT_SQL, "false" );
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		super.configureSessionFactoryBuilder( sfb );
		sfb.applyStatisticsSupport( true );
		sfb.applySecondLevelCacheSupport( false );
		sfb.applyQueryCacheSupport( false );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( CodeItemEntity.class );
		sources.addAnnotatedClass( CodeTableViewEntity.class );
	}

	@MappedSuperclass
	public static abstract class ModelEntity {
		@Id
		private long oid;
		private short version;

		public long getOid() {
			return oid;
		}

		public void setOid(long oid) {
			this.oid = oid;
		}

		public short getVersion() {
			return version;
		}

		public void setVersion(short version) {
			this.version = version;
		}
	}

	@Entity(name = "CodeItemEntity")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@Table(name = "CodeItem")
	public static class CodeItemEntity extends ModelEntity implements Serializable {

		@ManyToMany(targetEntity = CodeTableViewEntity.class, fetch = FetchType.LAZY)
		@LazyGroup("DependentView")
		@JoinTable(name = "CodeItem_DepView_CodeView",
				joinColumns = @JoinColumn(name = "HierarchyIts_CodeItem_Id"),
				inverseJoinColumns = @JoinColumn(name = "DepView_CodeView_Id")

		)
		@CollectionType(type = "baseutil.technology.hibernate.IskvLinkedSetCollectionType")
		protected Set<CodeTableViewEntity> DependentView = null;

		@OneToMany(targetEntity = CodeTableViewEntity.class, mappedBy = "DefaultItem", fetch = FetchType.LAZY)
		@LazyGroup("framework_business_codetable_CodeTableViewEntity_DefaultItem")
		@CollectionType(type = "org.hibernate.test.bytecode.enhancement.lazy.proxy")
		protected Set<CodeTableViewEntity> frameworkBusinessCodetableCodeTableViewEntityDefaultItem = null;

		@ManyToMany(targetEntity = CodeTableViewEntity.class, fetch = FetchType.LAZY)
		@LazyGroup("AllowedFor")
		@JoinTable(name = "CodeView_AllowedItems_CodeItem",
				joinColumns = @JoinColumn(name = "AllowedItems_CodeItem_Id"),
				inverseJoinColumns = @JoinColumn(name = "AllowedFor_CodeView_Id")
		)
		@CollectionType(type = "org.hibernate.test.bytecode.enhancement.lazy.proxy")
		protected Set<CodeTableViewEntity> AllowedFor = null;

		public Set getDependentView() {
			return DependentView;
		}

		public void setDependentView(Set dependentView) {
			DependentView = dependentView;
		}

		public Set getFrameworkBusinessCodetableCodeTableViewEntityDefaultItem() {
			return frameworkBusinessCodetableCodeTableViewEntityDefaultItem;
		}

		public void setFrameworkBusinessCodetableCodeTableViewEntityDefaultItem(Set frameworkBusinessCodetableCodeTableViewEntityDefaultItem) {
			this.frameworkBusinessCodetableCodeTableViewEntityDefaultItem = frameworkBusinessCodetableCodeTableViewEntityDefaultItem;
		}

		public Set getAllowedFor() {
			return AllowedFor;
		}

		public void setAllowedFor(Set allowedFor) {
			AllowedFor = allowedFor;
		}
	}

	@Entity(name = "CodeTableViewEntity")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@Table(name = "CodeTableView")
	public static class CodeTableViewEntity extends ModelEntity implements Serializable {

		@ManyToMany(targetEntity = CodeItemEntity.class, fetch = FetchType.LAZY, mappedBy = "DependentView")
		@LazyGroup("HierarchyItems")
		@CollectionType(type = "org.hibernate.test.bytecode.enhancement.lazy.proxy.SetCollectionType")
		protected Set HierarchyItems = null;
	}

	public static class SetCollectionType implements UserCollectionType {

		public PersistentCollection instantiate(SharedSessionContractImplementor session, CollectionPersister persister)
				throws HibernateException {
			return new PersistentSet( session );
		}

		public Iterator getElementsIterator(Object collection) {
			Set lSet = (Set) collection;
			return lSet.iterator();
		}

		public boolean contains(Object collection, Object entity) {
			Set lSet = (Set) collection;
			return lSet.contains( entity );
		}

		public PersistentCollection wrap(SharedSessionContractImplementor session, Object collection) {
			return new PersistentSet( session, (Set) collection );
		}

		public Object indexOf(Object collection, Object entity) {
			if ( entity != null ) {
				final Iterator iter = getElementsIterator( collection );
				int index = 0;
				while ( iter.hasNext() ) {
					if ( entity.equals( iter.next() ) ) {
						return new Integer( index );
					}
					index++;
				}
			}
			return null;
		}

		@Override
		public Object replaceElements(
				Object o,
				Object o1,
				org.hibernate.persister.collection.CollectionPersister persister,
				Object owner,
				Map copyCache,
				SharedSessionContractImplementor session) throws HibernateException {
			Set setA = (Set) o;
			Set setB = (Set) o1;
			setB.clear();
			setB.addAll( setA );
			return setB;
		}

		public Object instantiate(int anticipatedSize) {
			LinkedHashSet result = null;
			if ( anticipatedSize < 0 ) {
				result = new LinkedHashSet();
			}
			else {
				result = new LinkedHashSet( anticipatedSize );
			}
			return result;
		}

		@Override
		public PersistentCollection instantiate(
				SharedSessionContractImplementor session,
				org.hibernate.persister.collection.CollectionPersister persister) throws HibernateException {
			return new PersistentSet( session );
		}
	}
}
