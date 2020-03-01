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

package com.fujitsu.vdmj.tc.definitions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBindList;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.Pass;
import com.fujitsu.vdmj.util.Utils;

/**
 * A class to hold a multiple bind list definition.
 */
public class TCMultiBindListDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCMultipleBindList bindings;
	private TCDefinitionList defs = null;

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
		TCDefinitionSet defset = new TCDefinitionSet();

		for (TCMultipleBind mb: bindings)
		{
			TCType type = mb.typeCheck(base, scope);
			defset.addAll(mb.getDefinitions(type, scope));
		}

		defs = new TCDefinitionList();
		defs.addAll(defset);
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
	public TCNameList getVariableNames()
	{
		return defs == null ? new TCNameList() : defs.getVariableNames();
	}

	@Override
	public <R, S> R apply(TCDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMultiBindListDefinition(this, arg);
	}
}
