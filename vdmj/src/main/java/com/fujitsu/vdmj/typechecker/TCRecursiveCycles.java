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
package com.fujitsu.vdmj.typechecker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.tc.annotations.TCOperationMeasureAnnotation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionListList;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;

/**
 * A class to hold recursive loop data, which is used to detect mutual recursion and
 * missing measure functions.
 */
public class TCRecursiveCycles
{
	private static final int LOOP_SIZE_LIMIT = 8;
	private static TCRecursiveCycles INSTANCE = null;
	
	private static class Apply
	{
		public final TCDefinitionListList loops;
		public final TCDefinition calling;
		
		public Apply(TCDefinitionListList loops, TCDefinition calling)
		{
			this.loops = loops;
			this.calling = calling;
		}
	}

	private Map<TCDefinition, List<Apply>> applymap = null;
	private Map<TCNameToken, TCNameSet> callmap = null;
	private Map<TCNameToken, TCDefinition> defmap = null;
	private Map<TCNameToken, TCDefinitionListList> recursiveLoops = null;

	public static TCRecursiveCycles getInstance()
	{
		if (INSTANCE == null)
		{
			INSTANCE = new TCRecursiveCycles();
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
	
	public void addCaller(TCDefinition parent, TCDefinitionListList loops, TCDefinition calling)
	{
		if (!applymap.containsKey(parent))
		{
			applymap.put(parent, new Vector<Apply>());
			callmap.put(parent.name, new TCNameSet());
		}
		
		applymap.get(parent).add(new Apply(loops, calling));
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
					typeCheckCycles(cycles, parent, pair.calling, pair.loops);
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
	
	private void typeCheckCycles(TCDefinitionListList cycles,
		TCDefinition parent, TCDefinition calling, TCDefinitionListList loops)
	{
		List<List<String>> cycleNames = new Vector<List<String>>();
		boolean mutuallyRecursive = false;
		loops.clear();

		for (TCDefinitionList cycle: cycles)
		{
			if (cycle.size() >= 2)
			{
				if (cycle.get(1).equals(calling))	// The parent cycle involves this next call
				{
					loops.add(cycle);
					cycleNames.add(toStrings(cycle));
					mutuallyRecursive = mutuallyRecursive || cycle.size() > 2;	// eg. [f, g, f] not [f, f]
					checkCycleMeasures(cycle);
				}
			}
		}
		
		if (cycleNames.isEmpty())
		{
			// No recursion via this "parent calling" 
			return;
		}
		
		if (parent instanceof TCExplicitFunctionDefinition)
		{
			TCExplicitFunctionDefinition def = (TCExplicitFunctionDefinition)parent;
			def.recursive = true;
			
			if (def.measureExp == null)
			{
				missingMeasureWarning(mutuallyRecursive, def, cycleNames);
			}
		}
		else if (parent instanceof TCImplicitFunctionDefinition)
		{
			TCImplicitFunctionDefinition def = (TCImplicitFunctionDefinition)parent;
			def.recursive = true;
			
			if (def.measureExp == null)
			{
				missingMeasureWarning(mutuallyRecursive, def, cycleNames);
			}
		}
		else if (parent instanceof TCExplicitOperationDefinition)
		{
			TCExplicitOperationDefinition exop = (TCExplicitOperationDefinition)parent;
			exop.recursive = true;
			
			if (exop.annotations == null ||
				exop.annotations.getInstance(TCOperationMeasureAnnotation.class) == null)
			{
				missingMeasureWarning(mutuallyRecursive, exop, cycleNames);
			}
		}
		else if (parent instanceof TCImplicitOperationDefinition)
		{
			TCImplicitOperationDefinition imop = (TCImplicitOperationDefinition)parent;
			imop.recursive = true;

			if (imop.annotations == null ||
				imop.annotations.getInstance(TCOperationMeasureAnnotation.class) == null)
			{
				missingMeasureWarning(mutuallyRecursive, imop, cycleNames);
			}
		}
	}

	private void missingMeasureWarning(boolean mutuallyRecursive, TCDefinition def, List<List<String>> cycleNames)
	{
		if (mutuallyRecursive)
		{
			def.warning(5013, "Mutually recursive cycle has no measure");
			
			for (List<String> cycleName: cycleNames)
			{
				def.detail("Cycle", cycleName);
			}
		}
		else
		{
			String message = def.isFunction() ?
				"Recursive function has no measure" :
				"Recursive operation has no measure";

			def.warning(5012, message);
		}
	}

	private void checkCycleMeasures(TCDefinitionList cycle)
	{
		for (int i = 0; i < cycle.size()-2; i++)
		{
			TCDefinition d1 = cycle.get(i);
			TCDefinition d2 = cycle.get(i+1);
			StringBuilder sb1 = new StringBuilder();
			StringBuilder sb2 = new StringBuilder();
			
			TCType a = measureType(d1, sb1);
			TCType b = measureType(d2, sb2);
			
			if (a != null && b != null && !a.equals(b))
			{
				d1.report(3364, "Recursive cycle measures return different types");
				d1.detail(sb1.toString(), a);
				d1.detail(sb2.toString(), b);
			}
		}
	}

	private TCType measureType(TCDefinition def, StringBuilder mname)
	{
		if (def instanceof TCExplicitFunctionDefinition)
		{
			TCExplicitFunctionDefinition expl = (TCExplicitFunctionDefinition)def;

			if (expl.measureName != null)
			{
				mname.append(expl.measureName);
			}
			else
			{
				mname.append(def.name.toString());
			}

			return expl.measureDef != null ? expl.measureDef.type.result : null;
		}
		else if (def instanceof TCImplicitFunctionDefinition)
		{
			TCImplicitFunctionDefinition impl = (TCImplicitFunctionDefinition)def;

			if (impl.measureName != null)
			{
				mname.append(impl.measureName);
			}
			else
			{
				mname.append(def.name.toString());
			}

			return impl.measureDef != null ? impl.measureDef.type.result : null;
		}
		else if (def instanceof TCExplicitOperationDefinition)
		{
			if (def.annotations != null)
			{
				TCOperationMeasureAnnotation measure = def.annotations.getInstance(TCOperationMeasureAnnotation.class);

				if (measure != null)
				{
					mname.append(def.name.toString());
					return measure.args.get(0).getType();
				}
			}	
		}
		else if (def instanceof TCImplicitOperationDefinition)
		{
			if (def.annotations != null)
			{
				TCOperationMeasureAnnotation measure = def.annotations.getInstance(TCOperationMeasureAnnotation.class);

				if (measure != null)
				{
					mname.append(def.name.toString());
					return measure.args.get(0).getType();
				}
			}	
		}
		
		return null;
	}

	private List<String> toStrings(TCDefinitionList cycle)
	{
		List<String> calls = new Vector<String>();
		Set<String> modules = new HashSet<String>();

		for (TCDefinition d: cycle)
		{
			modules.add(d.name.getModule());
		}

		boolean explicit = (modules.size() > 1);	// Explicit names, if multiple module/classes

		for (TCDefinition d: cycle)
		{
			calls.add(d.name.getExplicit(explicit).toString());	// include PP param types
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
