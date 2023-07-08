/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package quickcheck;

import static com.fujitsu.vdmj.plugins.PluginConsole.errorln;
import static com.fujitsu.vdmj.plugins.PluginConsole.printf;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;
import static com.fujitsu.vdmj.plugins.PluginConsole.verbose;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.fujitsu.vdmj.ast.lex.LexBooleanToken;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.expressions.INBooleanLiteralExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INForAllExpression;
import com.fujitsu.vdmj.in.patterns.INBindingSetter;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.RootContext;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.util.GetResource;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueSet;

import quickcheck.qcplugins.QCPlugin;
import quickcheck.visitors.InternalRangeCreator;
import quickcheck.visitors.TypeBindFinder;

public class QuickCheck
{
	private int errorCount = 0;
	private List<QCPlugin> plugins = null;
	private ProofObligationList chosen = null;
	
	public QuickCheck()
	{
		// nothing special
	}
	
	public boolean hasErrors()
	{
		return errorCount > 0;
	}
	
	public void loadPlugins(List<String> argv)
	{
		plugins = new Vector<QCPlugin>();
		errorCount = 0;
		
		try
		{
			List<String> names = pluginNames(argv);
			List<String> failed = new Vector<String>(names);
			List<String> classnames = GetResource.readResource("qc.plugins");
			
			for (String classname: classnames)
			{
				try
				{
					Class<?> clazz = Class.forName(classname);
					Constructor<?> ctor = clazz.getDeclaredConstructor(List.class);
					QCPlugin instance = (QCPlugin) ctor.newInstance((Object)argv);
					
					if (names.isEmpty() || names.contains(instance.getName()))
					{
						plugins.add(instance);
						failed.remove(instance.getName());
					}
				}
				catch (ClassNotFoundException e)
				{
					errorln("Cannot find plugin class: " + classname);
					errorCount++;
				}
				catch (NoSuchMethodException e)
				{
					errorln("Plugin " + classname + " must implement ctor(List<String> argv)");
					errorCount++;
				}
				catch (Throwable th)
				{
					errorln("Plugin " + classname + ": " + th.toString());
					errorCount++;
				}
			}
			
			if (!failed.isEmpty())
			{
				for (String name: failed)
				{
					println("Could not find plugin " + name);
					errorCount++;
				}
			}
		}
		catch (Throwable e)
		{
			errorln("Cannot load plugins: " + e);
		}
	}
	
	public boolean initPlugins()
	{
		boolean doChecks = true;
		
		for (QCPlugin plugin: plugins)
		{
			doChecks = doChecks && plugin.init(this);
			
			if (plugin.hasErrors())
			{
				errorln("QCPlugin init failed: " + plugin.getName());
			}
			else
			{
				verbose("QCPlugin %s initialized\n", plugin.getName());
			}
		}

		return doChecks;
	}
	
	public List<QCPlugin> getPlugins()
	{
		return plugins;
	}
	
	public ProofObligationList getChosen()
	{
		return chosen;
	}
	
	public List<String> pluginNames(List<String> arglist)
	{
		List<String> names = new Vector<String>();
		Iterator<String> iter = arglist.iterator();
		
		while (iter.hasNext())
		{
			String arg = iter.next();
			
			if (arg.equals("-p"))
			{
				iter.remove();
				
				if (iter.hasNext())
				{
					arg = iter.next();
					iter.remove();
					names.add(arg);
				}
				else
				{
					errorln("-p must be followed by a plugin name");
					names.add("unknown");
				}
			}
		}
		
		return names;
	}
	
	public ProofObligationList getPOs(ProofObligationList all, List<Integer> poList)
	{
		errorCount = 0;
		
		if (poList.isEmpty())
		{
			chosen = new ProofObligationList();
			String def = Interpreter.getInstance().getDefaultName();
			
			for (ProofObligation po: all)
			{
				if (po.location.module.equals(def))
				{
					chosen.add(po);
				}
			}
			
			return chosen;	// No PO#s specified, so use default class/module's POs
		}
		else
		{
			chosen = new ProofObligationList();
			
			for (Integer n: poList)
			{
				if (n > 0 && n <= all.size())
				{
					chosen.add(all.get(n-1));
				}
				else
				{
					println("PO# " + n + " unknown. Must be between 1 and " + all.size());
					errorCount++;
				}
			}
			
			return errorCount > 0 ? null : chosen;
		}
	}
	
	public INExpression getINExpression(ProofObligation po)
	{
		try
		{
			if (po.isCheckable)
			{
				TCExpression tcexp = po.getCheckedExpression();
				return ClassMapper.getInstance(INNode.MAPPINGS).convert(tcexp);
			}
			else
			{
				// Not checkable, so just use "true"
				return new INBooleanLiteralExpression(new LexBooleanToken(true, po.location));
			}
		}
		catch (Exception e)
		{
			errorln("getINExpression: " + e);
			return new INBooleanLiteralExpression(new LexBooleanToken(false, po.location));
		}
	}
	
