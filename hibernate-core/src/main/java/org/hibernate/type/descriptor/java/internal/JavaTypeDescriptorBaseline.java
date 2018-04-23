/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import org.hibernate.type.descriptor.java.BigIntegerTypeDescriptor;
import org.hibernate.type.descriptor.java.BlobTypeDescriptor;
import org.hibernate.type.descriptor.java.BooleanTypeDescriptor;
import org.hibernate.type.descriptor.java.ByteArrayTypeDescriptor;
import org.hibernate.type.descriptor.java.ByteTypeDescriptor;
import org.hibernate.type.descriptor.java.CalendarTypeDescriptor;
import org.hibernate.type.descriptor.java.CharacterArrayTypeDescriptor;
import org.hibernate.type.descriptor.java.CharacterTypeDescriptor;
import org.hibernate.type.descriptor.java.ClassTypeDescriptor;
import org.hibernate.type.descriptor.java.ClobTypeDescriptor;
import org.hibernate.type.descriptor.java.CurrencyTypeDescriptor;
import org.hibernate.type.descriptor.java.DateTypeDescriptor;
import org.hibernate.type.descriptor.java.DoubleTypeDescriptor;
import org.hibernate.type.descriptor.java.DurationJavaDescriptor;
import org.hibernate.type.descriptor.java.FloatTypeDescriptor;
import org.hibernate.type.descriptor.java.InstantJavaDescriptor;
import org.hibernate.type.descriptor.java.IntegerTypeDescriptor;
import org.hibernate.type.descriptor.java.JdbcDateTypeDescriptor;
import org.hibernate.type.descriptor.java.JdbcTimeTypeDescriptor;
import org.hibernate.type.descriptor.java.JdbcTimestampTypeDescriptor;
import org.hibernate.type.descriptor.java.LocalDateJavaDescriptor;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaDescriptor;
import org.hibernate.type.descriptor.java.LocaleTypeDescriptor;
import org.hibernate.type.descriptor.java.LongTypeDescriptor;
import org.hibernate.type.descriptor.java.NClobTypeDescriptor;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaDescriptor;
import org.hibernate.type.descriptor.java.OffsetTimeJavaDescriptor;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayTypeDescriptor;
import org.hibernate.type.descriptor.java.PrimitiveCharacterArrayTypeDescriptor;
import org.hibernate.type.descriptor.java.ShortTypeDescriptor;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;
import org.hibernate.type.descriptor.java.TimeZoneTypeDescriptor;
import org.hibernate.type.descriptor.java.UUIDTypeDescriptor;
import org.hibernate.type.descriptor.java.UrlTypeDescriptor;
import org.hibernate.type.descriptor.java.ZonedDateTimeJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.Primitive;

/**
 *
 * @author Steve Ebersole
 */
public class JavaTypeDescriptorBaseline {
	public interface BaselineTarget {
		void addBaselineDescriptor(BasicJavaDescriptor descriptor);
		void addBaselineDescriptor(Class describedJavaType, BasicJavaDescriptor descriptor);
	}

	public static void prime(BaselineTarget target) {
		primePrimitive( target, ByteTypeDescriptor.INSTANCE );
		primePrimitive( target, BooleanTypeDescriptor.INSTANCE );
		primePrimitive( target, CharacterTypeDescriptor.INSTANCE );
		primePrimitive( target, ShortTypeDescriptor.INSTANCE );
		primePrimitive( target, IntegerTypeDescriptor.INSTANCE );
		primePrimitive( target, LongTypeDescriptor.INSTANCE );
		primePrimitive( target, FloatTypeDescriptor.INSTANCE );
		primePrimitive( target, DoubleTypeDescriptor.INSTANCE );

		target.addBaselineDescriptor( BigDecimalJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( BigIntegerTypeDescriptor.INSTANCE );

		target.addBaselineDescriptor( StringTypeDescriptor.INSTANCE );

		target.addBaselineDescriptor( BlobTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( ClobTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( NClobTypeDescriptor.INSTANCE );

		target.addBaselineDescriptor( ByteArrayTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( CharacterArrayTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( PrimitiveByteArrayTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( PrimitiveCharacterArrayTypeDescriptor.INSTANCE );

		target.addBaselineDescriptor( DurationJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( InstantJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( LocalDateJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( LocalDateTimeJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( OffsetDateTimeJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( OffsetTimeJavaDescriptor.INSTANCE );
		target.addBaselineDescriptor( ZonedDateTimeJavaDescriptor.INSTANCE );

		target.addBaselineDescriptor( CalendarTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( DateTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( java.sql.Date.class, JdbcDateTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( java.sql.Time.class, JdbcTimeTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( java.sql.Timestamp.class, JdbcTimestampTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( TimeZoneTypeDescriptor.INSTANCE );

		target.addBaselineDescriptor( ClassTypeDescriptor.INSTANCE );

		target.addBaselineDescriptor( CurrencyTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( LocaleTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( UrlTypeDescriptor.INSTANCE );
		target.addBaselineDescriptor( UUIDTypeDescriptor.INSTANCE );

		target.addBaselineDescriptor( new CollectionJavaDescriptor( Collection.class ) );
		target.addBaselineDescriptor( new CollectionJavaDescriptor( List.class ) );
		target.addBaselineDescriptor( new CollectionJavaDescriptor( Set.class ) );
		target.addBaselineDescriptor( new CollectionJavaDescriptor( SortedSet.class ) );
		target.addBaselineDescriptor( new CollectionJavaDescriptor( Map.class ) );
		target.addBaselineDescriptor( new CollectionJavaDescriptor( SortedMap.class ) );
		target.addBaselineDescriptor( MapEntryJavaDescriptor.INSTANCE );

	}

	private static void primePrimitive(BaselineTarget target, BasicJavaDescriptor descriptor) {
		target.addBaselineDescriptor( descriptor );
		target.addBaselineDescriptor( ( (Primitive) descriptor ).getPrimitiveClass(), descriptor );
	}
}