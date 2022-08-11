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

package com.fujitsu.vdmj.runtime;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.definitions.INDefinitionList;
import com.fujitsu.vdmj.in.definitions.INExplicitFunctionDefinition;
import com.fujitsu.vdmj.in.definitions.INExplicitOperationDefinition;
import com.fujitsu.vdmj.in.definitions.INImplicitFunctionDefinition;
import com.fujitsu.vdmj.in.definitions.INImplicitOperationDefinition;
import com.fujitsu.vdmj.in.patterns.INIdentifierPattern;
import com.fujitsu.vdmj.in.patterns.INPattern;
import com.fujitsu.vdmj.in.patterns.INPatternList;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.Value;

public class Delegate implements Serializable
{
	private static final long serialVersionUID = 1L;
	private final String name;
	private INDefinitionList definitions;

	public Delegate(String name, INDefinitionList definitions)
	{
		this.name = name;
		this.definitions = definitions;
	}

	private boolean delegateChecked = false;
	private Class<?> delegateClass = null;
	private Map<String, Method> delegateMethods = null;
	private Map<String, TCNameList> delegateArgs = null;
	
	// Names of old VDMJ classes that moved to stdlib
	private static String[] stdlibNames = { "CSV", "IO", "MATH", "VDMUtil" };

	public boolean hasDelegate()
	{
		if (!delegateChecked)
		{
			delegateChecked = true;

			try
			{
				String classname = name.replace('_', '.');
				delegateClass = this.getClass().getClassLoader().loadClass(classname);
				delegateMethods = new HashMap<String, Method>();
				delegateArgs = new HashMap<String, TCNameList>();
			}
			catch (ClassNotFoundException e)
			{
				if (Arrays.asList(stdlibNames).contains(name))
				{
					System.err.println("NOTE: include stdlib.jar in classpath to access " + name);
				}
			}
		}

		return (delegateClass != null);
	}

	public Object newInstance()
	{
		try
		{
			return delegateClass.getDeclaredConstructor().newInstance();
		}
		catch (NullPointerException e)
		{
			throw new InternalException(63,
				"No delegate class found: " + name);
		}
		catch (InstantiationException e)
		{
			throw new InternalException(54,
				"Cannot instantiate native object: " + e.getMessage());
		}
		catch (InvocationTargetException e)
		{
			throw new InternalException(54,
				"Cannot instantiate native object: " + e.getMessage());
		}
		catch (NoSuchMethodException e)
		{
			throw new InternalException(54,
				"Cannot instantiate native object: " + e.getMessage());
		}
		catch (IllegalAccessException e)
		{
			throw new InternalException(55,
				"Cannot access native object: " + e.getMessage());
		}
	}

