/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.synchronization.work;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.boot.AuditService;
import org.hibernate.envers.internal.tools.ArraysTools;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public class AddWorkUnit extends AbstractAuditWorkUnit implements AuditWorkUnit {
	private final Object[] state;
	private final Map<String, Object> data;

	public AddWorkUnit(
			SessionImplementor sessionImplementor,
			String entityName,
			AuditService auditService,
			Serializable id, EntityDescriptor entityPersister, Object[] state) {
		super( sessionImplementor, entityName, auditService, id, RevisionType.ADD );

		this.data = new HashMap<>();
		this.state = state;
		this.auditService.getEntityBindings().get( getEntityName() ).getPropertyMapper()
				.map(
						sessionImplementor,
						data,
						entityPersister.getPropertyNames(),
						state,
						null
				);
	}

	public AddWorkUnit(
			SessionImplementor sessionImplementor,
			String entityName,
			AuditService enversMetadataService,
			Serializable id,
			Map<String, Object> data) {
		super( sessionImplementor, entityName, enversMetadataService, id, RevisionType.ADD );

		this.data = data;
		final String[] propertyNames = sessionImplementor.getFactory()
				.getTypeConfiguration().findEntityPersister( getEntityName() )
				.getPropertyNames();
		this.state = ArraysTools.mapToArray( data, propertyNames );
	}

	@Override
	public boolean containsWork() {
		return true;
	}

	@Override
	public Map<String, Object> generateData(Object revisionData) {
		fillDataWithId( data, revisionData );
		return data;
	}

	public Object[] getState() {
		return state;
	}

	@Override
	public AuditWorkUnit merge(AddWorkUnit second) {
		return second;
	}

	@Override
	public AuditWorkUnit merge(ModWorkUnit second) {
		return new AddWorkUnit( sessionImplementor, entityName, auditService, id, second.getData() );
	}

	@Override
	public AuditWorkUnit merge(DelWorkUnit second) {
		return null;
	}

	@Override
	public AuditWorkUnit merge(CollectionChangeWorkUnit second) {
		second.mergeCollectionModifiedData( data );
		return this;
	}

	@Override
	public AuditWorkUnit merge(FakeBidirectionalRelationWorkUnit second) {
		return FakeBidirectionalRelationWorkUnit.merge( second, this, second.getNestedWorkUnit() );
	}

	@Override
	public AuditWorkUnit dispatch(WorkUnitMergeVisitor first) {
		return first.merge( this );
	}
}
