package org.hibernate.test.type.contributor;

import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.descriptor.sql.spi.VarcharSqlDescriptor;

/**
 * @author Vlad Mihalcea
 */
public class ArrayType
        extends BasicTypeImpl<Array> {

    public static final ArrayType INSTANCE = new ArrayType();

    public ArrayType() {
        super( VarcharSqlDescriptor.INSTANCE, ArrayTypeDescriptor.INSTANCE );
    }

    @Override
    public String getName() {
        return "comma-separated-array";
    }

}
