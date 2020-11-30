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

package com.fujitsu.vdmj.ast.types;

import java.io.Serializable;
import com.fujitsu.vdmj.ast.ASTNode;
import com.fujitsu.vdmj.ast.types.visitors.ASTTypeVisitor;
import com.fujitsu.vdmj.lex.LexLocation;

/**
 * The parent class of all static type checking types.
 */
public abstract class ASTType extends ASTNode implements Comparable<ASTType>, Serializable
{
	private static final long serialVersionUID = 1L;

	/** The location of the type definition. */
	public final LexLocation location;

	/**
	 * Create a new type at the given location.
	 *
	 * @param location
	 */
	public ASTType(LexLocation location)
	{
		this.location = location;
	}

	abstract protected String toDisplay();

	/** A flag to prevent recursive types from failing toString(). */
	private boolean inToString = false;

	/**
	 * Note that this is synchronized so that multiple threads calling
	 * toString will both get the same string, not "...". This causes
	 * problems with VDM-RT trace logs which are threaded, and use
	 * this method for operation names.
	 */

	@Override
	public synchronized String toString()
	{
		if (inToString)
		{
			return "...";
		}
		else
		{
			inToString = true;
		}

		String s = toDisplay();
		inToString = false;
		return s;
	}

	/**
	 * The type with expanded detail, in the case of record types.
	 *
	 * @return The detailed type string.
	 */

	public String toDetailedString()
	{
		return toString();
	}

	@Override
	public boolean equals(Object other)
	{
		return this.getClass() == other.getClass();
	}

	@Override
	public int compareTo(ASTType o)
	{
		// This is used by the TreeSet to do inserts, not equals!!
		return toString().compareTo(o.toString());
	}

	@Override
	public int hashCode()
	{
		return getClass().hashCode();
	}
	
	/**
	 * Implemented by all types to allow visitor processing.
	 */
	abstract public <R, S> R apply(ASTTypeVisitor<R, S> visitor, S arg);
}
