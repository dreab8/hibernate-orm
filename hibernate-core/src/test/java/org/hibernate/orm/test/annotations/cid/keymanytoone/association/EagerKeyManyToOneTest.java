/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.cid.keymanytoone.association;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				Card.class, CardField.class, Key.class, PrimaryKey.class
		})
@SessionFactory(statementInspectorClass = SQLStatementInspector.class)
public class EagerKeyManyToOneTest {
	public static final String CARD_ID = "cardId";
	public static final String KEY_ID = "keyId";

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Card card = new Card( CARD_ID );
					Key key = new Key( KEY_ID );
					CardField field = new CardField( card, key );
					card.setField( field );

					session.persist( key );
					session.persist( card );

					session.persist( field );
				}
		);
	}

	@AfterEach
	public void cleanUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Key" ).executeUpdate();
					session.createQuery( "delete from Card" ).executeUpdate();
					session.createQuery( "delete from CardField" ).executeUpdate();
				}
		);
	}

	@Test
	public void testLoadEntityWithEagerFetchingToKeyManyToOneReferenceBackToSelf(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					try {
						/*
						select
							c1_0.id,
							f1_0.card_id,
							f1_0."key_id",
							f1_0.card_id,
							f1_0."key_id",
							k1_0.id
						from
							Card as c1_0
						left outer join
							CardField as f1_0
								on c1_0.id=f1_0.card_id
						left outer join
							"key" k1_0
								on f1_0."key_id"=k1_0.id
						where
							c1_0.id=?`
						 */
						Card card = session.get( Card.class, CARD_ID );

						CardField cf = card.getField();
						assertSame( card, cf.getPrimaryKey().getCard() );

						statementInspector.assertExecutedCount( 1 );
						statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 2 );
					}
					catch (StackOverflowError soe) {
						fail( "eager + key-many-to-one caused stack-overflow in annotations" );
					}
				}
		);
	}
}
