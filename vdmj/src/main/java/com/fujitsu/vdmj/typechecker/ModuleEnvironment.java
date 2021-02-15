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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.typechecker;

import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionSet;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.modules.TCModule;

/**
 * Define the type checking environment for a modular specification.
 */
public class ModuleEnvironment extends Environment
{
	private final TCModule module;

	public ModuleEnvironment(TCModule module)
	{
		super(null);
		this.module = module;
		dupHideCheck(module.defs, NameScope.NAMESANDSTATE);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		for (TCDefinition d: module.defs)
		{
			sb.append(d.name == null ? d : d.name);
			sb.append("\n");
		}

		return sb.toString();
	}

	@Override
	public TCDefinition findName(TCNameToken name, NameScope scope)
	{
		TCDefinition def = module.defs.findName(name, scope);

		if (def != null && !def.excluded)
		{
			return def;
		}

		def = module.importdefs.findName(name, scope);

		if (def != null)
		{
			return def;
		}

   		return null;	// Modules are always bottom of the env chain
	}

	@Override
	public TCDefinition findType(TCNameToken name, String fromModule)
	{
		TCDefinition def = module.defs.findType(name, module.name.getName());

		if (def != null)
		{
			return def;
		}

		def = module.importdefs.findType(name, module.name.getName());

		if (def != null)
		{
			return def;
		}

		return null;	// Modules are always bottom of the env chain
	}

	@Override
	public TCDefinitionSet findMatches(TCNameToken name)
	{
		TCDefinitionSet defs = module.defs.findMatches(name);
		defs.addAll(module.importdefs.findMatches(name));
		return defs;
	}

	@Override
	public void unusedCheck()
	{
		// The usage of all executableModules is checked at the end of the type check
		// phase. Only flat environments implement this check, for unused
		// local definitions introduced by expressions and statements.
	}

	@Override
	public TCStateDefinition findStateDefinition()
	{
		TCStateDefinition def = module.defs.findStateDefinition();

		if (def != null)
		{
			return def;
		}

		return null;	// Modules are always bottom of the env chain
	}

	@Override
	public boolean isVDMPP()
	{
		return false;
	}

	@Override
	public boolean isSystem()
	{
		return false;
	}

	@Override
	public TCClassDefinition findClassDefinition()
	{
		return null;
	}

	@Override
	public boolean isStatic()
	{
		return false;
	}
}
