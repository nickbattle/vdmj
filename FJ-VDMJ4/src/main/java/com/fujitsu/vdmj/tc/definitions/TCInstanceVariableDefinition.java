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

import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCUndefinedExpression;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.PrivateClassEnvironment;
import com.fujitsu.vdmj.typechecker.TypeCheckException;

/**
 * A class to represent instance variable definitions.
 */
public class TCInstanceVariableDefinition extends TCAssignmentDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken oldname;
	public boolean initialized;

	public TCInstanceVariableDefinition(TCAccessSpecifier accessSpecifier, TCNameToken name,
		TCType type, TCExpression expression)
	{
		super(accessSpecifier, name, type, expression, NameScope.VARSANDSTATE);	// State and varstate
		oldname = name.getOldName();
		initialized = !(expression instanceof TCUndefinedExpression);
	}

	@Override
	public boolean isInstanceVariable()
	{
		return true;
	}

	@Override
	public void typeResolve(Environment env)
	{
		try
		{
			type = type.typeResolve(env, null);
		}
		catch (TypeCheckException e)
		{
			type.unResolve();
			throw e;
		}
	}

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		if (expression instanceof TCUndefinedExpression)
		{
			if (accessSpecifier.isStatic)
			{
				report(3037, "Static instance variable is not initialized: " + name);
			}
		}

		// Initializers can reference class members, so create a new env.
		// We set the type qualifier to unknown so that type-based name
		// resolution will succeed.

		Environment cenv = new PrivateClassEnvironment(classDefinition, base);
		
		if (this.isStatic())
		{
			FlatCheckedEnvironment checked = new FlatCheckedEnvironment(new TCDefinitionList(), base, NameScope.NAMES);
			checked.setStatic(true);
			cenv = checked;
		}
		
		super.typeCheck(cenv, NameScope.NAMESANDSTATE);
	}

	public void initializedCheck()
	{
		if (!initialized && !accessSpecifier.isStatic)
		{
			warning(5001, "Instance variable '" + name + "' is not initialized");
		}
	}

	@Override
	public TCDefinition findName(TCNameToken sought, NameScope scope)
	{
		TCDefinition found = super.findName(sought, scope);
		if (found != null) return found;
		return scope.matches(NameScope.OLDSTATE) &&
				oldname.equals(sought) ? this : null;
	}
	
	@Override
	public String kind()
	{
		return "instance variable";
	}

	@Override
	public TCNameSet getFreeVariables(Environment globals, Environment env, AtomicBoolean returns)
	{
		TCNameSet names = new TCNameSet();
		names.addAll(type.getFreeVariables(env));
		names.addAll(expression.getFreeVariables(globals, env));
		return names;
	}
}
