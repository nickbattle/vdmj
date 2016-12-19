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

public class ASTSetType extends ASTType
{
	private static final long serialVersionUID = 1L;
	public final ASTType setof;

	public ASTSetType(LexLocation location, ASTType type)
	{
		super(location);
		this.setof = type;
	}

	@Override
	public String toDisplay()
	{
		return "set of (" + setof + ")";
	}

	@Override
	public boolean equals(Object other)
	{
		if (other.getClass().equals(ASTSetType.class))
		{
			ASTSetType os = (ASTSetType)other;
			return setof.equals(os.setof);
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return setof.hashCode();
	}
}
