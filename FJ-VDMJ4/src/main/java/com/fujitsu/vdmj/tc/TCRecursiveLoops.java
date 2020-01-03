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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionListList;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.expressions.TCApplyExpression;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.modules.TCModuleList;

/**
 * A class to hold recursive loop data, which is used to detect mutual recursion and
 * missing measure functions.
 */
public class TCRecursiveLoops extends TCNode
{
	private static final long serialVersionUID = 1L;
	private static TCRecursiveLoops INSTANCE = null;
	
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
	private TCRecursiveMap recursiveLoops = null;

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
		applymap = new HashMap<TCDefinition, List<Apply>>();
	}
	
	public void addApplyExp(TCDefinition parent, TCApplyExpression apply, TCDefinition calling)
	{
		if (calling instanceof TCExplicitFunctionDefinition ||
			calling instanceof TCImplicitFunctionDefinition)
		{
			if (!applymap.containsKey(parent))
			{
				applymap.put(parent, new Vector<Apply>());
			}
			
			applymap.get(parent).add(new Apply(apply, calling));
		}
	}
	
	private Map<TCNameToken, TCNameSet> getCallMap()
	{
		Map<TCNameToken, TCNameSet> callmap = new HashMap<TCNameToken, TCNameSet>();
		
		for (TCDefinition def: applymap.keySet())
		{
			callmap.put(def.name, def.getCallMap());
		}
		
		return callmap;
	}
	
	public void typeCheck(TCClassList classes)
	{
		Map<TCNameToken, TCNameSet> callmap = getCallMap();
		recursiveLoops.clear();
		
		for (TCNameToken name: callmap.keySet())
		{
			for (Stack<TCNameToken> cycle: reachable(name, callmap))
			{
				addCycle(name, classes.findDefinitions(cycle));
			}
		}

		for (TCDefinition parent: applymap.keySet())
		{
			for (Apply pair: applymap.get(parent))
			{
				pair.apply.typeCheckCycles(parent, pair.calling);
			}
		}
		
		reset();	// save space!
	}
	
	public void typeCheck(TCModuleList modules)
	{
		Map<TCNameToken, TCNameSet> callmap = getCallMap();
		recursiveLoops.clear();
		
		for (TCNameToken name: callmap.keySet())
		{
			for (Stack<TCNameToken> cycle: reachable(name, callmap))
			{
				addCycle(name, modules.findDefinitions(cycle));
			}
		}

		for (TCDefinition parent: applymap.keySet())
		{
			for (Apply pair: applymap.get(parent))
			{
				pair.apply.typeCheckCycles(parent, pair.calling);
			}
		}
		
		reset();	// save space!
	}

	private void addCycle(TCNameToken name, TCDefinitionList defs)
	{
		TCDefinitionListList existing = getCycles(name);
		
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

	public TCDefinitionListList getCycles(TCNameToken name)
	{
		return recursiveLoops.get(name);
	}
	
	public List<String> getCycleNames(TCDefinitionList cycle)
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
		
		if (nextset.contains(sought))
		{
			stack.push(sought);
			Stack<TCNameToken> loop = new Stack<TCNameToken>();
			loop.addAll(stack);
			loops.add(loop);
			return true;
		}
		
		if (System.getProperty("skip.recursion.check") != null)
		{
			return false;		// For now, to allow us to skip if there are issues.
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
