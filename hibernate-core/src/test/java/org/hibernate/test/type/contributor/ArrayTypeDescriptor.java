package org.hibernate.test.type.contributor;

import java.sql.Types;
import java.util.Arrays;

import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Vlad Mihalcea
 */
public class ArrayTypeDescriptor extends AbstractBasicJavaDescriptor<Array> {

    private static final String DELIMITER = ",";

    public static final ArrayTypeDescriptor INSTANCE = new ArrayTypeDescriptor();

    public ArrayTypeDescriptor() {
        super( Array.class );
    }

    @Override
    public String toString(Array value) {
        StringBuilder builder = new StringBuilder();
        for ( String token : value ) {
            if ( builder.length() > 0 ) {
                builder.append( DELIMITER );
            }
            builder.append( token );
        }
        return builder.toString();
    }

    @Override
    public Array fromString(String string) {
        if ( string == null || string.isEmpty() ) {
            return null;
        }
        String[] tokens = string.split( DELIMITER );
        Array array = new Array();
        array.addAll( Arrays.asList(tokens) );
        return array;
    }

    @Override
    public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
        return context.getTypeConfiguration().getSqlTypeDescriptorRegistry().getDescriptor( Types.VARCHAR );
    }

    @SuppressWarnings({"unchecked"})
    public <X> X unwrap(Array value, Class<X> type, WrapperOptions options) {
        if ( value == null ) {
            return null;
        }
        if ( Array.class.isAssignableFrom( type ) ) {
            return (X) value;
        }
        if ( String.class.isAssignableFrom( type ) ) {
            return (X) toString( value);
        }
        throw unknownUnwrap( type );
    }

    public <X> Array wrap(X value, WrapperOptions options) {
        if ( value == null ) {
            return null;
        }
        if ( String.class.isInstance( value ) ) {
            return fromString( (String) value );
        }
        if ( Array.class.isInstance( value ) ) {
            return (Array) value;
        }
        throw unknownWrap( value.getClass() );
    }
}
