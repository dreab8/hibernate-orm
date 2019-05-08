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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
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
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.HibernateException;
import org.hibernate.annotations.CollectionType;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.annotations.TypeDef;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.usertype.UserCollectionType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.annotations.persister.CollectionPersister;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11147")
@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(lazyLoading = true)
public class CollectionSharedReferenceExceptionTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void testIt2() {
		inTransaction(
				session -> {
					CodeItemEntity codeItemEntity1 = new CodeItemEntity();
					codeItemEntity1.setOid( 1L );
					CodeItemEntity codeItemEntity2 = new CodeItemEntity();
					codeItemEntity2.setOid( 4L );

					CodeTableViewEntity tableViewEntity1 = new CodeTableViewEntity();
					tableViewEntity1.setOid( 2L );
					CodeTableViewEntity tableViewEntity2 = new CodeTableViewEntity();
					tableViewEntity2.setOid( 3L );

					Set<CodeTableViewEntity> hierarchyItems = new LinkedHashSet<>();
					hierarchyItems.add( tableViewEntity1 );

					Set<CodeItemEntity> itemEntities = new LinkedHashSet<>();
					itemEntities.add( codeItemEntity1 );

					tableViewEntity1.setHierarchyItems( itemEntities );

					codeItemEntity1.setFrameworkBusinessCodetableCodeTableViewEntityDefaultItem( hierarchyItems );

					codeItemEntity1.setAllowedFor( hierarchyItems );

					codeItemEntity1.setDependentView( hierarchyItems );
					tableViewEntity1.setDefaultItem( codeItemEntity1 );

					codeItemEntity2.setAllowedFor( hierarchyItems );
					codeItemEntity2.setDependentView( hierarchyItems );
					tableViewEntity1.setDefaultItem( codeItemEntity2 );
					hierarchyItems.add( tableViewEntity2 );

					session.save( codeItemEntity1 );
					session.save( codeItemEntity2 );
					session.save( tableViewEntity1 );
					session.save( tableViewEntity2 );

					session.flush();

				}
		);

		inTransaction(
				session -> {
					CodeItemEntity codeItemEntity1 = session.load( CodeItemEntity.class, 1L );
					Set<CodeTableViewEntity> allowedFor = codeItemEntity1.getAllowedFor();
					Iterator<CodeItemEntity> iterator = allowedFor.iterator().next().getHierarchyItems().iterator();
					iterator.next();
					iterator.next();
					session.flush();


				}
		);

