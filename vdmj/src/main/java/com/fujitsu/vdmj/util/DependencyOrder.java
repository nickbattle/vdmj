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

package com.fujitsu.vdmj.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
	
	public DependencyOrder()
	{
	}
    			
    public void classOrder(TCClassList classList)
	{
//		TCDefinitionList allDefs = new TCDefinitionList();

//    	for (TCClassDefinition c: classList)
//		{
//    		String name = c.name.getName();
//    		
//    		if (!name.equals("CPU") && !name.equals("BUS"))
//    		{
//    			nameToFile.put(name, c.name.getLocation().file);
//    			allDefs.addAll(c.getDefinitions());
//    		}
//		}

    	for (TCClassDefinition c: classList)
		{
    		String classname = c.name.getName();
			nameToFile.put(classname, c.name.getLocation().file);
    		
    		if (!classname.equals("CPU") && !classname.equals("BUS"))
    		{
		    	for (TCDefinition def: c.getDefinitions())
		    	{
		        	Environment globals = new FlatEnvironment(new TCDefinitionList());
		    		Environment empty = new FlatEnvironment(new TCDefinitionList());
					TCNameSet freevars = def.getFreeVariables(globals, empty, new AtomicBoolean(false));
			    	
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
}
