package org.hibernate.test.type.contributor;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.DiscriminatorType;
import org.hibernate.type.descriptor.sql.spi.VarcharSqlDescriptor;

/**
 * @author Vlad Mihalcea
 */
public class ArrayTypeImpl
        extends BasicTypeImpl<Array>
        implements DiscriminatorType<Array> {

    public static final ArrayTypeImpl INSTANCE = new ArrayTypeImpl();

    public ArrayTypeImpl() {
        super( VarcharSqlDescriptor.INSTANCE, ArrayTypeDescriptor.INSTANCE );
    }

    @Override
    public Array stringToObject(String xml) throws Exception {
        return fromString( xml );
    }

    @Override
    public String objectToSQLString(Array value, Dialect dialect) throws Exception {
        return toString( value );
    }

    @Override
    public String getName() {
        return "comma-separated-array";
    }

}
