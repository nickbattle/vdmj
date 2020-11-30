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

package com.fujitsu.vdmj.typechecker;

import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionSet;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

/**
 * Define the type checking environment for a list of definitions.
 */

public class FlatEnvironment extends Environment
{
	protected final TCDefinitionList definitions;
	private boolean limitStateScope = false;

	public FlatEnvironment(TCDefinitionList definitions)
	{
		super(null);
		this.definitions = definitions;
	}

	public FlatEnvironment(TCDefinitionList definitions, Environment env)
	{
		super(env);
		this.definitions = definitions;
	}

	public FlatEnvironment(TCDefinition one, Environment env)
	{
		super(env);
		this.definitions = new TCDefinitionList(one);
	}

	public FlatEnvironment(Environment env, Boolean functional, Boolean errors)
	{
		this(new TCDefinitionList(), env);
		setFunctional(functional, errors);
	}

	public void add(TCDefinition one)
	{
		definitions.add(one);
	}

	@Override
	public TCDefinition findName(TCNameToken name, NameScope scope)
	{
		TCDefinition def = definitions.findName(name, scope);

		if (def != null && !def.excluded)
		{
			return def;
		}

		if (outer == null)
		{
			return null;
		}
		else
		{
			if (limitStateScope)
			{
				scope = NameScope.NAMES;	// Limit NAMESAND(ANY)STATE
			}

			return outer.findName(name, scope);
		}
	}

	@Override
	public TCDefinition findType(TCNameToken name, String fromModule)
	{
		TCDefinition def = definitions.findType(name, fromModule);

		if (def != null)
		{
			return def;
		}

		return (outer == null) ? null : outer.findType(name, fromModule);
	}

	@Override
	public TCStateDefinition findStateDefinition()
	{
		TCStateDefinition def = definitions.findStateDefinition();

		if (def != null)
		{
			return def;
		}

   		return (outer == null) ? null : outer.findStateDefinition();
	}

	@Override
	public void unusedCheck()
	{
		definitions.unusedCheck();
	}

	@Override
	public boolean isVDMPP()
	{
		return outer == null ? false : outer.isVDMPP();
	}

	@Override
	public boolean isSystem()
	{
		return outer == null ? false : outer.isSystem();
	}

	@Override
	public TCClassDefinition findClassDefinition()
	{
		return outer == null ? null : outer.findClassDefinition();
	}

	@Override
	public boolean isStatic()
	{
		return outer == null ? false : outer.isStatic();
	}

	@Override
	public TCDefinitionSet findMatches(TCNameToken name)
	{
		TCDefinitionSet defs = definitions.findMatches(name);

		if (outer != null)
		{
			defs.addAll(outer.findMatches(name));
		}

		return defs;
	}

	@Override
    public void markUsed()
    {
		definitions.markUsed();
    }

	public void setLimitStateScope(boolean limitStateScope)
	{
		this.limitStateScope = limitStateScope;
	}
}
