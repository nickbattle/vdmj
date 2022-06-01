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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.annotations;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.fujitsu.vdmj.mapper.MappingOptional;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public abstract class TCAnnotation extends TCNode implements MappingOptional
{
	private static final long serialVersionUID = 1L;

	public final TCIdentifierToken name;
	public final TCExpressionList args;
	
	private static final Set<Class<? extends TCAnnotation>> declared = new HashSet<Class<? extends TCAnnotation>>(); 
	private static final List<TCAnnotation> instances = new Vector<TCAnnotation>(); 

	public TCAnnotation(TCIdentifierToken name, TCExpressionList args)
	{
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

	public static void init(Environment globals)
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
		
		for (TCAnnotation annotation: instances)
		{
			annotation.doInit(globals);
		}
	}
	
	public static void doInit()
	{
		// Nothing by default
	}

	protected void doInit(Environment globals)
	{
		// Nothing by default
	}

	public static List<TCAnnotation> getInstances(Class<?> type)
	{
		List<TCAnnotation> found = new Vector<TCAnnotation>();
		
		for (TCAnnotation instance: instances)
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

	public void tcBefore(TCDefinition def, Environment env, NameScope scope)
	{
		// Do nothing
	}
	
	public void tcBefore(TCStatement stmt, Environment env, NameScope scope)
	{
		// Do nothing
	}
	
	public void tcBefore(TCExpression exp, Environment env, NameScope scope)
	{
		// Do nothing
	}

	public void tcBefore(TCModule m)
	{
		// Do nothing
	}

	public void tcBefore(TCClassDefinition clazz)
	{
		// Do nothing
	}

	public void tcAfter(TCDefinition def, TCType type, Environment env, NameScope scope)
	{
		// Do nothing
	}
	
	public void tcAfter(TCStatement stmt, TCType type, Environment env, NameScope scope)
	{
		// Do nothing
	}
	
	public void tcAfter(TCExpression exp, TCType type, Environment env, NameScope scope)
	{
		// Do nothing
	}

	public void tcAfter(TCModule m)
	{
		// Do nothing
	}

	public void tcAfter(TCClassDefinition m)
	{
		// Do nothing
	}
	
	public static void close()
	{
		for (TCAnnotation annotation: instances)
		{
			annotation.doClose();
		}
	}
	
	public void doClose()
	{
		// Nothing by default
	}
}
