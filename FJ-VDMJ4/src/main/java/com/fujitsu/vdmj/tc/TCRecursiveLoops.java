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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionListList;
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

	public void add(TCNameToken name, TCDefinitionList defs)
	{
		TCDefinitionListList existing = get(name);
		
		if (existing == null)
		{
			TCDefinitionListList list = new TCDefinitionListList();
			list.add(defs);
			recursiveLoops.put(name, list);
		}
		else
		{
			existing.add(defs);
		}
	}

	public TCDefinitionListList get(TCNameToken name)
	{
		return recursiveLoops.get(name);
	}
	
	public List<List<String>> getCycles(TCNameToken name)
	{
		TCDefinitionListList loops = get(name);
		
		if (loops == null)
		{
			return null;
		}
		
		List<List<String>> all = new Vector<List<String>>();

		for (TCDefinitionList loop: loops)
		{
			if (loop.size() > 2)	// ie. not f calls f
			{
				List<String> calls = new Vector<String>();

				for (TCDefinition d: loop)
				{
					calls.add(d.name.getName());
				}
				
				all.add(calls);
			}
		}
		
		return all;
	}

	/**
	 * Return true if the name sought is reachable via the next set of names passed using
	 * the dependency map. The stack passed records the path taken to find a cycle.
	 */
	public Set<Stack<TCNameToken>> reachable(TCNameToken sought,
			Map<TCNameToken, TCNameSet> dependencies)
	{
		Stack<TCNameToken> stack = new Stack<TCNameToken>();
		Set<Stack<TCNameToken>> loops = new HashSet<Stack<TCNameToken>>();
		stack.push(sought);

		reachable(sought, dependencies.get(sought), dependencies, stack, loops);
		
		return loops;
	}

	private boolean reachable(TCNameToken sought, TCNameSet nextset,
		Map<TCNameToken, TCNameSet> dependencies, Stack<TCNameToken> stack,
		Set<Stack<TCNameToken>> loops)
	{
		if (nextset == null)
		{
			return false;
		}
		
		if (nextset.contains(sought))
		{
			stack.push(sought);
			Stack<TCNameToken> loop = new Stack<TCNameToken>();
			loop.addAll(stack);
			loops.add(loop);
			return true;
		}
		
		boolean found = false;
		
		for (TCNameToken nextname: nextset)
		{
			if (stack.contains(nextname))	// Been here before!
			{
				return false;
			}
			
			stack.push(nextname);
			
			if (reachable(sought, dependencies.get(nextname), dependencies, stack, loops))
			{
				Stack<TCNameToken> loop = new Stack<TCNameToken>();
				loop.addAll(stack);
				loops.add(loop);
				
				while (!stack.peek().equals(nextname))
				{
					stack.pop();
				}
				
				found = true;
			}
			
			stack.pop();
		}
		
		return found;
	}
}
