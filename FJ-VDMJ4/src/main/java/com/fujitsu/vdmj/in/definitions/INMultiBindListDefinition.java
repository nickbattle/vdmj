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

package com.fujitsu.vdmj.in.definitions;

import com.fujitsu.vdmj.in.patterns.INMultipleBindList;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.util.Utils;

/**
 * A class to hold a multiple bind list definition.
 */
public class INMultiBindListDefinition extends INDefinition
{
	private static final long serialVersionUID = 1L;
	public final INMultipleBindList bindings;
	public final INDefinitionList defs;

	public INMultiBindListDefinition(LexLocation location, INMultipleBindList bindings, INDefinitionList defs)
	{
		super(location, null, null);
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
		if (other instanceof INMultiBindListDefinition)
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
	public INDefinition findName(TCNameToken sought)
	{
		if (defs != null)
		{
			INDefinition def = defs.findName(sought);

			if (def != null)
			{
				return def;
			}
		}

		return null;
	}

	@Override
	public TCType getType()
	{
		TCTypeSet types = new TCTypeSet();

		for (INDefinition def: defs)
		{
			types.add(def.getType());
		}

		return new TCUnionType(location, types);
	}
}
