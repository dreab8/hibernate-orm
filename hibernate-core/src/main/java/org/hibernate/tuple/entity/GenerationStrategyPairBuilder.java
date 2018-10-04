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
package org.hibernate.tuple.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.InDatabaseValueGenerationStrategy;
import org.hibernate.tuple.InMemoryValueGenerationStrategy;
import org.hibernate.tuple.ValueGeneration;
import org.hibernate.tuple.ValueGenerator;

import static org.hibernate.internal.CoreLogging.messageLogger;

/**
 * @author Andrea Boriero
 */
public class GenerationStrategyPairBuilder {
	private static final CoreMessageLogger LOG = messageLogger( GenerationStrategyPairBuilder.class );

	private static final GenerationStrategyPair NO_GEN_PAIR = new GenerationStrategyPair();

	public static GenerationStrategyPair buildGenerationStrategyPair(
			final SessionFactoryImplementor sessionFactory,
			final Property mappingProperty) {
		final ValueGeneration valueGeneration = mappingProperty.getValueGenerationStrategy();
		if ( valueGeneration != null && valueGeneration.getGenerationTiming() != GenerationTiming.NEVER ) {
			// the property is generated in full. build the generation strategy pair.
			if ( valueGeneration.getValueGenerator() != null ) {
				// in-memory generation
				return new GenerationStrategyPair(
						FullInMemoryValueGenerationStrategy.create( valueGeneration )
				);
			}
			else {
				// in-db generation
				return new GenerationStrategyPair(
						create(
								sessionFactory,
								mappingProperty,
								valueGeneration
						)
				);
			}
		}
		else if ( mappingProperty.getValue() instanceof Component ) {
			final CompositeGenerationStrategyPairBuilder builder = new CompositeGenerationStrategyPairBuilder(
					mappingProperty );
			interpretPartialCompositeValueGeneration( sessionFactory, (Component) mappingProperty.getValue(), builder );
			return builder.buildPair();
		}

		return NO_GEN_PAIR;
	}

	private static void interpretPartialCompositeValueGeneration(
			SessionFactoryImplementor sessionFactory,
			Component composite,
			CompositeGenerationStrategyPairBuilder builder) {
		Iterator subProperties = composite.getPropertyIterator();
		while ( subProperties.hasNext() ) {
			final Property subProperty = (Property) subProperties.next();
			builder.addPair( buildGenerationStrategyPair( sessionFactory, subProperty ) );
		}
	}

	public static InDatabaseValueGenerationStrategyImpl create(
			SessionFactoryImplementor sessionFactoryImplementor,
			Property mappingProperty,
			ValueGeneration valueGeneration) {
		final int numberOfMappedColumns = mappingProperty.getType().getColumnSpan( sessionFactoryImplementor );
		if ( numberOfMappedColumns == 1 ) {
			return new InDatabaseValueGenerationStrategyImpl(
					valueGeneration.getGenerationTiming(),
					valueGeneration.referenceColumnInSql(),
					new String[] { valueGeneration.getDatabaseGeneratedReferencedColumnValue() }

			);
		}
		else {
			if ( valueGeneration.getDatabaseGeneratedReferencedColumnValue() != null ) {
				LOG.debugf(
						"Value generator specified column value in reference to multi-column attribute [%s -> %s]; ignoring",
						mappingProperty.getPersistentClass(),
						mappingProperty.getName()
				);
			}
			return new InDatabaseValueGenerationStrategyImpl(
					valueGeneration.getGenerationTiming(),
					valueGeneration.referenceColumnInSql(),
					new String[numberOfMappedColumns]
			);
		}
	}

	public static class GenerationStrategyPair {
		private final InMemoryValueGenerationStrategy inMemoryStrategy;
		private final InDatabaseValueGenerationStrategy inDatabaseStrategy;

		public GenerationStrategyPair() {
			this( NoInMemoryValueGenerationStrategy.INSTANCE, NoInDatabaseValueGenerationStrategy.INSTANCE );
		}

		public GenerationStrategyPair(FullInMemoryValueGenerationStrategy inMemoryStrategy) {
			this( inMemoryStrategy, NoInDatabaseValueGenerationStrategy.INSTANCE );
		}

