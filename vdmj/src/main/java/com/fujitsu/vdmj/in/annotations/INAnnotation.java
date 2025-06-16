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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.annotations;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.mapper.MappingOptional;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.values.Value;

public abstract class INAnnotation extends INNode implements MappingOptional
{
	private static final long serialVersionUID = 1L;

	public final TCIdentifierToken name;
	public final INExpressionList args;
	
	private static final Set<Class<? extends INAnnotation>> declared = new HashSet<Class<? extends INAnnotation>>(); 
	private static final List<INAnnotation> instances = new Vector<INAnnotation>();
	
	public static boolean suspended = false;

	public INAnnotation(TCIdentifierToken name, INExpressionList args)
	{
		super(name.getLocation());
		this.name = name;
		this.args = args;

		declared.add(this.getClass());
		instances.add(this);
	}
	
	public static void reset()
	{
		declared.clear();
		instances.clear();
	}
	
	public static void suspend(boolean flag)
	{
		suspended = flag;	// Used in INAnnotatedExpression and Statement
	}

	public static void init(Context ctxt)
	{
		for (Class<?> clazz: declared)
		{
			try
			{
				Method doInit = clazz.getMethod("doInit", (Class<?>[])null);
				doInit.invoke(null, (Object[])null);
			}
			catch (InvocationTargetException e)
			{
				if (e.getCause() instanceof ContextException)
				{
					throw (ContextException)e.getCause();
				}
				else
				{
					throw new RuntimeException(clazz.getSimpleName() + ": " + e.getCause());
				}
			}
			catch (Throwable e)
			{
				throw new RuntimeException(clazz.getSimpleName() + ": " + e);
			}
		}
		
		for (INAnnotation annotation: instances)
		{
			annotation.doInit(ctxt);
		}
	}
	
	public static void doInit()
	{
		// Nothing by default
	}

	protected void doInit(Context ctxt)
	{
		// Nothing by default
	}
	
	public static List<INAnnotation> getInstances(Class<?> type)
	{
		List<INAnnotation> found = new Vector<INAnnotation>();
		
		for (INAnnotation instance: instances)
		{
			if (type.isAssignableFrom(instance.getClass()))
			{
				found.add(instance);
			}
		}
		
		return found;
	}

	@Override
	public String toString()
	{
		return "@" + name + (args.isEmpty() ? "" : "(" + args + ")");
	}

	public void inBefore(INStatement stmt, Context ctxt)
	{
		// Do nothing
	}
	
	public void inBefore(INExpression exp, Context ctxt)
	{
		// Do nothing
	}

	public void inAfter(INStatement stmt, Value rv, Context ctxt)
	{
		// Do nothing
	}
	
	public void inAfter(INExpression exp, Value rv, Context ctxt)
	{
		// Do nothing
	}
}
