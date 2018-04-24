package org.hibernate.userguide.mapping.basic;

import java.util.BitSet;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.DiscriminatorType;
import org.hibernate.type.descriptor.sql.spi.VarcharSqlDescriptor;

/**
 * @author Vlad Mihalcea
 */
//tag::basic-custom-type-BitSetTypeImpl-example[]
public class BitSetTypeImpl
        extends BasicTypeImpl<BitSet>
        implements DiscriminatorType<BitSet> {

    public static final BitSetTypeImpl INSTANCE = new BitSetTypeImpl();

    public BitSetTypeImpl() {
        super( VarcharSqlDescriptor.INSTANCE, BitSetTypeDescriptor.INSTANCE );
    }

    @Override
    public BitSet stringToObject(String xml) throws Exception {
        return fromString( xml );
    }

    @Override
    public String objectToSQLString(BitSet value, Dialect dialect) throws Exception {
        return toString( value );
    }

    @Override
    public String getName() {
        return "bitset";
    }

}
//end::basic-custom-type-BitSetTypeImpl-example[]
