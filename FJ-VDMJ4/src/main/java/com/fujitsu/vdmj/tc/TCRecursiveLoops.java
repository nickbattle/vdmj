/*******************************************************************************
 *
 *	Copyright (c) 2019 Nick Battle.
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
package com.fujitsu.vdmj.tc;

import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

/**
 * A class to hold static data shared by VDM-SL and VDM++/RT.
 */
public class TCRecursiveLoops extends TCNode
{
	private static final long serialVersionUID = 1L;
	private static TCRecursiveLoops INSTANCE = null;
	public TCRecursiveMap recursiveLoops = null;

	public static TCRecursiveLoops getInstance()
	{
		if (INSTANCE == null)
		{
			INSTANCE = new TCRecursiveLoops();
		}
		
		return INSTANCE;
	}
	
	public void reset()
	{
		recursiveLoops = new TCRecursiveMap();
	}

	public void put(TCNameToken sought, TCDefinitionList defs)
	{
		recursiveLoops.put(sought, defs);
	}

	public List<TCDefinition> get(TCNameToken name)
	{
		return recursiveLoops.get(name);
	}

	/**
	 * Return true if the name sought is reachable via the next set of names passed using
	 * the dependency map. The stack passed records the path taken to find a cycle.
	 */
	public static boolean reachable(TCNameToken sought, TCNameSet nextset,
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
}
