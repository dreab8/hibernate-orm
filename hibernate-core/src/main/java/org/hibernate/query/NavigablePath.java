/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import java.io.Serializable;

import org.hibernate.internal.util.StringHelper;

/**
 * Compound-name where each path references to a domain or mapping model-part relative to a root path.  Generally
 * this root path is an entity name or a collection-role.
 *
 * @author Steve Ebersole
 */
public class NavigablePath implements DotIdentifierSequence, Serializable {
	public static final String IDENTIFIER_MAPPER_PROPERTY = "_identifierMapper";

	private final NavigablePath parent;
	private final String fullPath;
	private final String unaliasedLocalName;
	private final String identifierForTableGroup;
	private final Integer level;

	public NavigablePath(NavigablePath parent, String navigableName) {
		this.parent = parent;
		if ( parent != null ) {
			this.level = parent.getLevel() + 1;
		}
		else {
			this.level = 0;
		}

		// the _identifierMapper is a "hidden property" on entities with composite keys.
		// concatenating it will prevent the path from correctly being used to look up
		// various things such as criteria paths and fetch profile association paths
		if ( IDENTIFIER_MAPPER_PROPERTY.equals( navigableName ) ) {
			this.fullPath = parent != null ? parent.getFullPath() : "";
			this.unaliasedLocalName = "";
			this.identifierForTableGroup = parent != null ? parent.getIdentifierForTableGroup() : "";
		}
		else {
			this.unaliasedLocalName = navigableName;
			if ( parent != null ) {
				final String parentFullPath = parent.getFullPath();
				this.fullPath = StringHelper.isEmpty( parentFullPath )
						? navigableName
						: parentFullPath + "." + navigableName;
				this.identifierForTableGroup = StringHelper.isEmpty( parent.getIdentifierForTableGroup() )
						? navigableName
						: parent.getIdentifierForTableGroup() + "." + navigableName;
			}
			else {
				this.fullPath = navigableName;
				identifierForTableGroup = navigableName;
			}
		}
	}

	public NavigablePath(String localName) {
		this( localName, null );
	}

	public NavigablePath(String rootName, String alias) {
		this.parent = null;
		this.fullPath = alias == null ? rootName : rootName + "(" + alias + ")";
		this.unaliasedLocalName = StringHelper.unqualify( rootName );
		identifierForTableGroup = rootName;
		level = 0;
	}

	public NavigablePath(NavigablePath parent, String property, String alias) {
		String navigableName = alias == null
				? property
				: property + '(' + alias + ')';

		this.parent = parent;
		if ( parent != null ) {
			this.level = parent.getLevel() + 1;
		}
		else {
			this.level = 0;
		}

		// the _identifierMapper is a "hidden property" on entities with composite keys.
		// concatenating it will prevent the path from correctly being used to look up
		// various things such as criteria paths and fetch profile association paths
		if ( IDENTIFIER_MAPPER_PROPERTY.equals( navigableName ) ) {
			this.fullPath = parent != null ? parent.getFullPath() : "";
			this.unaliasedLocalName = "";
			identifierForTableGroup = parent != null ? parent.getFullPath() : "";
		}
		else {
			this.unaliasedLocalName = property;
			if ( parent != null ) {
				final String parentFullPath = parent.getFullPath();
				this.fullPath = StringHelper.isEmpty( parentFullPath )
						? navigableName
						: parentFullPath + "." + navigableName;
				this.identifierForTableGroup = StringHelper.isEmpty( parent.getIdentifierForTableGroup() )
						? navigableName
						: parent.getIdentifierForTableGroup() + "." + property;
			}
			else {
				this.fullPath = navigableName;
				this.identifierForTableGroup = property;
			}
		}
	}

	public NavigablePath() {
		this( "" );
	}

	NavigablePath(
			NavigablePath parent,
			String fullPath,
			String unaliasedLocalName,
			String identifierForTableGroup) {
		this.parent = parent;
		if ( parent != null ) {
			this.level = parent.getLevel() + 1;
		}
		else {
			this.level = 0;
		}
		this.fullPath = fullPath;
		this.unaliasedLocalName = unaliasedLocalName;
		this.identifierForTableGroup = identifierForTableGroup;
	}

	public NavigablePath treatAs(String entityName) {
		return new TreatedNavigablePath( this, entityName );
	}

	public NavigablePath treatAs(String entityName, String alias) {
		return new TreatedNavigablePath( this, entityName, alias );
	}

	public NavigablePath append(String property) {
		return new NavigablePath( this, property );
	}

	public NavigablePath append(String property, String alias) {
		return new NavigablePath( this, property, alias );
	}

	public NavigablePath getParent() {
		return parent instanceof TreatedNavigablePath ? parent.getParent() : parent;
	}

	public NavigablePath getRealParent() {
		return parent;
	}

	public String getLocalName() {
		return parent == null ? fullPath : StringHelper.unqualify( fullPath );
	}

	public String getUnaliasedLocalName() {
		return unaliasedLocalName;
	}

	public String getFullPath() {
		return fullPath;
	}

	public String getIdentifierForTableGroup() {
		if ( parent == null ) {
			return fullPath;
		}
		return identifierForTableGroup;
	}

	public boolean isParent(NavigablePath navigablePath) {
		while ( navigablePath != null ) {
			if ( this.equals( navigablePath.getParent() ) ) {
				return true;
			}
			navigablePath = navigablePath.getParent();
		}
		return false;
	}

	public int getLevel() {
		return level;
	}

	@Override
	public String toString() {
		return fullPath;
	}

	@Override
	public int hashCode() {
		return fullPath.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if ( this == other ) {
			return true;
		}

		if ( other instanceof EntityIdentifierNavigablePath ) {
			final EntityIdentifierNavigablePath otherPath = (EntityIdentifierNavigablePath) other;
			return otherPath.equals( this );
		}

		if ( ! ( other instanceof NavigablePath ) ) {
			return false;
		}

		final NavigablePath otherPath = (NavigablePath) other;

		// todo (6.0) : checking the full paths is definitely better performance
		//		But I'm not sure it is correct in all cases.  Take cases referencing
		//		an identifier at some level - the actual EntityIdentifierNavigablePath
		//		subclass has special handling for one path using the "role name" (`"{id}"`)
		//		while the other might instead use the attribute name
//		return Objects.equals( getFullPath(), otherPath.getFullPath() );

		if ( getParent() == null ) {
			return otherPath.getParent() == null;
		}

		return getParent().equals( otherPath.getParent() )
				&& getLocalName().equals( otherPath.getLocalName() );
	}
}