	private Method getDelegateMethod(String title)
	{
		Method m = delegateMethods.get(title);

		if (m == null)
		{
			INPatternList plist = null;
			String mname = title.substring(0, title.indexOf('('));

			for (INDefinition d: definitions)
			{
				if (d.name != null && d.name.getName().equals(mname))
				{
					plist = null;
					
    	 			if (d.isOperation())
    	 			{
    	 				if (d instanceof INExplicitOperationDefinition)
    	 				{
    	 					INExplicitOperationDefinition e = (INExplicitOperationDefinition)d;
    	 					plist = e.parameterPatterns;
    	 				}
    	 				else if (d instanceof INImplicitOperationDefinition)
    	 				{
    	 					INImplicitOperationDefinition e = (INImplicitOperationDefinition)d;
    	 					plist = e.getParamPatternList();
    	 				}
    	 			}
    	 			else if (d.isFunction())
    	 			{
    	 				if (d instanceof INExplicitFunctionDefinition)
    	 				{
    	 					INExplicitFunctionDefinition e = (INExplicitFunctionDefinition)d;
    	 					plist = e.paramPatternList.get(0);
    	 				}
    	 				else if (d instanceof INImplicitFunctionDefinition)
    	 				{
    	 					INImplicitFunctionDefinition e = (INImplicitFunctionDefinition)d;
    	 					plist = e.getParamPatternList().get(0);
    	 				}
    	 			}
    	 			
    	 			if (toTitle(mname, plist).equals(title))
    	 			{
    	 				break;
    	 			}
				}
			}

			TCNameList anames = new TCNameList();
			List<Class<?>> ptypes = new Vector<Class<?>>();

			if (plist != null)
			{
				for (INPattern p: plist)
				{
					if (p instanceof INIdentifierPattern)
					{
						INIdentifierPattern ip = (INIdentifierPattern)p;
						anames.add(ip.name);
						ptypes.add(Value.class);
					}
					else
					{
						throw new InternalException(56,
							"Native method must use identifier parameters: " + title);
					}
				}

				delegateArgs.put(title, anames);
			}
			else
			{
				throw new InternalException(57, "Native member not found: " + title);
			}

			try
			{
				Class<?>[] array = new Class<?>[0];
				m = delegateClass.getMethod(mname, ptypes.toArray(array));

				if (!m.getReturnType().equals(Value.class))
				{
					throw new InternalException(58,
						"Native method does not return Value: " + m);
				}
			}
			catch (SecurityException e)
			{
				throw new InternalException(60,
					"Cannot access native method: " + e.getMessage());
			}
			catch (NoSuchMethodException e)
			{
				throw new InternalException(61,
					"Cannot find native method: " + e.getMessage());
			}

			delegateMethods.put(title, m);
		}

		return m;
	}

	public Value invokeDelegate(Object delegateObject, Context ctxt, Token section)
	{
		Method m = getDelegateMethod(ctxt.title);

		if ((m.getModifiers() & Modifier.STATIC) == 0 &&
			delegateObject == null)
		{
			throw new InternalException(64,
				"Native method should be static: " + m.getName());
		}
		
		if (section == Token.FUNCTIONS && m.getAnnotation(VDMOperation.class) != null)
		{
			throw new InternalException(72,
					"Native method marked as @VDMOperation: " + m.getName());
		}
		else if (section == Token.OPERATIONS && m.getAnnotation(VDMFunction.class) != null)
		{
			throw new InternalException(71,
					"Native method marked as @VDMFunction: " + m.getName());
		}

		TCNameList anames = delegateArgs.get(ctxt.title);
		Object[] avals = new Object[anames.size()];
		int a = 0;

		for (TCNameToken arg: anames)
		{
			avals[a++] = ctxt.get(arg);
		}

		try
		{
			return (Value)m.invoke(delegateObject, avals);
		}
		catch (IllegalArgumentException e)
		{
			throw new InternalException(62,
				"Cannot invoke native method: " + e.getMessage());
		}
		catch (IllegalAccessException e)
		{
			throw new InternalException(62,
				"Cannot invoke native method: " + e.getMessage());
		}
		catch (InvocationTargetException e)
		{
			if (e.getTargetException() instanceof ExitException)
			{
				throw (ExitException) e.getTargetException();
			}
			else if (e.getTargetException() instanceof ContextException)
			{
				throw (ContextException) e.getTargetException();
			}
			else
			{
				throw new InternalException(59,
					"Failed in native method: " + e.getTargetException().getMessage());
			}
		}
	}

	/**
	 * The Method objects in the delegateMethods map cannot be serialized,
	 * which means that deep copies fail. So here, we clear the map when
	 * serialization occurs. The map is re-build later on demand.
	 */
	private void writeObject(java.io.ObjectOutputStream out)
		throws IOException
	{
		if (delegateMethods != null)
		{
			delegateMethods.clear();
		}

		out.defaultWriteObject();
	}

	private String toTitle(String mname, INPatternList paramPatterns)
	{
		return mname + Utils.listToString("(", paramPatterns, ", ", ")");
	}
}
