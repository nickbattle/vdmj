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

package com.fujitsu.vdmj.po.annotations;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.fujitsu.vdmj.mapper.MappingOptional;
import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.definitions.POClassDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.modules.POModule;
import com.fujitsu.vdmj.po.statements.POStatement;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;

public abstract class POAnnotation extends PONode implements MappingOptional
{
	private static final long serialVersionUID = 1L;

	public final TCIdentifierToken name;
	public final POExpressionList args;
	
	private static final Set<Class<? extends POAnnotation>> declared = new HashSet<Class<? extends POAnnotation>>(); 
	private static final List<POAnnotation> instances = new Vector<POAnnotation>(); 

	public POAnnotation(TCIdentifierToken name, POExpressionList args)
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
		
		for (POAnnotation annotation: instances)
		{
			annotation.doInit(null);
		}
	}
	
	public static void doInit()
	{
		// Nothing by default
	}

	public void doInit(Object none)
	{
		// Nothing by default, and nothing to pass
	}

	public static List<POAnnotation> getInstances(Class<?> type)
	{
		List<POAnnotation> found = new Vector<POAnnotation>();
		
		for (POAnnotation instance: instances)
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

	public ProofObligationList poBefore(PODefinition def, POContextStack ctxt)
	{
		return new ProofObligationList();
	}

	public ProofObligationList poBefore(POStatement stmt, POContextStack ctxt)
	{
		return new ProofObligationList();
	}

	public ProofObligationList poBefore(POExpression exp, POContextStack ctxt)
	{
		return new ProofObligationList();
	}

	public ProofObligationList poBefore(POModule module)
	{
		return new ProofObligationList();
	}

	public ProofObligationList poBefore(POClassDefinition clazz)
	{
		return new ProofObligationList();
	}

	public void poAfter(PODefinition def, ProofObligationList obligations, POContextStack ctxt)
	{
		return;
	}

	public void poAfter(POStatement stmt, ProofObligationList obligations, POContextStack ctxt)
	{
		return;
	}

	public void poAfter(POExpression exp, ProofObligationList obligations, POContextStack ctxt)
	{
		return;
	}

	public void poAfter(POModule module, ProofObligationList obligations)
	{
		return;
	}

	public void poAfter(POClassDefinition clazz, ProofObligationList obligations)
	{
		return;
	}
	
	public static void close()
	{
		for (POAnnotation annotation: instances)
		{
			annotation.doClose();
		}
	}
	
	public void doClose()
	{
		// Nothing by default
	}
}
