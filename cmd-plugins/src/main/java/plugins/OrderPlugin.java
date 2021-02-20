/*******************************************************************************
 *
 *	Copyright (c) 2021 Nick Battle.
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

package plugins;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import com.fujitsu.vdmj.commands.CommandPlugin;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.ModuleInterpreter;
import com.fujitsu.vdmj.tc.modules.TCImportFromModule;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.modules.TCModuleList;

public class OrderPlugin extends CommandPlugin
{
	private List<String> startpoints = new Vector<String>();
	private Map<String, Set<String>> uses = new HashMap<String, Set<String>>();
	private Map<String, Set<String>> usedBy  = new HashMap<String, Set<String>>();
	
	public OrderPlugin(Interpreter interpreter)
	{
		super(interpreter);
	}

	@Override
	public boolean run(String[] argv) throws Exception
	{
		startpoints.clear();
		uses.clear();
		usedBy.clear();
		
		for (int i=1; i < argv.length; i++)
		{
			startpoints.add(argv[i]);
		}

		if (interpreter instanceof ModuleInterpreter)
		{
			moduleOrder(interpreter.getTC());
		}
		else
		{
			// classes!
		}

		return true;	// Even if command failed
	}
    			
    private void moduleOrder(TCModuleList modules)
    {
		Set<String> allModules = new HashSet<String>();

		for (TCModule m: modules)
		{
	    	String myname = m.name.getName();
	    	allModules.add(myname);
			
			if (m.imports != null)
			{
		    	for (TCImportFromModule ifm: m.imports.imports)
		    	{
					add(myname, ifm.name.getName());
		    	}
			}
			else
			{
				uses.put(myname, new HashSet<String>());
			}
	    }
		
		// First remove any cycles
		int count = 0;
		
		for (String start: allModules)
		{
			count += removeCycles(start, new Stack<String>());
		}

		Console.out.println("Removed " + count + " cycles");

		for (String module: allModules)
		{
			if (usedBy.get(module) == null || usedBy.get(module).isEmpty())
			{
				startpoints.add(module);
				usedBy.put(module, new HashSet<String>());
			}
		}

		Console.out.println("startpoints = " + startpoints);
		// graphOf(uses, "uses.dot");
		// graphOf(usedBy, "usedby.dot");
		
		if (startpoints.isEmpty())
		{
			Console.out.println("No dependency startpoints found");
			Console.out.println("Run 'order <start modules>'");
			Console.out.println("Where start modules have fewest imports (or none).");
			return;
		}
		else
		{
			boolean failed = false;
			
			for (String name: startpoints)
			{
				if (!allModules.contains(name))
				{
					Console.out.println("Unknown module: " + name);
					failed = true;
				}
			}
			
			if (failed)
			{
				return;
			}
		}
		
		//	See https://en.wikipedia.org/wiki/Topological_sorting#Kahn's_algorithm
		//
		//	L ← Empty list that will contain the sorted elements
		//	S ← Set of all nodes with no incoming edge
		//
		//	while S is not empty do
		//	    remove a node n from S
		//	    add n to L
		//	    for each node m with an edge e from n to m do
		//	        remove edge e from the graph
		//	        if m has no other incoming edges then
		//	            insert m into S
		//
		//	if graph has edges then
		//	    return error   (graph has at least one cycle)
		//	else 
		//	    return L   (a topologically sorted order)

		List<String> ordering = new Vector<String>();

		while (!startpoints.isEmpty())
		{
		    String n = startpoints.remove(0);
		    ordering.add(n);
		    Set<String> usesSet = uses.get(n);
	    	
		    if (usesSet != null)
		    {
		    	Set<String> copy = new HashSet<String>(usesSet);
		    	
		    	for (String m: copy)
		    	{	
	    			if (delete(n, m) == 0) // && !ordering.contains(m))
			    	{
						startpoints.add(m);
				    }
		    	}
		    }
		}
		
		Collections.reverse(ordering);
		
		for (String name: ordering)
		{
	    	Console.out.println(name);
		}
		
		for (String name: allModules)
		{
			if (!ordering.contains(name))
			{
				Console.out.println("Missing: " + name);
			}
		}
    }
    
    /**
     * Create a "dot" language version of the graph for the graphviz tool.
     */
    public void graphOf(Map<String, Set<String>> map, String filename)
	{
    	try
		{
			FileWriter fw = new FileWriter(filename); 
			StringBuilder sb = new StringBuilder();
			sb.append("digraph G {\n");

			for (String key: map.keySet())
			{
				Set<String> nextSet = map.get(key);
				
				for (String next: nextSet)
				{
					sb.append("\t");
					sb.append(key);
					sb.append(" -> ");
					sb.append(next);
					sb.append(";\n");
				}
			}
			
			sb.append("}\n");
			fw.write(sb.toString());
			fw.close();
		}
		catch (IOException e)
		{
			Console.out.println("Cannot write " + filename + ": " + e);
		}
	}

	private int removeCycles(String start, Stack<String> stack)
	{
    	int count = 0;
    	Set<String> nextSet = new HashSet<String>(uses.get(start));
    	
    	if (!nextSet.isEmpty())
    	{
	    	stack.push(start);
	    	
	    	for (String next: nextSet)
	    	{
	    		if (stack.contains(next))
	    		{
	    			delete(start, next);
	    			count = count + 1;
	    		}
	    		else
	    		{
	    			count += removeCycles(next, stack);
	    		}
	    	}
	    	
	    	stack.pop();
    	}
    	
    	return count;
	}

	private int delete(String from, String to)
	{
    	uses.get(from).remove(to);
    	usedBy.get(to).remove(from);
    	return usedBy.get(to).size();	// remaining size
	}

	private void add(String from, String to)
    {
    	if (!from.equals(to))
    	{
	    	Set<String> set = uses.get(from);
	    	
	    	if (set == null)
	    	{
	    		set = new HashSet<String>();
	    		uses.put(from, set);
	    	}
	    	
    		set.add(to);
    		set = usedBy.get(to);
	    	
	    	if (set == null)
	    	{
	    		set = new HashSet<String>();
	    		usedBy.put(to, set);
	    	}
	    	
    		set.add(from);
    	}
    }

	@Override
	public String help()
	{
		return "order [start points] - print optimal module/class order";
	}
}
