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
import com.fujitsu.vdmj.tc.expressions.TCExistsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.util.GetResource;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

import quickcheck.strategies.QCStrategy;
import quickcheck.strategies.Results;
import quickcheck.visitors.FixedRangeCreator;
import quickcheck.visitors.TypeBindFinder;

public class QuickCheck
{
	private int errorCount = 0;
	private List<QCStrategy> strategies = null;		// Configured to be used
	private List<QCStrategy> disabled = null;		// Known, but not to be used
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
	
	public void loadStrategies(List<String> argv)
	{
		strategies = new Vector<QCStrategy>();
		disabled = new Vector<QCStrategy>();
		errorCount = 0;
		
		try
		{
			List<String> names = strategyNames(argv);
			List<String> failed = new Vector<String>(names);
			List<String> classnames = GetResource.readResource("qc.strategies");
			
			for (String classname: classnames)
			{
				try
				{
					Class<?> clazz = Class.forName(classname);
					Constructor<?> ctor = clazz.getDeclaredConstructor(List.class);
					int argvSize = argv.size();
					QCStrategy instance = (QCStrategy) ctor.newInstance((Object)argv);
					
					if (instance.hasErrors())
					{
						errorCount++;
					}
					else if ((names.isEmpty() && instance.useByDefault()) || names.contains(instance.getName()))
					{
						strategies.add(instance);
						failed.remove(instance.getName());
					}
					else
					{
						disabled.add(instance);
						
						if (argvSize != argv.size())
						{
							// Constructor took some arguments
							errorln("The " + instance.getName() + " strategy is not enabled. Add -p " + instance.getName());
							errorCount++;
						}
					}
				}
				catch (ClassNotFoundException e)
				{
					errorln("Cannot find strategy class: " + classname);
					errorCount++;
				}
				catch (NoSuchMethodException e)
				{
					errorln("Strategy " + classname + " must implement ctor(List<String> argv)");
					errorCount++;
				}
				catch (Throwable th)
				{
					errorln("Strategy " + classname + ": " + th.toString());
					errorCount++;
				}
			}
			
			for (String name: failed)
			{
				errorln("Could not find strategy " + name);
				errorCount++;
			}
		}
		catch (Throwable e)
		{
			errorln("Cannot load strategies: " + e);
			errorCount++;
		}
	}
	
	public boolean initStrategies()
	{
		boolean doChecks = true;
		
		for (QCStrategy strategy: strategies)
		{
			doChecks = doChecks && strategy.init(this);
			
			if (strategy.hasErrors())
			{
				errorln("QCStrategy init failed: " + strategy.getName());
			}
			else
			{
				verbose("QCStrategy %s initialized\n", strategy.getName());
			}
		}

		return doChecks;
	}
	
	public List<QCStrategy> getEnabledStrategies()
	{
		return strategies;
	}
	
	public List<QCStrategy> getDisabledStrategies()
	{
		return disabled;
	}
	
	public List<QCStrategy> getAllStrategies()	// Enabled and disabled
	{
		List<QCStrategy> all = new Vector<QCStrategy>(strategies);
		all.addAll(disabled);
		return all;
	}
	
	public ProofObligationList getChosen()
	{
		return chosen;
	}
	
	public List<String> strategyNames(List<String> arglist)
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
					errorln("-p must be followed by a strategy name");
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
					errorln("PO# " + n + " unknown. Must be between 1 and " + all.size());
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
		Map<String, ValueList> union = new HashMap<String, ValueList>();
		boolean proved = false;
		INExpression exp = getINExpression(po);
		List<INBindingSetter> binds = getINBindList(exp);
		long before = System.currentTimeMillis();
		
		for (QCStrategy strategy: strategies)
		{
			Results presults = strategy.getValues(po, exp, binds);
			Map<String, ValueList> cexamples = presults.counterexamples;
			
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
				// Generate some values for missing bindings, using the fixed method
				RootContext ctxt = Interpreter.getInstance().getInitialContext();
				ValueList list = new ValueList();
				list.addAll(bind.getType().apply(new FixedRangeCreator(ctxt), 10));
				union.put(bind.toString(), list);
			}
		}
		
		return new Results(proved, union, System.currentTimeMillis() - before);
	}
	
	public void checkObligation(ProofObligation po, Results results)
	{
		try
		{
			resetErrors();	// Only flag fatal errors
			RootContext ctxt = Interpreter.getInstance().getInitialContext();
			INExpression poexp = getINExpression(po);
			List<INBindingSetter> bindings = getINBindList(poexp);;

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
			else if (results.proved &&
					 results.counterexamples.isEmpty() &&
					 !bindings.isEmpty())	// empty binds => simple forall over sets, so must execute
			{
				po.status = POStatus.PROVED;
				printf("PO #%d, PROVED %s\n", po.number, duration(results.duration));
				return;
			}

			try
			{
				Map<String, ValueList> cexamples = results.counterexamples;
				
				for (INBindingSetter mbind: bindings)
				{
					ValueList values = cexamples.get(mbind.toString());
					
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
					
					if (e.rawMessage.equals("Execution cancelled"))
					{
						result = null;
					}
					else
					{
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
				}
				
				long after = System.currentTimeMillis() + results.duration;
				
				if (result == null)		// cancelled
				{
					println("----");
					printBindings(bindings);
					println("----");
					println(po);
				}
				else if (result instanceof BooleanValue)
				{
					if (result.boolValue(ctxt))
					{
						String outcome = null;
						
						if (po.getCheckedExpression() instanceof TCExistsExpression)
						{
							outcome = "PROVED";		// Any "true" of an exists is PROVED.
						}
						else
						{
							outcome = (results.proved) ? "PROVED" : "PASSED";
						}
						
						printf("PO #%d, %s %s\n", po.number, outcome, duration(before, after));
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
		int MAXVALUES = 10;
		
		for (INBindingSetter bind: bindings)
		{
			printf("%s = [", bind);
			
			ValueList list = bind.getBindValues();
			int max = (list.size() > MAXVALUES) ? MAXVALUES : list.size();
			String sep = "";
			
			for (int i=0; i<max; i++)
			{
				printf("%s%s", sep, list.get(i).toShortString(20));
				sep = ", ";
			}
			
			if (max < list.size())
			{
				printf("... (%d values)]\n", list.size());
			}
			else
			{
				printf("]\n");
			}
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

	private String duration(long time)
	{
		double duration = (double)(time)/1000;
		return "in " + duration + "s";
	}

	private String duration(long before, long after)
	{
		double duration = (double)(after - before)/1000;
		return "in " + duration + "s";
	}
}
