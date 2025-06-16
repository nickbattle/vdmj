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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.modules.TCImportFromModule;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;

public class DependencyOrder
{
	protected Map<String, File> nameToFile = new HashMap<String, File>();
	protected Map<String, Set<String>> uses = new HashMap<String, Set<String>>();
	protected Map<String, Set<String>> usedBy  = new HashMap<String, Set<String>>();
	
	private boolean sortCalled;
	
	public DependencyOrder()
	{
		sortCalled = false;
	}
    			
    public void classOrder(TCClassList classList)
	{
    	for (TCClassDefinition c: classList)
		{
    		String classname = c.name.getName();
    		
    		if (!classname.equals("CPU") && !classname.equals("BUS"))
    		{
    			nameToFile.put(classname, c.name.getLocation().file);

    			for (TCDefinition def: c.getDefinitions())
		    	{
		        	Environment globals = new FlatEnvironment(new TCDefinitionList());
		    		Environment empty = new FlatEnvironment(new TCDefinitionList());
					TCNameSet freevars = def.getDependencies(globals, empty, new AtomicBoolean(false));
			    	
			    	for (TCNameToken dep: freevars)
			    	{
			    		String m = dep.getModule();
						add(classname, m.equals("CLASS") ? dep.getName() : m);
			    	}
			    }
    		}
		}
    	
    	for (String cname: nameToFile.keySet())
    	{
    		if (!uses.containsKey(cname))
    		{
    			uses.put(cname, new HashSet<String>());
    		}
    	}
	}

	public void moduleOrder(TCModuleList moduleList)
    {
		for (TCModule m: moduleList)
		{
	    	String myname = m.name.getName();
	    	nameToFile.put(myname, m.name.getLocation().file);
			
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
    }
	
	public void definitionOrder(TCDefinitionList definitions)
	{
		for (TCDefinition def: definitions.singleDefinitions())
		{
	    	String myname = def.name.getName();
	    	nameToFile.put(myname, def.location.file);

			TCNameSet freevars = def.getFreeVariables();
	    	
	    	for (TCNameToken dep: freevars)
	    	{
				add(myname, dep.getName());
	    	}
	    }		
	}
	
    /**
     * Create a "dot" language version of the graph for the graphviz tool.
     * @throws IOException 
     */
    public void graphOf(File filename) throws IOException
	{
    	Map<String, Set<String>> map = uses;
    	
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
    
    public List<String> getStartpoints()
    {
		/*
		 * The startpoints are where there are no incoming links to a node. So
		 * the usedBy entry is blank (removed cycles) or null.
		 */
		List<String> startpoints = new Vector<String>();

		for (String module: nameToFile.keySet())
		{
			if (usedBy.get(module) == null || usedBy.get(module).isEmpty())
			{
				startpoints.add(module);
				usedBy.put(module, new HashSet<String>());
			}
		}

		return startpoints;
    }
    
    /**
     * Note that the graph must be acyclic!
     * @return the initialization order of the names
     */
    public List<String> topologicalSort()
    {
    	return topologicalSort(getStartpoints());
    }
    
    public List<String> topologicalSort(List<String> startpoints)
    {
    	if (sortCalled)
    	{
    		throw new IllegalStateException("topologicalSort already called");
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
	    			if (delete(n, m) == 0)
			    	{
						startpoints.add(m);
				    }
		    	}
		    }
		}
		
		if (edgeCount() > 0)
		{
			throw new IllegalStateException("Dependency graph has cycles");
		}
		else
		{
			Collections.reverse(ordering);	// the init order
			sortCalled = true;
			return ordering;
		}
    }

	private int edgeCount()
	{
		int count = 0;
		
		for (Set<String> set: uses.values())
		{
			count += set.size();
		}
		
		for (Set<String> set: usedBy.values())
		{
			count += set.size();	// include reverse links too?
		}
		
		return count;
	}

	protected void add(String from, String to)
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
	
	protected int delete(String from, String to)
	{
    	uses.get(from).remove(to);
    	usedBy.get(to).remove(from);
    	return usedBy.get(to).size();	// remaining size
	}

	public static void main(String[] args)
	{
		class Test extends DependencyOrder
		{
			public void test()
			{
				nameToFile.put("A", new File("test.vdm"));
				add("A", "B");	// A depends on B etc.
				add("A", "C");
				add("B", "C");
				System.out.println(nameToFile);
				System.out.println(uses);
				System.out.println(usedBy);
				System.out.println("Sort = " + topologicalSort());
			}
		};
		
		Test test = new Test();
		test.test();
	}
}
