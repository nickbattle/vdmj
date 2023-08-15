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

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.lex.LexBooleanToken;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.expressions.INBooleanLiteralExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INBindingSetter;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.pog.POStatus;
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
import quickcheck.qcplugins.Results;
import quickcheck.visitors.InternalRangeCreator;
import quickcheck.visitors.TypeBindFinder;

public class QuickCheck
{
	private int errorCount = 0;
	private List<QCPlugin> plugins = null;		// Configured to be used
	private List<QCPlugin> unused = null;		// Known, but not to be used
	private ProofObligationList chosen = null;
	
	public QuickCheck()
	{
		// nothing special
	}
	
	public boolean hasErrors()
	{
		return errorCount > 0;
	}
	
	public void resetErrors()
	{
		errorCount = 0;
	}
	
	public void loadPlugins(List<String> argv)
	{
		plugins = new Vector<QCPlugin>();
		unused = new Vector<QCPlugin>();
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
					
					if ((names.isEmpty() && instance.useByDefault()) || names.contains(instance.getName()))
					{
						plugins.add(instance);
						failed.remove(instance.getName());
					}
					else
					{
						unused.add(instance);
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
	
	public List<QCPlugin> getAllPlugins()	// Used and unused
	{
		List<QCPlugin> all = new Vector<QCPlugin>(plugins);
		all.addAll(unused);
		return all;
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
	
	public Results getValues(ProofObligation po)
	{
		Map<String, ValueSet> union = new HashMap<String, ValueSet>();
		boolean proved = false;
		INExpression exp = getINExpression(po);
		List<INBindingSetter> binds = getINBindList(exp);
		
		for (QCPlugin plugin: plugins)
		{
			Results presults = plugin.getValues(po, exp, binds);
			Map<String, ValueSet> cexamples = presults.counterexamples;
			
			for (String bind: cexamples.keySet())
			{
				if (union.containsKey(bind))
				{
					union.get(bind).addAll(cexamples.get(bind));
				}
				else
				{
					union.put(bind, cexamples.get(bind));
				}
			}
			
			proved |= presults.proved;
		}
		
		for (INBindingSetter bind: binds)
		{
			if (!union.containsKey(bind.toString()))
			{
				// Generate some values for missing bindings, using the default method
				RootContext ctxt = Interpreter.getInstance().getInitialContext();
				ValueSet values = bind.getType().apply(new InternalRangeCreator(ctxt, 10), 10);
				union.put(bind.toString(), values);
			}
		}
		
		return new Results(proved, union);
	}
	
	public void checkObligation(ProofObligation po, Results results)
	{
		try
		{
			resetErrors();	// Only flag fatal errors
			RootContext ctxt = Interpreter.getInstance().getInitialContext();
			List<INBindingSetter> bindings = null;

			if (!po.isCheckable)
			{
				printf("PO #%d, UNCHECKED\n", po.number);
				return;
			}
			else if (po.status == POStatus.TRIVIAL)
			{
				printf("PO #%d, TRIVIAL by %s\n", po.number, po.proof);
				return;
			}
			else if (results.proved)
			{
				po.status = POStatus.PROVED;
				printf("PO #%d, PROVED\n", po.number);
				return;
			}

			try
			{
				Map<String, ValueSet> cexamples = results.counterexamples;
				INExpression poexp = getINExpression(po);
				bindings = getINBindList(poexp);
				
				for (INBindingSetter mbind: bindings)
				{
					ValueSet values = cexamples.get(mbind.toString());
					
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
				
				long before = System.currentTimeMillis();
				Value result = new BooleanValue(false);
				
				try
				{
					verbose("PO #%d, starting...\n", po.number);
					result = poexp.eval(ctxt);
				}
				catch (ContextException e)
				{
					printf("PO #%d, Exception: %s\n", po.number, e.getMessage());
					result = new BooleanValue(false);

					if (Settings.verbose)
					{
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
					}
					else
					{
						println("Use -verbose to see exception stack");
					}
				}
				
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
						printFailPath(bindings);
						println("----");
						println(po);
					}
				}
				else
				{
					printf("PO #%d, Error: PO evaluation returns %s?\n", po.number, result.kind());
					println("----");
					printBindings(bindings);
					println("----");
					println(po);
				}
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
					mbind.setCounterexample(null);
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
	
	private void printFailPath(List<INBindingSetter> bindings)
	{
		Context path = null;
		
		for (INBindingSetter setter: bindings)
		{
			path = setter.getCounterexample();
			
			if (path != null)
			{
				break;	// Any one will do - see INForAllExpression
			}
		}
		
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
