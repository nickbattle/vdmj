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

package plugins;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.PluginRegistry;
import com.fujitsu.vdmj.plugins.analyses.TCPlugin;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.util.DependencyOrder;

/**
 * Attempt to produce an optimal ordering for modules and classes.
 */
public class OrderPlugin extends AnalysisCommand
{
	private final static String CMD = "order [filename]";
	private final static String USAGE = "Usage: " + CMD;
	public  final static String HELP = CMD + " - print/save optimal module/class order";

	public OrderPlugin(String line)
	{
		super(line);
		
		if (!argv[0].equals("order"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public String run(String line)
	{
		String outputfile = null;
		
		if (argv.length == 2)
		{
			outputfile = argv[1];
		}
		else if (argv.length != 1)
		{
			return USAGE;
		}
		
		Order order = new Order(outputfile);
		TCPlugin tc = PluginRegistry.getInstance().getPlugin("TC");
		
		if (Settings.dialect == Dialect.VDM_SL)
		{
			order.moduleOrder(tc.getTC());
		}
		else
		{
			order.classOrder(tc.getTC());
		}

		return null;
	}

	/**
	 * Extend the VDMJ DependencyOrder so that we can add Console messages.
	 */
	private static class Order extends DependencyOrder
	{
		private String outputfile;
		
		public Order(String outputfile)
		{
			this.outputfile = outputfile;
		}

		@Override
		public void moduleOrder(TCModuleList moduleList)
		{
			super.moduleOrder(moduleList);
			processGraph();
		}

		@Override
		public void classOrder(TCClassList classList)
		{
			super.classOrder(classList);
			processGraph();
		}
		
	    private void processGraph()
	    {
	    	/**
	    	 * If the output file looks like a DOT file, just write the DOT
	    	 * format without the cyclic removal.
	    	 */
	    	if (outputfile != null && outputfile.endsWith(".dot"))
	    	{
	    		graphOf(uses, outputfile);
	    		return;
	    	}
	    	
			/**
			 * First remove any cycles. For some reason it's not enough to search from
			 * the startpoints, so we just search from everywhere. It's reasonably
			 * quick.
			 */
			Console.out.println("Checking for cyclic dependencies");
			int count = 0;
			
			for (String start: nameToFile.keySet())
			{
				count += removeCycles(start, new Stack<String>());
			}
	
			Console.out.println("Removed " + count + " cycles");
			
			/*
			 * The startpoints are where there are no incoming links to a node. So
			 * the usedBy entry is blank (removed cycles) or null.
			 */
			List<String> startpoints = getStartpoints();
	
			if (startpoints.isEmpty())
			{
				Console.out.println("No dependency startpoints found?");
				return;
			}
			else
			{
				Console.out.println("Ordering from startpoints: " + startpoints);
			}
			
			List<String> ordering = topologicalSort(startpoints);
			List<String> filenames = new Vector<String>();
			
			for (String name: ordering)
			{
				for (String module: nameToFile.keySet())
				{
					if (module.equals(name))
					{
						String file = nameToFile.get(module).toString();
						
						if (!filenames.contains(file))	// files with >= two modules
						{
							filenames.add(file);
						}
						break;
					}
				}
			}
			
			if (outputfile == null)
			{
				for (String name: filenames)
				{
			    	Console.out.println(name);
				}
			}
			else
			{
				try
				{
					PrintWriter fw = new PrintWriter(new FileWriter(outputfile)); 
					
					for (String name: filenames)
					{
				    	fw.println(name);
					}
	
					fw.close();
					Console.out.println("Order written to " + outputfile);
				}
				catch (IOException e)
				{
					Console.out.println("Cannot write " + outputfile + ": " + e);
				}
			}
	    }
	    
	    /**
	     * Create a "dot" language version of the graph for the graphviz tool.
	     */
	    private void graphOf(Map<String, Set<String>> map, String filename)
		{
	    	try
			{
				Console.out.println("Writing Graphviz DOT file to " + filename);
				super.graphOf(new File(filename));
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
		    			Console.out.println("Removing link " + start + " -> " + next);
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
	}
}
