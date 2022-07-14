/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.internal.FilterAliasGenerator;
import org.hibernate.internal.StaticFilterAliasGenerator;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.internal.util.collections.JoinedList;
import org.hibernate.jdbc.Expectation;
import org.hibernate.jdbc.Expectations;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.NamedTableReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.UnionTableGroup;
import org.hibernate.sql.ast.tree.from.UnionTableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * An {@link EntityPersister} implementing the
 * {@link jakarta.persistence.InheritanceType#TABLE_PER_CLASS}
 * mapping strategy for an entity and its inheritance hierarchy.
 * <p>
 * This is implemented as a separate table for each concrete class,
 * with all inherited attributes persisted as columns of that table.
 *
 * @author Gavin King
 */
public class UnionSubclassEntityPersister extends AbstractEntityPersister {

	// the class hierarchy structure
	private final String subquery;
	private final String tableName;
	//private final String rootTableName;
	private final String[] subclassTableNames;
	private final String[] spaces;
	private final String[] subclassSpaces;
	private final String[] subclassTableExpressions;
	private final Object discriminatorValue;
	private final String discriminatorSQLValue;
	private final BasicType<?> discriminatorType;
	private final Map<Object,String> subclassByDiscriminatorValue = new HashMap<>();

	private final String[] constraintOrderedTableNames;
	private final String[][] constraintOrderedKeyColumnNames;

	//INITIALIZATION:

	@Deprecated(since = "6.0")
	public UnionSubclassEntityPersister(
			final PersistentClass persistentClass,
			final EntityDataAccess cacheAccessStrategy,
			final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			final PersisterCreationContext creationContext) throws HibernateException {
		this( persistentClass,cacheAccessStrategy,naturalIdRegionAccessStrategy,
				(RuntimeModelCreationContext) creationContext );
	}

	public UnionSubclassEntityPersister(
			final PersistentClass persistentClass,
			final EntityDataAccess cacheAccessStrategy,
			final NaturalIdDataAccess naturalIdRegionAccessStrategy,
			final RuntimeModelCreationContext creationContext) throws HibernateException {

		super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, creationContext );

		if ( getIdentifierGenerator() instanceof IdentityGenerator ) {
			throw new MappingException(
					"Cannot use identity column key generation with <union-subclass> mapping for: " +
							getEntityName()
			);
		}

		final SessionFactoryImplementor factory = creationContext.getSessionFactory();
		final Dialect dialect = factory.getJdbcServices().getDialect();

		// TABLE

		tableName = determineTableName( persistentClass.getTable() );
		subclassTableNames = new String[]{tableName};
		//Custom SQL

		String sql;
		boolean callable;
		ExecuteUpdateResultCheckStyle checkStyle;
		sql = persistentClass.getCustomSQLInsert();
		callable = sql != null && persistentClass.isCustomInsertCallable();
		checkStyle = sql == null
				? ExecuteUpdateResultCheckStyle.COUNT
				: persistentClass.getCustomSQLInsertCheckStyle() == null
				? ExecuteUpdateResultCheckStyle.determineDefault( sql, callable )
				: persistentClass.getCustomSQLInsertCheckStyle();
		customSQLInsert = new String[] {sql};
		insertCallable = new boolean[] {callable};
		insertExpectations = new Expectation[] { Expectations.appropriateExpectation( checkStyle ) };

		sql = persistentClass.getCustomSQLUpdate();
		callable = sql != null && persistentClass.isCustomUpdateCallable();
		checkStyle = sql == null
				? ExecuteUpdateResultCheckStyle.COUNT
				: persistentClass.getCustomSQLUpdateCheckStyle() == null
				? ExecuteUpdateResultCheckStyle.determineDefault( sql, callable )
				: persistentClass.getCustomSQLUpdateCheckStyle();
		customSQLUpdate = new String[] {sql};
		updateCallable = new boolean[] {callable};
		updateExpectations = new Expectation[] { Expectations.appropriateExpectation( checkStyle ) };

		sql = persistentClass.getCustomSQLDelete();
		callable = sql != null && persistentClass.isCustomDeleteCallable();
		checkStyle = sql == null
				? ExecuteUpdateResultCheckStyle.COUNT
				: persistentClass.getCustomSQLDeleteCheckStyle() == null
				? ExecuteUpdateResultCheckStyle.determineDefault( sql, callable )
				: persistentClass.getCustomSQLDeleteCheckStyle();
		customSQLDelete = new String[] {sql};
		deleteCallable = new boolean[] {callable};
		deleteExpectations = new Expectation[] { Expectations.appropriateExpectation( checkStyle ) };

		discriminatorValue = persistentClass.getSubclassId();
		discriminatorSQLValue = String.valueOf( persistentClass.getSubclassId() );
		discriminatorType = factory.getTypeConfiguration()
				.getBasicTypeRegistry()
				.resolve( StandardBasicTypes.INTEGER );

		// PROPERTIES

		// SUBCLASSES
		subclassByDiscriminatorValue.put(
				persistentClass.getSubclassId(),
				persistentClass.getEntityName()
		);
		if ( persistentClass.isPolymorphic() ) {
			for ( Subclass subclass : persistentClass.getSubclasses() ) {
				subclassByDiscriminatorValue.put( subclass.getSubclassId(), subclass.getEntityName() );
			}
		}

		//SPACES
		//TODO: I'm not sure, but perhaps we should exclude
		//      abstract denormalized tables?

		int spacesSize = 1 + persistentClass.getSynchronizedTables().size();
		spaces = new String[spacesSize];
		spaces[0] = tableName;
		Iterator<String> iter = persistentClass.getSynchronizedTables().iterator();
		for ( int i = 1; i < spacesSize; i++ ) {
			spaces[i] = iter.next();
		}

		HashSet<String> subclassTables = new HashSet<>();
		for ( Table table : persistentClass.getSubclassTableClosure() ) {
			subclassTables.add( determineTableName( table ) );
		}
		subclassSpaces = ArrayHelper.toStringArray( subclassTables );

		subquery = generateSubquery( persistentClass, creationContext.getMetadata() );
		final List<String> tableExpressions = new ArrayList<>( subclassSpaces.length * 2 );
		Collections.addAll( tableExpressions, subclassSpaces );
		tableExpressions.add( subquery );
		PersistentClass parentPersistentClass = persistentClass.getSuperclass();
		while ( parentPersistentClass != null ) {
			tableExpressions.add( generateSubquery( parentPersistentClass, creationContext.getMetadata() ) );
			parentPersistentClass = parentPersistentClass.getSuperclass();
		}
		for ( PersistentClass subPersistentClass : persistentClass.getSubclassClosure() ) {
			if ( subPersistentClass.hasSubclasses() ) {
				tableExpressions.add( generateSubquery( subPersistentClass, creationContext.getMetadata() ) );
			}
		}
		subclassTableExpressions = ArrayHelper.toStringArray( tableExpressions );

		if ( hasMultipleTables() ) {
			int idColumnSpan = getIdentifierColumnSpan();
			ArrayList<String> tableNames = new ArrayList<>();
			ArrayList<String[]> keyColumns = new ArrayList<>();
			for ( Table table : persistentClass.getSubclassTableClosure() ) {
				if ( !table.isAbstractUnionTable() ) {
					tableNames.add( determineTableName( table ) );
					String[] key = new String[idColumnSpan];
					List<Column> columns = table.getPrimaryKey().getColumns();
					for ( int k = 0; k < idColumnSpan; k++ ) {
						key[k] = columns.get(k).getQuotedName( dialect );
					}
					keyColumns.add( key );
				}
			}

			constraintOrderedTableNames = ArrayHelper.toStringArray( tableNames );
			constraintOrderedKeyColumnNames = ArrayHelper.to2DStringArray( keyColumns );
		}
		else {
			constraintOrderedTableNames = new String[] { tableName };
			constraintOrderedKeyColumnNames = new String[][] { getIdentifierColumnNames() };
		}

		initSubclassPropertyAliasesMap( persistentClass );

		postConstruct( creationContext.getMetadata() );
	}

	@Override
	public boolean containsTableReference(String tableExpression) {
		for ( String subclassTableExpression : subclassTableExpressions ) {
			if ( subclassTableExpression.equals( tableExpression ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public TableGroup createRootTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			String explicitSourceAlias,
			Supplier<Consumer<Predicate>> additionalPredicateCollectorAccess,
			SqlAliasBase sqlAliasBase,
			SqlExpressionResolver expressionResolver,
			FromClauseAccess fromClauseAccess,
			SqlAstCreationContext creationContext) {
		final UnionTableReference tableReference = resolvePrimaryTableReference( sqlAliasBase );

		return new UnionTableGroup( canUseInnerJoins, navigablePath, tableReference, this, explicitSourceAlias );
	}

	@Override
	protected UnionTableReference resolvePrimaryTableReference(SqlAliasBase sqlAliasBase) {
		return new UnionTableReference(
				getTableName(),
				subclassTableExpressions,
				sqlAliasBase.generateNewAlias(),
				false,
				getFactory()
		);
	}

	@Override
	public Serializable[] getQuerySpaces() {
		return subclassSpaces;
	}

	@Override
	public String getRootTableName() {
		return tableName;
	}

	@Override
	public String getTableName() {
		return hasSubclasses() ? subquery : tableName;
	}

	@Override
	public Type getDiscriminatorType() {
		return discriminatorType;
	}

	@Override
	public Object getDiscriminatorValue() {
		return discriminatorValue;
	}

	@Override
	public String getDiscriminatorSQLValue() {
		return discriminatorSQLValue;
	}

	@Override
	public String getSubclassForDiscriminatorValue(Object value) {
		return subclassByDiscriminatorValue.get( value );
	}

	@Override
	public Serializable[] getPropertySpaces() {
		return spaces;
	}

	@Override
	protected boolean shouldProcessSuperMapping() {
		return false;
	}

	@Override
	public boolean hasDuplicateTables() {
		return false;
	}

	@Override
	public String getTableName(int j) {
		return tableName;
	}

	@Override
	public String[] getKeyColumns(int j) {
		return getIdentifierColumnNames();
	}

	@Override
	public boolean isTableCascadeDeleteEnabled(int j) {
		return false;
	}

	@Override
	public boolean isPropertyOfTable(int property, int j) {
		return true;
	}

	// Execute the SQL:

	@Override
	public String fromTableFragment(String name) {
		return getTableName() + ' ' + name;
	}

	@Override
	public String getSubclassPropertyTableName(int i) {
		return getTableName();//ie. the subquery! yuck!
	}

	@Override
	public String getAttributeMutationTableName(int attributeIndex) {
		return getRootTableName();
	}

	@Override
	protected int getSubclassPropertyTableNumber(int i) {
		return 0;
	}

	@Override
	public int getSubclassPropertyTableNumber(String propertyName) {
		return 0;
	}

	@Override
	protected String physicalTableNameForMutation(SelectableMapping selectableMapping) {
		return tableName;
	}

	@Override
	protected boolean hasMultipleTables() {
		// This could also just be true all the time...
		return isAbstract() || hasSubclasses();
	}

	@Override
	public void pruneForSubclasses(TableGroup tableGroup, Set<String> treatedEntityNames) {
		final NamedTableReference tableReference = (NamedTableReference) tableGroup.resolveTableReference( getRootTableName() );
		// Replace the default union sub-query with a specially created one that only selects the tables for the treated entity names
		tableReference.setPrunedTableExpression( generateSubquery( treatedEntityNames ) );
	}

	@Override
	public void visitConstraintOrderedTables(ConstraintOrderedTableConsumer consumer) {
		for ( int i = 0; i < constraintOrderedTableNames.length; i++ ) {
			final String tableName = constraintOrderedTableNames[i];
			final int tablePosition = i;

			consumer.consume(
					tableName,
					() -> columnConsumer -> columnConsumer.accept( tableName, constraintOrderedKeyColumnNames[tablePosition] )
			);
		}
	}

	@Override
	protected boolean isPhysicalDiscriminator() {
		return false;
	}

	@Override
	protected EntityDiscriminatorMapping generateDiscriminatorMapping(
			PersistentClass bootEntityDescriptor,
			MappingModelCreationProcess modelCreationProcess) {
		return hasSubclasses() ? super.generateDiscriminatorMapping( bootEntityDescriptor, modelCreationProcess ) : null;
	}

	@Override
	public int getTableSpan() {
		return 1;
	}

	protected boolean[] getTableHasColumns() {
		return ArrayHelper.TRUE;
	}

	@Override
	protected int[] getPropertyTableNumbers() {
		return new int[getPropertySpan()];
	}

	protected String generateSubquery(PersistentClass model, Metadata mapping) {

		Dialect dialect = getFactory().getJdbcServices().getDialect();
		SqlStringGenerationContext sqlStringGenerationContext = getFactory().getSqlStringGenerationContext();

		if ( !model.hasSubclasses() ) {
			return model.getTable().getQualifiedName( sqlStringGenerationContext );
		}

		Set<Column> columns = new LinkedHashSet<>();
		for ( Table table : model.getSubclassTableClosure() ) {
			if ( !table.isAbstractUnionTable() ) {
				columns.addAll( table.getColumns() );
			}
		}

		StringBuilder buf = new StringBuilder()
				.append( "( " );

		List<PersistentClass> classes = new JoinedList<>(
				List.of( model ),
				Collections.unmodifiableList( model.getSubclasses() )
		);

		for ( PersistentClass clazz : classes ) {
			Table table = clazz.getTable();
			if ( !table.isAbstractUnionTable() ) {
				//TODO: move to .sql package!!
				buf.append( "select " );
				for ( Column col : columns ) {
					if ( !table.containsColumn(col) ) {
						int sqlType = col.getSqlTypeCode( mapping );
						buf.append( dialect.getSelectClauseNullString( sqlType, getFactory().getTypeConfiguration() ) )
								.append(" as ");
					}
					buf.append(col.getQuotedName(dialect));
					buf.append(", ");
				}
				buf.append( clazz.getSubclassId() )
						.append( " as clazz_" );
				buf.append( " from " )
						.append(
								table.getQualifiedName(
										sqlStringGenerationContext
								)
						);
				buf.append( " union " );
				if ( dialect.supportsUnionAll() ) {
					buf.append( "all " );
				}
			}
		}

		if ( buf.length() > 2 ) {
			//chop the last union (all)
			buf.setLength( buf.length() - ( dialect.supportsUnionAll() ? 11 : 7 ) );
		}

		return buf.append( " )" ).toString();
	}

	protected String generateSubquery(Set<String> treated) {
		if ( !hasSubclasses() ) {
			return getTableName();
		}

		final Dialect dialect = getFactory().getJdbcServices().getDialect();
		final MappingMetamodelImplementor metamodel = getFactory().getRuntimeMetamodels().getMappingMetamodel();

		// Collect all selectables of every entity subtype and group by selection expression as well as table name
		final LinkedHashMap<String, Map<String, SelectableMapping>> selectables = new LinkedHashMap<>();
		final SelectableConsumer selectableConsumer = (i, selectable) -> {
			selectables.computeIfAbsent( selectable.getSelectionExpression(), k -> new HashMap<>() )
					.put( selectable.getContainingTableExpression(), selectable );
		};
		// Collect the concrete subclass table names for the treated entity names
		final Set<String> treatedTableNames = new HashSet<>( treated.size() );
		for ( String subclassName : treated ) {
			final UnionSubclassEntityPersister subPersister =
					(UnionSubclassEntityPersister) metamodel.getEntityDescriptor( subclassName );
			for ( String subclassTableName : subPersister.getSubclassTableNames() ) {
				if ( ArrayHelper.indexOf( subclassSpaces, subclassTableName ) != -1 ) {
					treatedTableNames.add( subclassTableName );
				}
			}
			subPersister.getIdentifierMapping().forEachSelectable( selectableConsumer );
			if ( subPersister.getVersionMapping() != null ) {
				subPersister.getVersionMapping().forEachSelectable( selectableConsumer );
			}
			subPersister.visitSubTypeAttributeMappings(
					attributeMapping -> attributeMapping.forEachSelectable( selectableConsumer )
			);
		}

		// Create a union sub-query for the table names, like generateSubquery(PersistentClass model, Mapping mapping)
		final StringBuilder buf = new StringBuilder( subquery.length() )
				.append( "( " );

		for ( String name : getSubclassEntityNames() ) {
			final AbstractEntityPersister persister = (AbstractEntityPersister) metamodel.findEntityDescriptor( name );
			final String subclassTableName = persister.getTableName();
			if ( treatedTableNames.contains( subclassTableName ) ) {
				buf.append( "select " );
				for ( Map<String, SelectableMapping> selectableMappings : selectables.values() ) {
					SelectableMapping selectableMapping = selectableMappings.get( subclassTableName );
					if ( selectableMapping == null ) {
						// If there is no selectable mapping for a table name, we render a null expression
						selectableMapping = selectableMappings.values().iterator().next();
						final int sqlType = selectableMapping.getJdbcMapping().getJdbcType()
								.getDefaultSqlTypeCode();
						buf.append( dialect.getSelectClauseNullString( sqlType, getFactory().getTypeConfiguration() ) )
								.append( " as " );
					}
					buf.append(
							new ColumnReference( (String) null, selectableMapping, getFactory() ).getExpressionText()
					);
					buf.append( ", " );
				}
				buf.append( persister.getDiscriminatorSQLValue() ).append( " as clazz_" );
				buf.append( " from " ).append( subclassTableName );
				buf.append( " union " );
				if ( dialect.supportsUnionAll() ) {
					buf.append( "all " );
				}
			}
		}

		if ( buf.length() > 2 ) {
			//chop the last union (all)
			buf.setLength( buf.length() - ( dialect.supportsUnionAll() ? 11 : 7 ) );
		}

		return buf.append( " )" ).toString();
	}

	@Override
	protected String[] getSubclassTableKeyColumns(int j) {
		if ( j != 0 ) {
			throw new AssertionFailure( "only one table" );
		}
		return getIdentifierColumnNames();
	}

	@Override
	public String getSubclassTableName(int j) {
		if ( j != 0 ) {
			throw new AssertionFailure( "only one table" );
		}
		return tableName;
	}

	@Override
	protected String[] getSubclassTableNames(){
		return subclassTableNames;
	}

	@Override
	public int getSubclassTableSpan() {
		return 1;
	}

	@Override
	protected boolean isClassOrSuperclassTable(int j) {
		if ( j != 0 ) {
			throw new AssertionFailure( "only one table" );
		}
		return true;
	}

	@Override
	public String[] getConstraintOrderedTableNameClosure() {
		return constraintOrderedTableNames;
	}

	@Override
	public String[][] getContraintOrderedTableKeyColumnClosure() {
		return constraintOrderedKeyColumnNames;
	}

	@Override
	public FilterAliasGenerator getFilterAliasGenerator(String rootAlias) {
		return new StaticFilterAliasGenerator( rootAlias );
	}
}
