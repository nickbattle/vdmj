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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/
package com.fujitsu.vdmj.tc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionListList;
import com.fujitsu.vdmj.tc.expressions.TCApplyExpression;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

/**
 * A class to hold recursive loop data, which is used to detect mutual recursion and
 * missing measure functions.
 */
public class TCRecursiveFunctions
{
	private static final int LOOP_SIZE_LIMIT = 8;
	private static TCRecursiveFunctions INSTANCE = null;
	
	private static class Apply
	{
		public final TCApplyExpression apply;
		public final TCDefinition calling;
		
		public Apply(TCApplyExpression apply, TCDefinition calling)
		{
			this.apply = apply;
			this.calling = calling;
		}
	}

	private Map<TCDefinition, List<Apply>> applymap = null;
	private Map<TCNameToken, TCNameSet> callmap = null;
	private Map<TCNameToken, TCDefinition> defmap = null;
	private Map<TCNameToken, TCDefinitionListList> recursiveLoops = null;

	public static TCRecursiveFunctions getInstance()
	{
		if (INSTANCE == null)
		{
			INSTANCE = new TCRecursiveFunctions();
		}
		
		return INSTANCE;
	}

	public void reset()
	{
		recursiveLoops = new HashMap<TCNameToken, TCDefinitionListList>();
		applymap = new HashMap<TCDefinition, List<Apply>>();
		callmap = new HashMap<TCNameToken, TCNameSet>();
		defmap = new  HashMap<TCNameToken, TCDefinition>();
	}
	
	public void addApplyExp(TCDefinition parent, TCApplyExpression apply, TCDefinition calling)
	{
		if (!applymap.containsKey(parent))
		{
			applymap.put(parent, new Vector<Apply>());
			callmap.put(parent.name, new TCNameSet());
		}
		
		applymap.get(parent).add(new Apply(apply, calling));
		callmap.get(parent.name).add(calling.name);
		defmap.put(parent.name, parent);
		defmap.put(calling.name, calling);
	}
	
	public void typeCheck()
	{
		recursiveLoops.clear();
		
		for (TCNameToken name: callmap.keySet())
		{
			for (Stack<TCNameToken> cycle: reachable(name, callmap))
			{
				addCycle(name, findDefinitions(cycle));
			}
		}

		for (TCDefinition parent: applymap.keySet())
		{
			TCDefinitionListList cycles = recursiveLoops.get(parent.name);

			if (cycles != null)
			{
				for (Apply pair: applymap.get(parent))
				{
					pair.apply.typeCheckCycles(cycles, parent, pair.calling);
				}
			}
		}
		
		reset();	// save space!
	}

	private TCDefinitionList findDefinitions(Stack<TCNameToken> stack)
	{
		TCDefinitionList list = new TCDefinitionList();
		
		for (TCNameToken name: stack)
		{
			list.add(defmap.get(name));
		}
		
		return list.contains(null) ? null : list;	// Usually local func definitions
	}

	private void addCycle(TCNameToken name, TCDefinitionList defs)
	{
		if (defs != null)
		{
			TCDefinitionListList existing = recursiveLoops.get(name);
			
			if (existing == null)
			{
				existing = new TCDefinitionListList();
				recursiveLoops.put(name, existing);
			}
			
			existing.add(defs);
		}
	}

	public List<String> toStrings(TCDefinitionList cycle)
	{
		List<String> calls = new Vector<String>();

		for (TCDefinition d: cycle)
		{
			calls.add(d.name.toString());	// ie. include PP param types
		}
		
		return calls;
	}

	/**
	 * Return true if the name sought is reachable via the next set of names passed using
	 * the dependency map. The stack passed records the path taken to find a cycle.
	 */
	private Set<Stack<TCNameToken>> reachable(TCNameToken sought,
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

		boolean found = false;
		
		if (nextset.contains(sought))
		{
			stack.push(sought);
			Stack<TCNameToken> loop = new Stack<TCNameToken>();
			loop.addAll(stack);
			loops.add(loop);
			stack.pop();
			found = true;
		}
		
		if (Properties.tc_skip_recursive_check)
		{
			return found;	// For now, to allow us to skip if there are issues.
		}
		
		if (stack.size() < LOOP_SIZE_LIMIT)
		{
			for (TCNameToken nextname: nextset)
			{
				if (!stack.contains(nextname))	// Been here before!
				{
					stack.push(nextname);
					
					if (reachable(sought, dependencies.get(nextname), dependencies, stack, loops))
					{
						found = true;
					}
					
					stack.pop();
				}
			}
		}
		
		return found;
	}
}
