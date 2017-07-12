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

import java.util.concurrent.atomic.AtomicBoolean;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.Pass;

/**
 * A class to hold a local variable definition.
 */
public class TCLocalDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;
	public TCType type;
	
	private TCValueDefinition valueDefinition = null;

	public TCLocalDefinition(LexLocation location, TCNameToken name, TCType type)
	{
		this(location, name, type, NameScope.LOCAL);
	}

	public TCLocalDefinition(LexLocation location, TCNameToken name, TCType type, NameScope scope)
	{
		super(Pass.DEFS, location, name, scope);
		this.type = type;
	}

	@Override
	public String toString()
	{
		return name + " = " + type;
	}
	
	@Override
	public String kind()
	{
		return "local";
	}

	@Override
	public void typeResolve(Environment base)
	{
   		if (type != null)
   		{
   			type = type.typeResolve(base, null);
   		}
	}

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
   		if (type != null)
   		{
   			type = type.typeResolve(base, null);
   		}
	}

	@Override
	public TCType getType()
	{
		return type == null ? new TCUnknownType(location) : type;
	}

	@Override
	public TCDefinitionList getDefinitions()
	{
		return new TCDefinitionList(this);
	}

	@Override
	public TCNameList getVariableNames()
	{
		return new TCNameList(name);
	}

	@Override
	public boolean isFunction()
	{
		// This is only true for local definitions of member functions or
		// operations, not local definitions that happen to be function values.
		// So we exclude parameter types. We also exclude value definitions.

		return (valueDefinition != null ||
			type.isType(TCParameterType.class, location)) ? false : type.isFunction(location);
	}

	@Override
	public boolean isUpdatable()
	{
		return nameScope.matches(NameScope.STATE) || getType().isClass(null);
	}

	public void setValueDefinition(TCValueDefinition def)
	{
		valueDefinition = def;
	}

	@Override
	public boolean isValueDefinition()
	{
		return valueDefinition != null;
	}

	@Override
	public TCNameSet getFreeVariables(Environment globals, Environment env, AtomicBoolean returns)
	{
		TCNameSet names = type.getFreeVariables(env);
		
		if (valueDefinition != null)
		{
			names.addAll(valueDefinition.getFreeVariables(globals, env, returns));
		}
		
		return names;
	}
}
