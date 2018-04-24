package org.hibernate.userguide.mapping.basic;

import java.util.BitSet;

import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.descriptor.sql.spi.VarcharSqlDescriptor;

/**
 * @author Vlad Mihalcea
 */
//tag::basic-custom-type-BitSetType-example[]
public class BitSetType
        extends BasicTypeImpl<BitSet> {
    public static final BitSetType INSTANCE = new BitSetType();

    public BitSetType() {
        super( VarcharSqlDescriptor.INSTANCE, BitSetTypeDescriptor.INSTANCE );
    }

    @Override
    public String getName() {
        return "bitset";
    }

}
//end::basic-custom-type-BitSetType-example[]
