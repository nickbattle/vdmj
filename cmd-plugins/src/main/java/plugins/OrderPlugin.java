/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fujitsu.vdmj.commands.CommandPlugin;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.ModuleInterpreter;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;

public class OrderPlugin extends CommandPlugin
{
	public OrderPlugin(Interpreter interpreter)
	{
		super(interpreter);
	}

	@Override
	public boolean run(String[] argv) throws Exception
	{
		if (argv.length != 1)
		{
			Console.out.println(help());
			return true;
		}

		if (interpreter instanceof ModuleInterpreter)
		{
			moduleOrder(interpreter.getTC());
		}

		return true;	// Even if command failed
	}
    			
    private void moduleOrder(TCModuleList modules)
    {
		TCDefinitionList alldefs = new TCDefinitionList();

		for (TCModule m: modules)
		{
			for (TCDefinition d: m.importdefs)
			{
				alldefs.add(d);
			}
		}

		for (TCModule m: modules)
		{
			for (TCDefinition d: m.defs)
			{
				alldefs.add(d);
			}
		}

		Map<String, Set<String>> dependencies = new HashMap<String, Set<String>>();
		Environment globals = new FlatEnvironment(alldefs, null);
		List<String> startpoints = new Vector<String>();

		for (TCModule m: modules)
		{
			TCNameSet freevars = new TCNameSet();
			
	    	for (TCDefinition def: m.defs)
	    	{
	    		Environment empty = new FlatEnvironment(new TCDefinitionList());
				freevars.addAll(def.getFreeVariables(globals, empty, new AtomicBoolean(false)));
	    	}
	    	
	    	Set<String> modnames = new HashSet<String>();
	    	String myname = m.name.getName();
	    	
	    	for (TCNameToken name: freevars)
	    	{
	    		modnames.add(name.getModule());
	    	}
	    	
	    	dependencies.put(myname, modnames);
	    	
	    	if (modnames.isEmpty() || modnames.size() == 1 && modnames.contains(myname))
	    	{
	    		startpoints.add(myname);
	    	}
		}
		
		if (startpoints.isEmpty())	// complex loops, so choose smallest dependencies
		{
			int min = Integer.MAX_VALUE;
			
			for (String name: dependencies.keySet())
			{
				int size = dependencies.get(name).size();
				
				if (size < min)
				{
					min = size;
					startpoints.clear();
					startpoints.add(name);
				}
				else if (size == min)
				{
					startpoints.add(name);
				}
			}
		}
		
		// This is Kuhn's algorithm for topological sorting (but allowing loops)
	
		List<String> ordering = new Vector<String>(dependencies.size());
		
		while (!startpoints.isEmpty())
		{
		    String node = startpoints.remove(0);
		    
		    if (!ordering.contains(node))
		    {
		    	ordering.add(node);
		    }
		    
			for (Entry<String, Set<String>> pair: dependencies.entrySet())
		    {
				String key = pair.getKey();
				Set<String> deps = pair.getValue();
				
				if (deps.contains(node))
				{
					deps.remove(node);
					
					if (deps.isEmpty() || deps.size() == 1 && deps.contains(key))
					{
						if (!startpoints.contains(key))
						{
							startpoints.add(key);
						}
					}
				}
		    }
		}
		
		for (String name: ordering)
		{
			Console.out.println(name);
		}
    }

	@Override
	public String help()
	{
		return "order - print optimal module/class order";
	}
}