		inTransaction(
				session -> {
					CodeTableViewEntity codeItemEntity1 = session.load( CodeTableViewEntity.class, 2L );
					Set<CodeItemEntity> hierarchyItems = codeItemEntity1.getHierarchyItems();


					session.save( codeItemEntity1 );

				}
		);
	}

	@Test
	public void testIt() {
		List<CodeItemEntity> codeItemEntities = new ArrayList<>(  );
		inTransaction(
				session -> {
					CodeEntity codeEntity = new CodeEntity();
					codeEntity.setOid( 1L );

					CodeEntity codeEntity2 = new CodeEntity();
					codeEntity2.setOid( 2L );

					CodeItemEntity codeItemEntity1 = new CodeItemEntity();
					codeItemEntity1.setOid( 1L );
					CodeItemEntity codeItemEntity2 = new CodeItemEntity();
					codeItemEntity2.setOid( 4L );

					codeItemEntities.add( codeItemEntity1 );

					CodeTableViewEntity tableViewEntity1 = new CodeTableViewEntity();
					tableViewEntity1.setOid( 2L );
					CodeTableViewEntity tableViewEntity2 = new CodeTableViewEntity();
					tableViewEntity2.setOid( 3L );

					tableViewEntity1.addBidirectionalHierarchyItems( codeItemEntity1 );
					tableViewEntity1.addBidirectionalHierarchyItems( codeItemEntity2 );

					tableViewEntity2.addBidirectionalHierarchyItems( codeItemEntity1 );
					tableViewEntity2.addBidirectionalHierarchyItems( codeItemEntity2 );

					codeItemEntity2.addAllowedFor( tableViewEntity1 );
					codeItemEntity2.addBidirectionalDefaultItem( tableViewEntity1 );
					codeItemEntity2.addBidirectionalDefaultItem( tableViewEntity2 );

					codeItemEntity1.addAllowedFor( tableViewEntity1 );
					codeItemEntity1.addBidirectionalDefaultItem( tableViewEntity1 );
					codeItemEntity1.addBidirectionalDefaultItem( tableViewEntity2 );

					codeEntity.setItemEntity( codeItemEntity1 );
					codeEntity2.setItemEntity( codeItemEntity1 );

					session.save( codeEntity );
					session.save( codeEntity2 );

					session.save( codeItemEntity1 );
					session.save( codeItemEntity2 );
					session.save( tableViewEntity1 );
					session.save( tableViewEntity2 );


					session.flush();
				}
		);

		inTransaction(
				session -> {
					List<CodeEntity> codeEntities = session.createQuery( "from CodeEntity", CodeEntity.class).list();

					List<CodeItemEntity> codeItemEntities2 = session.createQuery( "from CodeItemEntity", CodeItemEntity.class).list();

					codeItemEntities2.get( 0 ).getDependentView();

					CodeEntity codeEntity = codeEntities.get( 0 );
//					CodeItemEntity itemEntity = codeEntity.getItemEntity();
//					itemEntity.setVersion( new Short( "6" ) );
//					itemEntity.getDependentView().iterator();
//					codeEntities.get( 1 ).getItemEntity().getDependentView();
					session.save( codeEntity );
					session.flush();
				}
		);

		inTransaction(
				session -> {

					CodeItemEntity codeItemEntity1 = session.load( CodeItemEntity.class, 1L );
					codeItemEntity1.setVersion( new Short( "2" ) );
					codeItemEntity1.getDependentView().iterator();
					session.flush();
				}
		);


//		inTransaction(
//				session -> {
//					CodeItemEntity codeItemEntity1 = session.load( CodeItemEntity.class, 1L );
//					Set<CodeTableViewEntity> allowedFor = codeItemEntity1.getAllowedFor();
//					CodeTableViewEntity next = allowedFor.iterator().next();
//					Set dependentView = next.getDefaultItem().getDependentView();
//					next.getHierarchyItems().iterator().next().getAllowedFor().iterator().next();
//					CodeItemEntity codeItemEntity2 = session.load( CodeItemEntity.class, 4L );
//
////					codeItemEntity1.getAllowedFor().remove( next );
//
//					CodeItemEntity codeItemEntity3 = new CodeItemEntity();
//					codeItemEntity3.setOid( 5L );
//					codeItemEntity3.setDependentView( dependentView );
//					codeItemEntity3.addBidirectionalDefaultItem( next );
//					next.addBidirectionalHierarchyItems( codeItemEntity3 );
//
////					codeItemEntity2.getAllowedFor();
//					session.save( codeItemEntity3 );
//					session.save( codeItemEntity2 );
//					session.save( next );
//					session.flush();
//				}
//		);
//
//		inTransaction(
//				session -> {
//					CodeItemEntity codeItemEntity1 = session.get( CodeItemEntity.class, 1L );
//					Set<CodeTableViewEntity> allowedFor = codeItemEntity1.getAllowedFor();
//					allowedFor.iterator().next().getDefaultItem().getDependentView();
//					allowedFor.iterator().next().getHierarchyItems().iterator().next().getAllowedFor();
//					CodeItemEntity codeItemEntity2 = session.load( CodeItemEntity.class, 4L );
//
//					session.save( codeItemEntity1 );
//					session.flush();
//				}
//		);
//
//		inTransaction(
//				session -> {
//					QueryImplementor<CodeItemEntity> query = session.createQuery(
//							"from CodeItemEntity",
//							CodeItemEntity.class
//					);
//					List<CodeItemEntity> codeItemEntities = query.list();
//					CodeItemEntity codeItemEntity1 = codeItemEntities.get( 0 );
//					CodeItemEntity codeItemEntity2 = codeItemEntities.get( 1 );
//
//					Set<CodeTableViewEntity> allowedFor = codeItemEntity1.getAllowedFor();
//					allowedFor.iterator().next().getHierarchyItems().iterator().next().getAllowedFor();
////					session.save( codeItemEntity1 );
////					session.save( codeItemEntity2 );
//					session.flush();
//				}
//		);
//
//		inTransaction(
//				session -> {
//					QueryImplementor<CodeItemEntity> query = session.createQuery(
//							"from CodeItemEntity",
//							CodeItemEntity.class
//					);
//					List<CodeItemEntity> codeItemEntities = query.list();
//					CodeItemEntity codeItemEntity1 = codeItemEntities.get( 0 );
//					CodeItemEntity codeItemEntity2 = codeItemEntities.get( 1 );
//					Set<CodeTableViewEntity> allowedFor = codeItemEntity1.getAllowedFor();
//					allowedFor.iterator().next().getHierarchyItems().iterator().next().getAllowedFor();
//
//					Iterator<CodeTableViewEntity> iterator = allowedFor.iterator();
//					iterator.next().getDefaultItem();
////					session.save( codeItemEntity1 );
////					session.save( codeItemEntity2 );
//					session.flush();
//				}
//		);
	}

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
		sources.addAnnotatedClass( CodeEntity.class );
	}

	@MappedSuperclass
	@TypeDef(name = "SetCollectionType", typeClass = SetCollectionType.class)
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

	@Entity(name = "CodeEntity")
	public static class CodeEntity extends ModelEntity implements Serializable {

		@OneToOne(fetch = FetchType.LAZY)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		protected CodeItemEntity itemEntity;

		public CodeItemEntity getItemEntity() {
			return itemEntity;
		}

		public void setItemEntity(CodeItemEntity itemEntity) {
			this.itemEntity = itemEntity;
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
		@CollectionType(type = "SetCollectionType")
		protected Set<CodeTableViewEntity> DependentView = null;

		@OneToMany(targetEntity = CodeTableViewEntity.class, mappedBy = "DefaultItem", fetch = FetchType.LAZY)
		@LazyGroup("framework_business_codetable_CodeTableViewEntity_DefaultItem")
		@CollectionType(type = "SetCollectionType")
		protected Set<CodeTableViewEntity> frameworkBusinessCodetableCodeTableViewEntityDefaultItem = null;

		@ManyToMany(targetEntity = CodeTableViewEntity.class, fetch = FetchType.LAZY)
		@LazyGroup("AllowedFor")
		@JoinTable(name = "CodeView_AllowedItems_CodeItem",
				joinColumns = @JoinColumn(name = "AllowedItems_CodeItem_Id"),
				inverseJoinColumns = @JoinColumn(name = "AllowedFor_CodeView_Id")
		)
		@CollectionType(type = "SetCollectionType")
		protected Set<CodeTableViewEntity> AllowedFor = null;

		public Set getDependentView() {
			return DependentView;
		}

		public void setDependentView(Set dependentView) {
			DependentView = dependentView;
		}

		void addDependentView(CodeTableViewEntity view) {
			if ( DependentView == null ) {
				this.DependentView = new LinkedHashSet<>();
			}
			DependentView.add( view );
		}

		public Set getFrameworkBusinessCodetableCodeTableViewEntityDefaultItem() {
			return frameworkBusinessCodetableCodeTableViewEntityDefaultItem;
		}

		public void setFrameworkBusinessCodetableCodeTableViewEntityDefaultItem(Set frameworkBusinessCodetableCodeTableViewEntityDefaultItem) {
			this.frameworkBusinessCodetableCodeTableViewEntityDefaultItem = frameworkBusinessCodetableCodeTableViewEntityDefaultItem;
		}

		public void addBidirectionalDefaultItem(CodeTableViewEntity item) {
			if ( frameworkBusinessCodetableCodeTableViewEntityDefaultItem == null ) {
				frameworkBusinessCodetableCodeTableViewEntityDefaultItem = new LinkedHashSet<>();
			}
			frameworkBusinessCodetableCodeTableViewEntityDefaultItem.add( item );
			item.setDefaultItem( this );
		}

		public Set<CodeTableViewEntity> getAllowedFor() {
			return AllowedFor;
		}

		public void setAllowedFor(Set allowedFor) {
			AllowedFor = allowedFor;
		}

		public void addAllowedFor(CodeTableViewEntity viewEntity) {
			if ( AllowedFor == null ) {
				AllowedFor = new LinkedHashSet<>();
			}
			AllowedFor.add( viewEntity );
		}
	}

	@Entity(name = "CodeTableViewEntity")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@Table(name = "CodeTableView")
	public static class CodeTableViewEntity extends ModelEntity implements Serializable {

		@ManyToMany(targetEntity = CodeItemEntity.class, fetch = FetchType.LAZY, mappedBy = "DependentView")
		@LazyGroup("HierarchyItems")
		@CollectionType(type = "SetCollectionType")
		protected Set<CodeItemEntity> HierarchyItems = null;

		@ManyToOne(fetch = FetchType.LAZY)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		@LazyGroup("code")
		protected CodeItemEntity DefaultItem;


		public Set<CodeItemEntity> getHierarchyItems() {
			return HierarchyItems;
		}

		public void setHierarchyItems(Set<CodeItemEntity> hierarchyItems) {
			HierarchyItems = hierarchyItems;
		}

		public void addBidirectionalHierarchyItems(CodeItemEntity item) {
			if ( HierarchyItems == null ) {
				HierarchyItems = new LinkedHashSet<>();
			}
			HierarchyItems.add( item );
			item.addDependentView( this );
		}

		public CodeItemEntity getDefaultItem() {
			return DefaultItem;
		}

		public void setDefaultItem(CodeItemEntity defaultItem) {
			DefaultItem = defaultItem;
		}
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
