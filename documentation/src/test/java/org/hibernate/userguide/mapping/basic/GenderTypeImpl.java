package org.hibernate.userguide.mapping.basic;

import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.descriptor.sql.spi.CharSqlDescriptor;

/**
 * @author Vlad Mihalcea
 */
//tag::basic-enums-custom-type-example[]
public class GenderTypeImpl extends BasicTypeImpl<Gender> {

    public static final GenderTypeImpl INSTANCE = new GenderTypeImpl();

    public GenderTypeImpl() {
        super(
				CharSqlDescriptor.INSTANCE,
				GenderJavaTypeDescriptor.INSTANCE
        );
    }

    public String getName() {
        return "gender";
    }

    @Override
    protected boolean registerUnderJavaType() {
        return true;
    }
}
//end::basic-enums-custom-type-example[]
