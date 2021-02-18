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
	private List<String> startpoints = new Vector<String>();
	
	public OrderPlugin(Interpreter interpreter)
	{
		super(interpreter);
	}

	@Override
	public boolean run(String[] argv) throws Exception
	{
		startpoints.clear();
		
		for (int i=1; i < argv.length; i++)
		{
			startpoints.add(argv[i]);
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

		Map<String, Set<String>> uses = new HashMap<String, Set<String>>();
		Map<String, Set<String>> usedBy  = new HashMap<String, Set<String>>();
		Environment globals = new FlatEnvironment(alldefs, null);
		Set<String> allModules = new HashSet<String>();

		for (TCModule m: modules)
		{
			TCNameSet freevars = new TCNameSet();
			
	    	for (TCDefinition def: m.defs)
	    	{
	    		Environment empty = new FlatEnvironment(new TCDefinitionList());
				freevars.addAll(def.getFreeVariables(globals, empty, new AtomicBoolean(false)));
	    	}
	    	
	    	Set<String> depnames = new HashSet<String>();
	    	String myname = m.name.getName();
	    	allModules.add(myname);
	    	
	    	for (TCNameToken name: freevars)
	    	{
	    		depnames.add(name.getModule());		// Just module names
	    	}
	    	
	    	uses.put(myname, depnames);
	    	
	    	for (String depname: depnames)
	    	{
	    		Set<String> curr = usedBy.get(depname);
	    		
	    		if (curr == null)
	    		{
	    			curr = new HashSet<String>();
	    			usedBy.put(depname, curr);
	    		}
	    		
	    		curr.add(myname);
	    	}
		}

		if (startpoints.isEmpty())
		{
			for (TCModule m: modules)
			{
				String name = m.name.getName();
				
				if (usedBy.get(name) == null)
				{
					startpoints.add(name);
				}
			}
			
			if (startpoints.isEmpty())
			{
				Console.out.println("Module dependencies have cycles");
				Console.out.println("Run 'order <start modules>'");
				Console.out.println("Where start modules are the least dependent.");
				return;
			}
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
		
		List<String> ordering = new Vector<String>();
		
		while (!startpoints.isEmpty())
		{
		    String node = startpoints.remove(0);
	    	ordering.add(node);
	    	Set<String> usedBySet = usedBy.get(node);
	    	
	    	if (usedBySet != null)
	    	{
		    	for (String next: usedBySet)
		    	{
					if (!ordering.contains(next) && !startpoints.contains(next))
					{
						startpoints.add(next);
					}
			    }
	    	}
		}
		
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

	@Override
	public String help()
	{
		return "order [start points] - print optimal module/class order";
	}
}
