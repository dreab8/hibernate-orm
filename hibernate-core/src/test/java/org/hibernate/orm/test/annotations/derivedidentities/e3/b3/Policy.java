package org.hibernate.orm.test.annotations.derivedidentities.e3.b3;

import jakarta.persistence.*;

@Entity
@Table(name="`Policy`")
public class Policy {
	@EmbeddedId
	PolicyId id;

	@JoinColumns({
			@JoinColumn(name = "FIRSTNAME", referencedColumnName = "FIRSTNAME"),
			@JoinColumn(name = "LASTNAME", referencedColumnName = "lastName"),
			@JoinColumn(name = "NAME", referencedColumnName = "Name")
	})
	@MapsId("depPK")
	@ManyToOne
	Dependent dep;

}
