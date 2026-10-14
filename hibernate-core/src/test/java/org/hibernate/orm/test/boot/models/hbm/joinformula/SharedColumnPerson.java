package org.hibernate.orm.test.boot.models.hbm.joinformula;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

/**
 * A {@code @OneToOne} whose join reuses the owner's own id column, read-only, mixed
 * with a formula literal -- the "shared owner identifier column" pattern described
 * as a pending (not-yet-implemented) scope extension in the formula-one-to-one
 * specification. Such a mixed tuple is expected to keep its existing logical
 * many-to-one binding, just like an ordinary mixed column/formula tuple.
 *
 * @author Steve Ebersole
 */
@Entity
public class SharedColumnPerson {
	@Id
	private String name;

	@OneToOne(optional = true, cascade = CascadeType.ALL)
	@JoinColumnsOrFormulas({
			@JoinColumnOrFormula(column = @JoinColumn(
					name = "name", referencedColumnName = "personName",
					insertable = false, updatable = false)),
			@JoinColumnOrFormula(formula = @JoinFormula(
					value = "'HOME'", referencedColumnName = "addressType"))
	})
	private SharedColumnAddress address;
}
