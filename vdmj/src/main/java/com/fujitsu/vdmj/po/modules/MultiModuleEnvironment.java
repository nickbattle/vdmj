/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package com.fujitsu.vdmj.po.modules;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionSet;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.ModuleEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

/**
 * An environment to allow "global" type checking, so that invariants and pre/post calls
 * can appear in POs, even if those symbols have not been imported into the module. Note
 * that this means names have to be qualified if they are not local, like Z`inv_T(...).
 */
public class MultiModuleEnvironment extends Environment
{
	private final List<ModuleEnvironment> moduleEnvs;
	
	public MultiModuleEnvironment(POModuleList modules)
	{
		super(null);
		
		moduleEnvs = new Vector<ModuleEnvironment>(modules.size());
		
		for (POModule m: modules)
		{
			moduleEnvs.add(new ModuleEnvironment(m.tcmodule));
		}
	}

	@Override
	public TCDefinition findName(TCNameToken name, NameScope scope)
	{
		for (ModuleEnvironment menv: moduleEnvs)
		{
			TCDefinition def = menv.findName(name, scope);
			
			if (def != null)
			{
				return def;
			}
		}
		
		return null;
	}

	@Override
	public TCDefinition findType(TCNameToken name, String fromModule)
	{
		for (ModuleEnvironment menv: moduleEnvs)
		{
			TCDefinition def = menv.findType(name, fromModule);
			
			if (def != null)
			{
				return def;
			}
		}
		
		return null;
	}

	@Override
	public TCStateDefinition findStateDefinition()
	{
		return null;	// Each module can have one, so...
	}

	@Override
	public TCClassDefinition findClassDefinition()
	{
		return null;	// PP/RT only
	}

	@Override
	public boolean isStatic()
	{
		return false;
	}

	@Override
	public void unusedCheck()
	{
		return;
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
	public TCDefinitionSet findMatches(TCNameToken name)
	{
		TCDefinitionSet set = new TCDefinitionSet();
		
		for (ModuleEnvironment menv: moduleEnvs)
		{
			set.addAll(menv.findMatches(name));
		}

		return set;
	}
}
