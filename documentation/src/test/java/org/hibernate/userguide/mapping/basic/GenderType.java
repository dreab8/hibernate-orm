package org.hibernate.userguide.mapping.basic;

import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.descriptor.sql.spi.CharSqlDescriptor;

/**
 * @author Vlad Mihalcea
 */
//tag::basic-enums-custom-type-example[]
public class GenderType extends BasicTypeImpl<Gender> {

    public static final GenderType INSTANCE = new GenderType();

    public GenderType() {
        super(
				CharSqlDescriptor.INSTANCE,
				GenderJavaTypeDescriptor.INSTANCE
        );
    }

    public String getName() {
        return "gender";
    }
}
//end::basic-enums-custom-type-example[]
