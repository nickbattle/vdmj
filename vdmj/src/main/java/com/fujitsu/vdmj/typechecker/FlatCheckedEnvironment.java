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

package com.fujitsu.vdmj.typechecker;

import com.fujitsu.vdmj.tc.definitions.TCAccessSpecifier;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;

/**
 * Define the type checking environment for a list of definitions, including
 * a check for duplicates and name hiding.
 */
public class FlatCheckedEnvironment extends FlatEnvironment
{
	private boolean isStatic = false;

	public FlatCheckedEnvironment(
		TCDefinitionList definitions, NameScope scope)
	{
		super(definitions);
		dupHideCheck(definitions, scope);
	}

	public FlatCheckedEnvironment(
		TCDefinitionList definitions, Environment env, NameScope scope)
	{
		super(definitions, env);
		dupHideCheck(definitions, scope);
		setStatic(env.isStatic());
	}

	public FlatCheckedEnvironment(
		TCDefinition one, Environment env, NameScope scope)
	{
		super(one, env);
		dupHideCheck(definitions, scope);
		setStatic(env.isStatic());
	}

	public void setStatic(TCAccessSpecifier access)
	{
		isStatic = access.isStatic;
	}

	public void setStatic(boolean access)
	{
		isStatic = access;
	}

	@Override
	public boolean isStatic()
	{
		return isStatic;
	}
}
