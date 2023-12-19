package org.hibernate.orm.test.merge;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				CompositeIdWithAssociationsAndGeneratedValuesMerge2Test.Middle.class,
				CompositeIdWithAssociationsAndGeneratedValuesMerge2Test.Bottom.class,
				CompositeIdWithAssociationsAndGeneratedValuesMerge2Test.Side.class
		}
)
@SessionFactory
public class CompositeIdWithAssociationsAndGeneratedValuesMerge2Test {

	@Test
	public void testMerge(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					Middle m1 = new Middle( "Middle" );
					new Bottom( m1, 0, "Bottom" );
//					new Side(new PK(1, m1, new PK2(1,3)), "Side");
					Middle merge = session.merge( m1 );
					assertThat( merge.getId() ).isNotNull();
					assertThat( m1.getId() ).isNull();
				}
		);
	}

	@Entity(name = "Middle")
	@Table(name = "middle_table")
	public static class Middle {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(cascade = CascadeType.MERGE)
		private Side side;

		private String name;

		@OneToMany(mappedBy = "middle", cascade = { CascadeType.MERGE, CascadeType.PERSIST })
		private List<Bottom> bottoms;

		public Middle() {
		}

		public Middle(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}


		public String getName() {
			return name;
		}


		public List<Bottom> getBottoms() {
			return bottoms;
		}


		public void addBottom(Bottom bottom) {
			if ( bottoms == null ) {
				bottoms = new ArrayList<>();
			}
			bottoms.add( bottom );
		}

		public void setSide(Side side) {
			this.side = side;
		}
	}

	@Entity(name = "Bottom")
	@Table(name = "bottom_table")
	public static class Bottom {
		@Id
		@ManyToOne(optional = false)
		@JoinColumn(name = "middle_id", nullable = false)
		private Middle middle;

		@Id
		@Column(name = "type_column")
		private Integer type;

		private String note;

		public Bottom() {
		}

		public Bottom(Middle middle, Integer type,String note) {
			this.middle = middle;
			this.middle.addBottom( this );
			this.type = type;
			this.note = note;
		}
	}

	@Entity(name = "Side")
	@Table(name = "side_table")
	public static class Side {
		@EmbeddedId
		private PK pk;

		private String note;

		public Side() {
		}

		public Side(PK pk, String note) {
			this.pk = pk;
			this.note = note;
			pk.middle.setSide(this);
		}

	}

	@Embeddable
	public static class PK {
		private Integer id1;

		@ManyToOne(optional = false)
		private Middle middle;

		private PK2 pk2;

		public PK() {
		}

		public PK(Integer id1, Middle middle, PK2 pk2) {
			this.id1 = id1;
			this.middle = middle;
			this.pk2 = pk2;
		}
	}

	@Embeddable
	public static class PK2{
		private Integer id3;
		private Integer id4;

		public PK2(){}

		public PK2(Integer id3, Integer id4) {
			this.id3 = id3;
			this.id4 = id4;
		}
	}
}