		public GenerationStrategyPair(InDatabaseValueGenerationStrategyImpl inDatabaseStrategy) {
			this( NoInMemoryValueGenerationStrategy.INSTANCE, inDatabaseStrategy );
		}

		public GenerationStrategyPair(
				InMemoryValueGenerationStrategy inMemoryStrategy,
				InDatabaseValueGenerationStrategy inDatabaseStrategy) {
			// perform some normalization.  Also check that only one (if any) strategy is specified
			if ( inMemoryStrategy == null ) {
				inMemoryStrategy = NoInMemoryValueGenerationStrategy.INSTANCE;
			}
			if ( inDatabaseStrategy == null ) {
				inDatabaseStrategy = NoInDatabaseValueGenerationStrategy.INSTANCE;
			}

			if ( inMemoryStrategy.getGenerationTiming() != GenerationTiming.NEVER
					&& inDatabaseStrategy.getGenerationTiming() != GenerationTiming.NEVER ) {
				throw new ValueGenerationStrategyException(
						"in-memory and in-database value generation are mutually exclusive"
				);
			}

			this.inMemoryStrategy = inMemoryStrategy;
			this.inDatabaseStrategy = inDatabaseStrategy;
		}

		public InMemoryValueGenerationStrategy getInMemoryStrategy() {
			return inMemoryStrategy;
		}

		public InDatabaseValueGenerationStrategy getInDatabaseStrategy() {
			return inDatabaseStrategy;
		}
	}

	public static class ValueGenerationStrategyException extends HibernateException {
		public ValueGenerationStrategyException(String message) {
			super( message );
		}
	}

	private static class CompositeGenerationStrategyPairBuilder {
		private final Property mappingProperty;

		private boolean hadInMemoryGeneration;
		private boolean hadInDatabaseGeneration;

		private List<InMemoryValueGenerationStrategy> inMemoryStrategies;
		private List<InDatabaseValueGenerationStrategy> inDatabaseStrategies;

		public CompositeGenerationStrategyPairBuilder(Property mappingProperty) {
			this.mappingProperty = mappingProperty;
		}

		public void addPair(GenerationStrategyPair generationStrategyPair) {
			add( generationStrategyPair.getInMemoryStrategy() );
			add( generationStrategyPair.getInDatabaseStrategy() );
		}

		private void add(InMemoryValueGenerationStrategy inMemoryStrategy) {
			if ( inMemoryStrategies == null ) {
				inMemoryStrategies = new ArrayList<>();
			}
			inMemoryStrategies.add( inMemoryStrategy );

			if ( inMemoryStrategy.getGenerationTiming() != GenerationTiming.NEVER ) {
				hadInMemoryGeneration = true;
			}
		}

		private void add(InDatabaseValueGenerationStrategy inDatabaseStrategy) {
			if ( inDatabaseStrategies == null ) {
				inDatabaseStrategies = new ArrayList<>();
			}
			inDatabaseStrategies.add( inDatabaseStrategy );

			if ( inDatabaseStrategy.getGenerationTiming() != GenerationTiming.NEVER ) {
				hadInDatabaseGeneration = true;
			}
		}

