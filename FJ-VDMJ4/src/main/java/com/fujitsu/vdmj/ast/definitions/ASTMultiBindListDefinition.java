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

import com.fujitsu.vdmj.ast.patterns.ASTMultipleBindList;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.util.Utils;

/**
 * A class to hold a multiple bind list definition.
 */
public class ASTMultiBindListDefinition extends ASTDefinition
{
	private static final long serialVersionUID = 1L;
	public final ASTMultipleBindList bindings;

	public ASTMultiBindListDefinition(LexLocation location, ASTMultipleBindList bindings)
	{
		super(location, null);
		this.bindings = bindings;
	}

	@Override
	public String toString()
	{
		return "def " + Utils.listToString(bindings);
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof ASTMultiBindListDefinition)
		{
			return toString().equals(other.toString());
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}

	@Override
	public String kind()
	{
		return "bind";
	}
}
