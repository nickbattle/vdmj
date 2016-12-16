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

import com.fujitsu.vdmj.lex.LexLocation;

public class ASTMapType extends ASTType
{
	private static final long serialVersionUID = 1L;
	public final ASTType from;
	public final ASTType to;
	public final boolean empty;

	public ASTMapType(LexLocation location, ASTType from, ASTType to)
	{
		super(location);
		this.from = from;
		this.to = to;
		this.empty = false;
	}

	public ASTMapType(LexLocation location)
	{
		super(location);
		this.from = new ASTUnknownType(location);
		this.to = new ASTUnknownType(location);
		this.empty = true;
	}

	@Override
	public String toDisplay()
	{
		return "map (" + from + ") to (" + to + ")";
	}

	@Override
	public boolean equals(Object other)
	{
		if (other.getClass() == getClass())	// inmaps too
		{
			ASTMapType mt = (ASTMapType)other;
			return from.equals(mt.from) && to.equals(mt.to);
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return from.hashCode() + to.hashCode();
	}
}
