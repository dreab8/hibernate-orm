/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.collections.type;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Date;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.StringTypeImpl;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.LiteralType;
import org.hibernate.type.VersionType;
import org.hibernate.type.descriptor.java.internal.JdbcTimestampJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.BigIntSqlDescriptor;

/**
 * @author Vlad Mihalcea
 */
//tag::collections-map-custom-key-type-mapping-example[]

public class TimestampEpochTypeImpl
        extends BasicTypeImpl<Date>
        implements VersionType<Date>, LiteralType<Date> {

    public static final TimestampEpochTypeImpl INSTANCE = new TimestampEpochTypeImpl();

    public TimestampEpochTypeImpl() {
        super(
				BigIntSqlDescriptor.INSTANCE,
				JdbcTimestampJavaDescriptor.INSTANCE
        );
    }

    @Override
    public String getName() {
        return "epoch";
    }

    @Override
    public Date next(
        Date current,
        SharedSessionContractImplementor session) {
        return seed( session );
    }

    @Override
    public Date seed(
        SharedSessionContractImplementor session) {
        return new Timestamp( System.currentTimeMillis() );
    }

    @Override
    public Comparator<Date> getComparator() {
        return getJavaTypeDescriptor().getComparator();
    }

    @Override
    public String objectToSQLString(
        Date value,
        Dialect dialect) throws Exception {
        final Timestamp ts = Timestamp.class.isInstance( value )
            ? ( Timestamp ) value
            : new Timestamp( value.getTime() );
        return StringTypeImpl.INSTANCE.objectToSQLString(
            ts.toString(), dialect
        );
    }

    @Override
    public Date fromStringValue(
        String xml) throws HibernateException {
        return fromString( xml );
    }
}
//end::collections-map-custom-key-type-mapping-example[]