	public List<INBindingSetter> getINBindList(INExpression inexp)
	{
		return inexp.apply(new TypeBindFinder(), null);
	}
	
	public Map<String, ValueSet> getValues(ProofObligation po)
	{
		Map<String, ValueSet> union = new HashMap<String, ValueSet>();
		INExpression exp = getINExpression(po);
		List<INBindingSetter> binds = getINBindList(exp);
		
		for (QCPlugin plugin: plugins)
		{
			Map<String, ValueSet> pvalues = plugin.getValues(po, exp, binds);
			
			for (String bind: pvalues.keySet())
			{
				if (union.containsKey(bind))
				{
					union.get(bind).addAll(pvalues.get(bind));
				}
				else if (pvalues.containsKey(bind))		// plugin may not contribute all binds
				{
					union.put(bind, pvalues.get(bind));
				}
			}
		}
		
		for (INBindingSetter bind: binds)
		{
			if (!union.containsKey(bind.toString()))
			{
				// Generate some values for missing bindings, using the default method
				RootContext ctxt = Interpreter.getInstance().getInitialContext();
				ValueSet values = bind.getType().apply(new InternalRangeCreator(ctxt, 10), 5);
				union.put(bind.toString(), values);
			}
		}
		
		return union;
	}
	
	public void checkObligation(ProofObligation po, Map<String, ValueSet> ranges)
	{
		try
		{
			errorCount = 0;
			RootContext ctxt = Interpreter.getInstance().getInitialContext();
			List<INBindingSetter> bindings = null;

			if (!po.isCheckable)
			{
				printf("PO #%d, UNCHECKED\n", po.number);
				return;
			}

			INExpression poexp = getINExpression(po);
			bindings = getINBindList(poexp);
			
			for (INBindingSetter mbind: bindings)
			{
				ValueSet values = ranges.get(mbind.toString());
				
				if (values != null)
				{
					verbose("PO #%d, setting %s, %d values\n", po.number, mbind.toString(), values.size());
					mbind.setBindValues(values);	// Unset in finally clause
				}
				else
				{
					errorln("PO #" + po.number + ": No range defined for " + mbind);
					errorCount++;
				}
			}
			
			try
			{
				long before = System.currentTimeMillis();
				verbose("PO #%d, starting...\n", po.number);
				Value result = poexp.eval(ctxt);
				long after = System.currentTimeMillis();
				
				if (result instanceof BooleanValue)
				{
					if (result.boolValue(ctxt))
					{
						printf("PO #%d, PASSED %s\n", po.number, duration(before, after));
					}
					else
					{
						printf("PO #%d, FAILED %s: ", po.number, duration(before, after));
						printFailPath(INForAllExpression.failPath, bindings);
						println("----");
						println(po);
						errorCount++;
					}
				}
				else
				{
					printf("PO #%d, Error: PO evaluation returns %s?\n", po.number, result.kind());
					println("----");
					printBindings(bindings);
					println("----");
					println(po);
					errorCount++;
				}
			}
			catch (ContextException e)
			{
				printf("PO #%d, Exception: %s\n", po.number, e.getMessage());
				
				if (e.ctxt.outer != null)
				{
					e.ctxt.printStackFrames(Console.out);
				}
				else
				{
					println("In context of " + e.ctxt.title + " " + e.ctxt.location);
				}
				
				println("----");
				printBindings(bindings);
				println("----");
				println(po);
				errorCount++;
			}
			catch (Exception e)
			{
				printf("PO #%d, Exception: %s\n", po.number, e.getMessage());
				println("----");
				printBindings(bindings);
				println("----");
				println(po);
				errorCount++;
			}
			finally
			{
				for (INBindingSetter mbind: bindings)
				{
					mbind.setBindValues(null);
				}
			}
		}
		catch (Exception e)
		{
			errorCount++;
			println(e);
			return;
		}
	}
	
	private void printBindings(List<INBindingSetter> bindings)
	{
		for (INBindingSetter bind: bindings)
		{
			printf("%s = %s\n", bind, bind.getBindValues());
		}
	}
	
	private void printFailPath(Context path, List<INBindingSetter> bindings)
	{
		if (path == null || path.isEmpty())
		{
			printf("No counterexample\n");
			printBindings(bindings);
			return;
		}
		
		printf("Counterexample: ");
		String sep = "";
		Context ctxt = path;
		
		while (true)
		{
			if (ctxt.outer != null)
			{
				if (ctxt instanceof RootContext)
				{
					sep = "; from " + ctxt.title + " where ";
				}
				
				for (TCNameToken name: ctxt.keySet())
				{
					printf("%s%s = %s", sep, name, ctxt.get(name));
					sep = ", ";
				}
				
				ctxt = ctxt.outer;
			}
			else
			{
				break;
			}
		}
		
		println("");
	}

	private String duration(long before, long after)
	{
		double duration = (double)(after - before)/1000;
		return "in " + duration + "s";
	}
}