		public GenerationStrategyPair buildPair() {
			if ( hadInMemoryGeneration && hadInDatabaseGeneration ) {
				throw new ValueGenerationStrategyException(
						"Composite attribute [" + mappingProperty.getName() + "] contained both in-memory"
								+ " and in-database value generation"
				);
			}
			else if ( hadInMemoryGeneration ) {
				throw new NotYetImplementedException( "Still need to wire in composite in-memory value generation" );

			}
			else if ( hadInDatabaseGeneration ) {
				final Component composite = (Component) mappingProperty.getValue();

				// we need the numbers to match up so we can properly handle 'referenced sql column values'
				if ( inDatabaseStrategies.size() != composite.getPropertySpan() ) {
					throw new ValueGenerationStrategyException(
							"Internal error : mismatch between number of collected in-db generation strategies" +
									" and number of attributes for composite attribute : " + mappingProperty.getName()
					);
				}

				// the base-line values for the aggregated InDatabaseValueGenerationStrategy we will build here.
				GenerationTiming timing = GenerationTiming.INSERT;
				boolean referenceColumns = false;
				String[] columnValues = new String[composite.getColumnSpan()];

				// start building the aggregate values
				int propertyIndex = -1;
				int columnIndex = 0;
				Iterator subProperties = composite.getPropertyIterator();
				while ( subProperties.hasNext() ) {
					propertyIndex++;
					final Property subProperty = (Property) subProperties.next();
					final InDatabaseValueGenerationStrategy subStrategy = inDatabaseStrategies.get( propertyIndex );

					if ( subStrategy.getGenerationTiming() == GenerationTiming.ALWAYS ) {
						// override the base-line to the more often "ALWAYS"...
						timing = GenerationTiming.ALWAYS;

					}
					if ( subStrategy.referenceColumnsInSql() ) {
						// override base-line value
						referenceColumns = true;
					}
					if ( subStrategy.getReferencedColumnValues() != null ) {
						if ( subStrategy.getReferencedColumnValues().length != subProperty.getColumnSpan() ) {
							throw new ValueGenerationStrategyException(
									"Internal error : mismatch between number of collected 'referenced column values'" +
											" and number of columns for composite attribute : " + mappingProperty.getName() +
											'.' + subProperty.getName()
							);
						}
						System.arraycopy(
								subStrategy.getReferencedColumnValues(),
								0,
								columnValues,
								columnIndex,
								subProperty.getColumnSpan()
						);
					}
				}

				// then use the aggregated values to build the InDatabaseValueGenerationStrategy
				return new GenerationStrategyPair(
						new InDatabaseValueGenerationStrategyImpl( timing, referenceColumns, columnValues )
				);
			}
			else {
				return NO_GEN_PAIR;
			}
		}
	}

	private static class NoInMemoryValueGenerationStrategy implements InMemoryValueGenerationStrategy {
		/**
		 * Singleton access
		 */
		public static final NoInMemoryValueGenerationStrategy INSTANCE = new NoInMemoryValueGenerationStrategy();

		@Override
		public GenerationTiming getGenerationTiming() {
			return GenerationTiming.NEVER;
		}

		@Override
		public ValueGenerator getValueGenerator() {
			return null;
		}
	}

	private static class FullInMemoryValueGenerationStrategy implements InMemoryValueGenerationStrategy {
		private final GenerationTiming timing;
		private final ValueGenerator generator;

		private FullInMemoryValueGenerationStrategy(GenerationTiming timing, ValueGenerator generator) {
			this.timing = timing;
			this.generator = generator;
		}

		public static FullInMemoryValueGenerationStrategy create(ValueGeneration valueGeneration) {
			return new FullInMemoryValueGenerationStrategy(
					valueGeneration.getGenerationTiming(),
					valueGeneration.getValueGenerator()
			);
		}

		@Override
		public GenerationTiming getGenerationTiming() {
			return timing;
		}

		@Override
		public ValueGenerator getValueGenerator() {
			return generator;
		}
	}

	private static class NoInDatabaseValueGenerationStrategy implements InDatabaseValueGenerationStrategy {
		/**
		 * Singleton access
		 */
		public static final NoInDatabaseValueGenerationStrategy INSTANCE = new NoInDatabaseValueGenerationStrategy();

		@Override
		public GenerationTiming getGenerationTiming() {
			return GenerationTiming.NEVER;
		}

		@Override
		public boolean referenceColumnsInSql() {
			return true;
		}

		@Override
		public String[] getReferencedColumnValues() {
			return null;
		}
	}

	private static class InDatabaseValueGenerationStrategyImpl implements InDatabaseValueGenerationStrategy {
		private final GenerationTiming timing;
		private final boolean referenceColumnInSql;
		private final String[] referencedColumnValues;

		private InDatabaseValueGenerationStrategyImpl(
				GenerationTiming timing,
				boolean referenceColumnInSql,
				String[] referencedColumnValues) {
			this.timing = timing;
			this.referenceColumnInSql = referenceColumnInSql;
			this.referencedColumnValues = referencedColumnValues;
		}

		@Override
		public GenerationTiming getGenerationTiming() {
			return timing;
		}

		@Override
		public boolean referenceColumnsInSql() {
			return referenceColumnInSql;
		}

		@Override
		public String[] getReferencedColumnValues() {
			return referencedColumnValues;
		}
	}
}
