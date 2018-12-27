/*******************************************************************************
 *
 *	Copyright (c) 2018 Nick Battle.
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

package com.fujitsu.vdmj.tc.annotations;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public abstract class TCAnnotation
{
	public final TCIdentifierToken name;
	
	public final TCExpressionList args;
	
	private static final Set<Class<?>> declared = new HashSet<Class<?>>(); 

	public TCAnnotation(TCIdentifierToken name, TCExpressionList args)
	{
		this.name = name;
		this.args = args;
		
		declared.add(this.getClass());
	}

	public static void init()
	{
		for (Class<?> clazz: declared)
		{
			try
			{
				Method doInit = clazz.getMethod("doInit", (Class<?>[])null);
				doInit.invoke(null, (Object[])null);
			}
			catch (Throwable e)
			{
				throw new RuntimeException(clazz.getSimpleName() + ":" + e);
			}
		}
	}
	
	public static void doInit()
	{
		// Nothing by default
	}

	@Override
	public String toString()
	{
		return "@" + name + (args.isEmpty() ? "" : "(" + args + ")");
	}

	public void before(TCDefinition def, Environment env, NameScope scope)
	{
		// Do nothing
	}
	
	public void before(TCStatement stmt, Environment env, NameScope scope)
	{
		// Do nothing
	}
	
	public void before(TCExpression exp, Environment env, NameScope scope)
	{
		// Do nothing
	}

	public void before(TCModule m)
	{
		// Do nothing
	}

	public void after(TCDefinition def, TCType type, Environment env, NameScope scope)
	{
		// Do nothing
	}
	
	public void after(TCStatement stmt, TCType type, Environment env, NameScope scope)
	{
		// Do nothing
	}
	
	public void after(TCExpression exp, TCType type, Environment env, NameScope scope)
	{
		// Do nothing
	}

	public void after(TCModule m)
	{
		// Do nothing
	}
}
