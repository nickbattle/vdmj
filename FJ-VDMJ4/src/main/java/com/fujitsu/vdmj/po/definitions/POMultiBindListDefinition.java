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

package com.fujitsu.vdmj.po.definitions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.patterns.POMultipleBindList;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.util.Utils;

/**
 * A class to hold a multiple bind list definition.
 */
public class POMultiBindListDefinition extends PODefinition
{
	private static final long serialVersionUID = 1L;
	public final POMultipleBindList bindings;
	public final PODefinitionList defs;

	public POMultiBindListDefinition(LexLocation location, POMultipleBindList bindings, PODefinitionList defs)
	{
		super(location, null);
		this.bindings = bindings;
		this.defs = defs;
	}

	@Override
	public String toString()
	{
		return "def " + Utils.listToString(bindings);
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof POMultiBindListDefinition)
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
	public TCType getType()
	{
		TCTypeSet types = new TCTypeSet();

		for (PODefinition def: defs)
		{
			types.add(def.getType());
		}

		return new TCUnionType(location, types);
	}

	@Override
	public TCNameList getVariableNames()
	{
		return defs == null ? new TCNameList() : defs.getVariableNames();
	}
}
