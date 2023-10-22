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
import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.in.expressions.INBooleanLiteralExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INBindingSetter;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.po.annotations.POAnnotation;
import com.fujitsu.vdmj.pog.POStatus;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.ObjectContext;
import com.fujitsu.vdmj.tc.expressions.TCExistsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.util.GetResource;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.ParameterValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

import annotations.IterableContext;
import annotations.po.POQuickCheckAnnotation;
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
							errorln("The " + instance.getName() + " strategy is not enabled. Add -s " + instance.getName());
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
			
			if (arg.equals("-s"))
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
					errorln("-s must be followed by a strategy name");
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
			
			if (!all.isEmpty() && chosen.isEmpty())
			{
				String m = (Settings.dialect == Dialect.VDM_SL) ? "module" : "class";
				println("No POs in current " + m + " (" + def + ")");
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
			if (po.isCheckable && po.getCheckedExpression() != null)
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
		
		if (!po.isCheckable)
		{
			return new Results(null, union, 0);
		}
		
		INExpression exp = getINExpression(po);
		List<INBindingSetter> binds = getINBindList(exp);
		long before = System.currentTimeMillis();
		IterableContext ictxt = addTypeParams(po, Interpreter.getInstance().getInitialContext());
		
		while (ictxt.hasNext())
		{
			ictxt.next();
			
			for (QCStrategy strategy: strategies)
			{
				Results sresults = strategy.getValues(po, exp, binds, ictxt);
				Map<String, ValueList> cexamples = sresults.counterexamples;
				
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
				
				if (sresults.provedBy != null)	// No need to go further
				{
					return new Results(sresults.provedBy, cexamples, System.currentTimeMillis() - before);
				}
			}
		
			for (INBindingSetter bind: binds)
			{
				if (!union.containsKey(bind.toString()))
				{
					// Generate some values for missing bindings, using the fixed method
					ValueList list = new ValueList();
					list.addAll(bind.getType().apply(new FixedRangeCreator(ictxt), 10));
					union.put(bind.toString(), list);
				}
			}
		}
		
		return new Results(null, union, System.currentTimeMillis() - before);
	}
	
	public void checkObligation(ProofObligation po, Results results)
	{
		try
		{
			resetErrors();	// Only flag fatal errors
			INExpression poexp = getINExpression(po);
			List<INBindingSetter> bindings = getINBindList(poexp);

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
			else if (results.provedBy != null)
			{
				po.setStatus(POStatus.PROVED);
				po.setProvedBy(results.provedBy);
				printf("PO #%d, PROVED by %s strategy %s\n", po.number, results.provedBy, duration(results.duration));
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
				ContextException exception = null;
				Context ctxt = Interpreter.getInstance().getInitialContext();
				
				if (Settings.dialect != Dialect.VDM_SL)
				{
					ClassInterpreter in = ClassInterpreter.getInstance();
					INClassDefinition classdef = in.getDefaultClass();
					ObjectValue object = classdef.newInstance(null, null, ctxt);

					ctxt = new ObjectContext(
							classdef.name.getLocation(), classdef.name.getName() + "()",
							ctxt, object);

					ctxt.put(classdef.name.getSelfName(), object);
				}
				
				IterableContext ictxt = addTypeParams(po, ctxt);
				
				try
				{
					verbose("PO #%d, starting...\n", po.number);
					
					do
					{
						ictxt.next();
						result = poexp.eval(ictxt);
					}
					while (ictxt.hasNext() && result.boolValue(ctxt));
				}
				catch (ContextException e)
				{
					if (e.rawMessage.equals("Execution cancelled"))
					{
						result = null;
					}
					else
					{
						result = new BooleanValue(false);
						exception = e;
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
						POStatus outcome = null;
						
						if (po.getCheckedExpression() instanceof TCExistsExpression)
						{
							outcome = POStatus.PROVED;		// An "exists" PO is PROVED, if true.
						}
						else
						{
							outcome = POStatus.MAYBE;
						}
						
						printf("PO #%d, %s %s\n", po.number, outcome.toString().toUpperCase(), duration(before, after));
						po.setStatus(outcome);
					}
					else
					{
						if (po.getCheckedExpression() instanceof TCExistsExpression)
						{
							printf("PO #%d, MAYBE %s\n", po.number, duration(before, after));
							po.setStatus(POStatus.MAYBE);
						}
						else
						{
							printf("PO #%d, FAILED %s: ", po.number, duration(before, after));
							po.setStatus(POStatus.FAILED);
							po.setCounterexample(printFailPath(bindings));
							
							if (exception != null)
							{
								String msg = "Causes " + exception.getMessage(); 
								println(msg);
								po.setCounterMessage(msg);
							}
							
							println("----");
							println(po);
						}
					}
				}
				else
				{
					String msg = String.format("Error: PO evaluation returns %s?\n", result.kind());
					printf("PO #%d, %s\n", po.number, msg);
					po.setStatus(POStatus.FAILED);
					po.setCounterMessage(msg);
					println("----");
					printBindings(bindings);
					println("----");
					println(po);
				}
			}
			catch (Exception e)
			{
				String msg = String.format("Exception: %s", e.getMessage());
				printf("PO #%d, %s\n", po.number, msg);
				po.setStatus(POStatus.FAILED);
				po.setCounterMessage(msg);
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
	
	private IterableContext addTypeParams(ProofObligation po, Context ctxt)
	{
		IterableContext ictxt = new IterableContext(po.location, "Type params", ctxt);

		if (po.typeParams != null)
		{
			if (po.annotations != null)
			{
				for (POAnnotation a: po.annotations)
				{
					if (a instanceof POQuickCheckAnnotation)
					{
						POQuickCheckAnnotation qca = (POQuickCheckAnnotation)a;
						int index = 0;
						
						for (TCType ptype: qca.qcTypes)
						{
							Map<TCNameToken, Value> map = ictxt.newMap(index++);
							map.put(qca.qcParam.name, new ParameterValue(ptype));
						}
					}
				}
			}
			
			if (!ictxt.hasNext())	// Still empty after any annotations
			{
				ictxt.newMap(0);	// So something to hold defaults
			}
			
			for (TCType type: po.typeParams)
			{
				TCParameterType ptype = (TCParameterType)type;
				ictxt.setDefaults(ptype.name, new ParameterValue(new TCRealType(po.location)));
			}
		}
		else
		{
			ictxt.newMap(0);	// So hasNext() and next() work
		}

		return ictxt;
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
	
	private Context printFailPath(List<INBindingSetter> bindings)
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
			return null;
		}
		
		printf("Counterexample: ");
		String sep = "";
		Context ctxt = path;
		
		while (ctxt.outer != null)
		{
			for (TCNameToken name: ctxt.keySet())
			{
				printf("%s%s = %s", sep, name, ctxt.get(name));
				sep = ", ";
			}
			
			ctxt = ctxt.outer;
		}
		
		println("");
		return path;
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
