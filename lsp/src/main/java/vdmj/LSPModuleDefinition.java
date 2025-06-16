/*******************************************************************************
 *
 *	Copyright (c) 2021 Nick Battle.
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

package vdmj;

import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.visitors.TCDefinitionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.Pass;

/**
 * A dummy TCDefinition to return from a F12 navigation if the name selected
 * is a module name. Modules don't have real definitions (unlike classes). 
 */
public class LSPModuleDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;

	public LSPModuleDefinition(TCModule module)
	{
		super(Pass.DEFS,
			module.name.getLocation(),
			new TCNameToken(module.name.getLocation(), module.name.getName(), ""),
			NameScope.ANYTHING);
	}

	@Override
	public String toString()
	{
		return "module " + name;
	}

	@Override
	public String kind()
	{
		return "module";
	}

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		// ignore
	}

	@Override
	public TCDefinitionList getDefinitions()
	{
		return null;
	}

	@Override
	public TCType getType()
	{
		return null;
	}

	@Override
	public <R, S> R apply(TCDefinitionVisitor<R, S> visitor, S arg)
	{
		return null;
	}
}
