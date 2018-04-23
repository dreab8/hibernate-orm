package org.hibernate.test.type.contributor;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.DiscriminatorType;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.VarcharSqlDescriptor;
import org.hibernate.type.internal.BasicTypeImpl;

/**
 * @author Vlad Mihalcea
 */
public class ArrayType
        extends BasicTypeImpl<Array>
        implements DiscriminatorType<Array> {

    public static final ArrayType INSTANCE = new ArrayType();

    public ArrayType() {
        super( ArrayTypeDescriptor.INSTANCE, VarcharSqlDescriptor.INSTANCE, );
    }

    @Override
    public Array stringToObject(String xml) throws Exception {
        return fromString( xml );
    }

    @Override
    public String objectToSQLString(Array value, Dialect dialect) throws Exception {
        return toString( value );
    }

}
