/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
 *
 *	Author: Nick Battle
 *
 *	This file is part of VDMJ.
 *
 *	VDMJ is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	VDMJ is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package com.fujitsu.vdmj.ast.definitions;

import java.io.Serializable;

import com.fujitsu.vdmj.ast.ASTNode;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.lex.LexLocation;

/**
 * The abstract parent of all definitions. A definition can represent a data
 * type, a value (constant), implicit or explicit functions, implicit or
 * explicit operations, module state, as well as various sorts of local variable
 * definition.
 */
public abstract class ASTDefinition extends ASTNode implements Serializable, Comparable<ASTDefinition>
{
	private static final long serialVersionUID = 1L;

	/** The textual location of the definition */
	public final LexLocation location;
	/** The name of the object being defined. */
	public final LexNameToken name;

	/** A public/private/protected/static specifier, if any. */
	public ASTAccessSpecifier accessSpecifier = null;
	/** A pointer to the enclosing class definition, if any. */
	public ASTClassDefinition classDefinition = null;

	/**
	 * Create a new definition of a particular name and location.
	 */
	public ASTDefinition(LexLocation location, LexNameToken name)
	{
		this.location = location;
		this.name = name;
		this.accessSpecifier = ASTAccessSpecifier.DEFAULT;
	}

	@Override
	abstract public String toString();

	@Override
	public boolean equals(Object other)		// Used for sets of definitions.
	{
		if (other instanceof ASTDefinition)
		{
			ASTDefinition odef = (ASTDefinition)other;
			return name != null && odef.name != null && name.equals(odef.name);
		}

		return false;
	}
	
	@Override
	public int compareTo(ASTDefinition o)
	{
		return name == null ? 0 : name.compareTo(o.name); 
	};

	@Override
	public int hashCode()
	{
		return name.hashCode();		// Used for sets of definitions (see equals).
	}

	/** A string with the informal kind of the definition, like "operation". */
	abstract public String kind();

	/**
	 * Set the definition's POAccessSpecifier. This is used in VDM++ definitions
	 * to hold  static and public/protected/private settings.
	 *
	 * @param access The POAccessSpecifier to set.
	 */
	public void setAccessSpecifier(ASTAccessSpecifier access)
	{
		accessSpecifier = access;
	}
}
