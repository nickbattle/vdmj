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

import com.fujitsu.vdmj.tc.definitions.TCBUSClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCCPUClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionSet;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.definitions.TCSystemDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

/**
 * Define the type checking environment for a class as observed from inside.
 */

public class PrivateClassEnvironment extends Environment
{
	private final TCClassDefinition classdef;

	public PrivateClassEnvironment(TCClassDefinition classdef)
	{
		this(classdef, null);
	}

	public PrivateClassEnvironment(TCClassDefinition classdef, Environment env)
	{
		super(env);
		this.classdef = classdef;
	}

	@Override
	public TCDefinition findName(TCNameToken sought, NameScope scope)
	{
		TCDefinition def = classdef.findName(sought, scope);

		if (def != null && !def.excluded)
		{
			return def;
		}

		return (outer == null) ? null : outer.findName(sought, scope);
	}

	@Override
	public TCDefinition findType(TCNameToken name, String fromModule)
	{
		TCDefinition def = classdef.findType(name, null);

		if (def != null)
		{
			return def;
		}

		return (outer == null) ? null : outer.findType(name, null);
	}

	@Override
	public TCDefinitionSet findMatches(TCNameToken name)
	{
		TCDefinitionSet defs = classdef.findMatches(name);

		if (outer != null)
		{
			defs.addAll(outer.findMatches(name));
		}

		return defs;
	}

	@Override
	public void unusedCheck()
	{
		classdef.unusedCheck();
	}

	@Override
	public TCStateDefinition findStateDefinition()
	{
		return null;
	}

	@Override
	public boolean isVDMPP()
	{
		return true;
	}

	@Override
	public boolean isSystem()
	{
		return (classdef instanceof TCSystemDefinition ||
				classdef instanceof TCCPUClassDefinition ||
				classdef instanceof TCBUSClassDefinition);
	}

	@Override
	public TCClassDefinition findClassDefinition()
	{
		return classdef;
	}

	@Override
	public boolean isStatic()
	{
		return false;
	}
}
