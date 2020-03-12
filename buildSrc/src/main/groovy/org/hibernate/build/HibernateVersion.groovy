/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.build

import org.gradle.api.GradleException

class HibernateVersion {
	final String fullName
	final String majorVersion
	final int microVersion
	final String family

	final String osgiVersion

	final boolean isSnapshot

	static HibernateVersion fromProperties(File file){
		return readVersion( file )
	}

	HibernateVersion(String fullName) {
		this.fullName = fullName

		final String[] hibernateVersionComponents = fullName.split( '\\.' )
		this.majorVersion = hibernateVersionComponents[0]
		this.family = hibernateVersionComponents[0] + '.' + hibernateVersionComponents[1]

		this.isSnapshot = fullName.endsWith( '-SNAPSHOT' )
		if ( this.isSnapshot ) {
			this.microVersion = hibernateVersionComponents[2].replace( "-SNAPSHOT", "" ).toInteger()
		}
		else {
			this.microVersion = hibernateVersionComponents[2].toInteger(  )
		}

		this.osgiVersion = isSnapshot ? family + '.' + hibernateVersionComponents[2] + '.SNAPSHOT' : fullName
		System.out.println( ">>>>>>>> family ${this.family}" )
		System.out.println( ">>>>>>>> micro ${this.microVersion}" )
	}

	@Override
	String toString() {
		return this.fullName
	}

	private static HibernateVersion readVersion(File file) {
		Properties versionProperties = readVersionProperties(file)
		return  new HibernateVersion(versionProperties.ormversion)

	}

	private static Properties readVersionProperties(File file) {
		if ( !file.exists() ) {
			throw new GradleException( "Version file $file.canonicalPath does not exists" )
		}
		Properties versionProperties = new Properties()
		file.withInputStream {
			stream -> versionProperties.load( stream )
		}
		return versionProperties
	}
}
