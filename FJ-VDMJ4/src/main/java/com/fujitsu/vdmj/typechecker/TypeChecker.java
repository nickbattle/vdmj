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

package com.fujitsu.vdmj.typechecker;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.messages.VDMWarning;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;


/**
 * The abstract root of all type checker classes.
 */

abstract public class TypeChecker
{
	private static List<VDMError> errors = new Vector<VDMError>();
	private static List<VDMWarning> warnings = new Vector<VDMWarning>();
	private static VDMMessage lastMessage = null;
	private static final int MAX = 100;
	
	public TypeChecker()
	{
		clearErrors();
	}

	abstract public void typeCheck();
	
	/**
	 * Check for cyclic dependencies between the free variables that definitions depend
	 * on and the definition of those variables.
	 */
	protected void cyclicDependencyCheck(TCDefinitionList defs)
	{
		Map<TCNameToken, TCNameSet> dependencies = new HashMap<TCNameToken, TCNameSet>();

    	for (TCDefinition def: defs)
    	{
			TCNameSet freevars = def.getFreeVariables();
			
			for (TCNameToken name: def.getVariableNames())
			{
				dependencies.put(name.getExplicit(true), freevars);
			}
    	}
    	
		for (TCNameToken sought: dependencies.keySet())
		{
			Stack<TCNameToken> stack = new Stack<TCNameToken>();
			stack.push(sought);
			
			if (reachable(sought, dependencies.get(sought), dependencies, stack))
			{
	    		report(9999, "Cyclic dependency detected", stack.peek().getLocation());
	    		detail("Cycle", stack.toString());
			}
			
			stack.pop();
		}
	}

	/**
	 * Return true if the name sought is reachable via the next set of names passed using
	 * the dependency map. The stack passed records the path taken to find a cycle.
	 */
	private boolean reachable(TCNameToken sought, TCNameSet nextset,
		Map<TCNameToken, TCNameSet> dependencies, Stack<TCNameToken> stack)
	{
		if (nextset == null)
		{
			return false;
		}
		
		if (nextset.contains(sought))
		{
			stack.push(sought);
			return true;
		}
		
		for (TCNameToken nextname: nextset)
		{
			if (stack.contains(nextname))	// Been here before!
			{
				return false;
			}
			
			stack.push(nextname);
			
			if (reachable(sought, dependencies.get(nextname), dependencies, stack))
			{
				return true;
			}
			
			stack.pop();
		}
		
		return false;
	}

	public static void report(int number, String problem, LexLocation location)
	{
		VDMError error = new VDMError(number, problem, location);

		if (!errors.contains(error))
		{
			errors.add(error);
			lastMessage = error;

    		if (errors.size() >= MAX-1)
    		{
    			errors.add(new VDMError(10, "Too many type checking errors", location));
    			throw new InternalException(10, "Too many type checking errors");
    		}
		}
		else
		{
			lastMessage = null;
		}
	}

	public static void warning(int number, String problem, LexLocation location)
	{
		VDMWarning warning = new VDMWarning(number, problem, location);

		if (!warnings.contains(warning))
		{
			warnings.add(warning);
			lastMessage = warning;
		}
	}

	public static void detail(String tag, Object obj)
	{
		if (lastMessage != null)
		{
			lastMessage.add(tag + ": " + obj);
		}
	}

	public static void detail2(String tag1, Object obj1, String tag2, Object obj2)
	{
		detail(tag1, obj1);
		detail(tag2, obj2);
	}

	public static void clearErrors()
	{
		errors.clear();
		warnings.clear();
	}

	public static int getErrorCount()
	{
		return errors.size();
	}

	public static int getWarningCount()
	{
		return warnings.size();
	}

	public static List<VDMError> getErrors()
	{
		return errors;
	}

	public static List<VDMWarning> getWarnings()
	{
		return warnings;
	}

	public static void printErrors(PrintWriter out)
	{
		for (VDMError e: errors)
		{
			out.println(e.toString());
		}
	}

	public static void printWarnings(PrintWriter out)
	{
		for (VDMWarning w: warnings)
		{
			out.println(w.toString());
		}
	}
}
