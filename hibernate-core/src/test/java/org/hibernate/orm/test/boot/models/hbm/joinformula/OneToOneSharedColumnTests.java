package org.hibernate.orm.test.boot.models.hbm.joinformula;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the mixed tuple described in the formula-one-to-one specification as the
 * "read-only physical owner column" extension: a {@code @OneToOne} join mixing a
 * read-only {@code @JoinColumn} that reuses the owner's own id column with a
 * {@code @JoinFormula}. That extension is not implemented -- this confirms the
 * mapping still binds as the existing logical many-to-one rather than being
 * reinterpreted as the formula-only "special one-to-one".
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class OneToOneSharedColumnTests {
	@Test
	@DomainModel( annotatedClasses = {SharedColumnPerson.class, SharedColumnAddress.class} )
	void testSharedReadOnlyColumnPlusFormula(DomainModelScope domainModelScope) {
		final PersistentClass personEntityBinding = domainModelScope.getEntityBinding( SharedColumnPerson.class );
		final Property address = personEntityBinding.getProperty( "address" );
		final Value addressValue = address.getValue();

		// a mixed column/formula tuple -- even a read-only one -- is not eligible
		// for the formula-only special-one-to-one binding path
		assertThat( addressValue ).isInstanceOf( ManyToOne.class );
		assertThat( ( (ManyToOne) addressValue ).isLogicalOneToOne() ).isTrue();

		assertThat( addressValue.getSelectables() ).hasSize( 2 );
		assertThat( ( (Column) addressValue.getSelectables().get( 0 ) ).getName() ).isEqualTo( "name" );
		assertThat( ( (Formula) addressValue.getSelectables().get( 1 ) ).getFormula() ).isEqualTo( "'HOME'" );

		// the shared column is read-only: the association must not write to it
		assertThat( addressValue.getColumnInsertability() ).containsOnly( false );
		assertThat( addressValue.getColumnUpdateability() ).containsOnly( false );

		// no duplicate/conflicting "name" column was created on Person's table
		assertThat( personEntityBinding.getTable().getColumns() )
				.extracting( Column::getName )
				.containsExactly( "name" );
	}
}
