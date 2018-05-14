/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.db2;

import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.descriptor.spi.ValueBinder;
import org.hibernate.type.descriptor.spi.ValueExtractor;
import org.hibernate.type.descriptor.spi.WrapperOptions;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.BasicBinder;
import org.hibernate.type.descriptor.sql.spi.BasicExtractor;
import org.hibernate.type.descriptor.sql.spi.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.codec.db.db2.Db2ClobDecoder;
import org.geolatte.geom.codec.db.db2.Db2ClobEncoder;

/**
 * Type Descriptor for the DB2 Geometry type (as Clob)
 * <p>
 * Created by Karel Maesen, Geovise BVBA, and David Adler, Adtech Geospatial
 */
public class DB2GeometryTypeDescriptor implements SqlTypeDescriptor {


	private final Integer srid;

	public DB2GeometryTypeDescriptor(Integer srid) {
		this.srid = srid;
	}

	@Override
	public int getSqlType() {
		return Types.CLOB;
	}

	@Override
	public boolean canBeRemapped() {
		return false;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {

		return new BasicBinder<X>( javaTypeDescriptor, this ) {
			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				st.setObject( index, toText( value, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {

				st.setObject( name, toText( value, options ) );
			}

			private String toText(X value, WrapperOptions options) {
				final Geometry<?> geometry = getJavaDescriptor().unwrap( value, Geometry.class, options );
				final Db2ClobEncoder encoder = new Db2ClobEncoder();
				String encoded = encoder.encode( geometry );
				return encoded;
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {

			@Override
			protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
				return getJavaDescriptor().wrap( toGeometry( rs.getObject( name ) ), options );
			}

			@Override
			protected X doExtract(ResultSet rs, int position, WrapperOptions options) throws SQLException {
				return getJavaDescriptor().wrap( toGeometry( rs.getObject( position ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getJavaDescriptor().wrap( toGeometry( statement.getObject( index ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getJavaDescriptor().wrap( toGeometry( statement.getObject( name ) ), options );
			}
		};
	}

	public Geometry<?> toGeometry(Object object) {
		if ( object == null ) {
			return null;
		}

		if ( object instanceof Clob ) {
			Db2ClobDecoder decoder = new Db2ClobDecoder( srid );
			return decoder.decode( (Clob) object );
		}

		throw new IllegalStateException( "Object of type " + object.getClass()
				.getCanonicalName() + " not handled by DB2 as spatial value" );
	}


	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
		return null;
	}
}
