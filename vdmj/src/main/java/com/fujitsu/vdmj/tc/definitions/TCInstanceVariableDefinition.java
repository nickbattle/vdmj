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

package com.fujitsu.vdmj.tc.definitions;

import com.fujitsu.vdmj.tc.annotations.TCAnnotationList;
import com.fujitsu.vdmj.tc.definitions.visitors.TCDefinitionVisitor;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCUndefinedExpression;
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

	public TCInstanceVariableDefinition(TCAnnotationList annotations,
		TCAccessSpecifier accessSpecifier, TCNameToken name,
		TCType type, TCExpression expression)
	{
		super(accessSpecifier, name, type, expression, NameScope.VARSANDSTATE);	// State and varstate
		this.annotations = annotations;
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
			type = type.typeResolve(env);
			if (annotations != null) annotations.tcResolve(this, env);
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
		if (annotations != null) annotations.tcBefore(this, base, scope);

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
		if (annotations != null) annotations.tcAfter(this, type, base, scope);
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
	public <R, S> R apply(TCDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseInstanceVariableDefinition(this, arg);
	}
}
