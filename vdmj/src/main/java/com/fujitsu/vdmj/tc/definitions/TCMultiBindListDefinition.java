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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.definitions;

import java.util.HashMap;
import java.util.Map;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.visitors.TCDefinitionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBindList;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.Pass;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Utils;

/**
 * A class to hold a multiple bind list definition.
 */
public class TCMultiBindListDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCMultipleBindList bindings;
	public TCDefinitionList defs = null;

	public TCMultiBindListDefinition(LexLocation location, TCMultipleBindList bindings)
	{
		super(Pass.DEFS, location, null, null);
		this.bindings = bindings;
	}

	@Override
	public String toString()
	{
		return "def " + Utils.listToString(bindings);
	}
	
	@Override
	public String kind()
	{
		return "multibind";
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof TCMultiBindListDefinition)
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
	public void typeCheck(Environment base, NameScope scope)
	{
		TCDefinitionList deflist = new TCDefinitionList();

		for (TCMultipleBind mb: bindings)
		{
			TCType type = mb.typeCheck(base, scope);
			deflist.addAll(mb.getDefinitions(type, scope));
		}

		/**
		 * Multiple definitions can bind the same name, so we have to do a pass
		 * to possibly combine them into unions. Note that TCDefinitions are
		 * compared for equality using their name only!
		 */

		Map<TCDefinition, TCTypeSet> map = new HashMap<TCDefinition, TCTypeSet>();
		
		for (TCDefinition def: deflist)
		{
			if (map.containsKey(def))
			{
				TCTypeSet set = map.get(def);
				set.add(def.getType());
			}
			else
			{
				map.put(def, new TCTypeSet(def.getType()));
			}
		}

		defs = new TCDefinitionList();

		for (TCDefinition def: map.keySet())
		{
			TCTypeSet set = map.get(def);
			
			if (set.size() == 1)
			{
				defs.add(def);	// Avoid unnecessary def/type copying
			}
			else
			{
				// The types that are bound to the same variable must all be compatible,
				// otherwise no values can be bound.
				
				for (TCType t1: set)
				{
					for (TCType t2: set)
					{
						if (!TypeComparator.compatible(t1, t2))
						{
							report(3322, "Duplicate patterns bind to different types");
							detail2(def.name.toString(), t1, def.name.toString(), t2);
						}
					}
				}
				
				defs.add(new TCLocalDefinition(location, def.name, set.getType(location)));
			}
		}
		
		defs.typeCheck(base, scope);
	}

	@Override
	public TCDefinition findName(TCNameToken sought, NameScope incState)
	{
		if (defs != null)
		{
			TCDefinition def = defs.findName(sought, incState);

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

		for (TCDefinition def: defs)
		{
			types.add(def.getType());
		}
		
		if (types.size() == 1)
		{
			return types.iterator().next();
		}
		else
		{
			return new TCUnionType(location, types);
		}
	}

	@Override
	public void unusedCheck()
	{
		if (defs != null)
		{
			defs.unusedCheck();
		}
	}

	@Override
	public TCDefinitionList getDefinitions()
	{
		return defs == null ? new TCDefinitionList() : defs;
	}

	@Override
	public <R, S> R apply(TCDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMultiBindListDefinition(this, arg);
	}
}
