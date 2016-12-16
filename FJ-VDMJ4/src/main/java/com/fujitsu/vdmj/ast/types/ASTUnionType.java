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
import com.fujitsu.vdmj.util.Utils;

public class ASTUnionType extends ASTType
{
	private static final long serialVersionUID = 1L;

	public final ASTTypeSet types;

	public ASTUnionType(LexLocation location, ASTType a, ASTType b)
	{
		super(location);
		types = new ASTTypeSet();
		types.add(a);
		types.add(b);
	}

	public ASTUnionType(LexLocation location, ASTTypeSet types)
	{
		super(location);
		this.types = types;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof ASTUnionType)
		{
			ASTUnionType uother = (ASTUnionType)other;

			for (ASTType t: uother.types)
			{
				if (!types.contains(t))
				{
					return false;
				}
			}

			return true;
		}

		return types.contains(other);
	}

	@Override
	public int hashCode()
	{
		return types.hashCode();
	}

	@Override
	public String toDisplay()
	{
		if (types.size() == 1)
		{
			return types.iterator().next().toString();
		}
		else
		{
			return Utils.setToString(types, " | ");
		}
	}
}
