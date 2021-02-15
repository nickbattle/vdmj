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

import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionSet;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

/**
 * Define the type checking environment for a set of classes, as observed
 * from the outside.
 */
public class PublicClassEnvironment extends Environment
{
	private final TCClassList classes;

	public PublicClassEnvironment(TCClassList classes)
	{
		super(null);
		this.classes = classes;
	}

	public PublicClassEnvironment(TCClassList classes, Environment env)
	{
		super(env);
		this.classes = classes;
	}

	public PublicClassEnvironment(TCClassDefinition one)
	{
		super(null);
		this.classes = new TCClassList(one);
	}

	public PublicClassEnvironment(TCClassDefinition one, Environment env)
	{
		super(env);
		this.classes = new TCClassList(one);
	}

	@Override
	public TCDefinition findName(TCNameToken name, NameScope scope)
	{
		TCDefinition def = classes.findName(name, scope);

		if (def != null && def.isAccess(Token.PUBLIC) && !def.excluded)
		{
			return def;
		}

		return (outer == null) ? null : outer.findName(name, scope);
	}

	@Override
	public TCDefinition findType(TCNameToken name, String fromModule)
	{
		TCDefinition def = classes.findType(name);

		if (def != null && def.isAccess(Token.PUBLIC))
		{
			return def;
		}

		return (outer == null) ? null : outer.findType(name, null);
	}

	@Override
	public TCDefinitionSet findMatches(TCNameToken name)
	{
		TCDefinitionSet defs = classes.findMatches(name);

		if (outer != null)
		{
			defs.addAll(outer.findMatches(name));
		}

		return defs;
	}

	@Override
	public void unusedCheck()
	{
		classes.unusedCheck();
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
		return false;	// See PrivateClassEnvironment
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
