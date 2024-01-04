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
import static com.fujitsu.vdmj.plugins.PluginConsole.println;
import static quickcheck.commands.QCConsole.infof;
import static quickcheck.commands.QCConsole.infoln;
import static quickcheck.commands.QCConsole.verbose;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.lex.LexBooleanToken;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.annotations.INAnnotation;
import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.in.expressions.INBooleanLiteralExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INBindingOverride;
import com.fujitsu.vdmj.in.patterns.INBindingGlobals;
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
import com.fujitsu.vdmj.runtime.ValueException;
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

import quickcheck.annotations.po.POQuickCheckAnnotation;
import quickcheck.annotations.IterableContext;
import quickcheck.strategies.FixedQCStrategy;
import quickcheck.strategies.QCStrategy;
import quickcheck.strategies.StrategyResults;
import quickcheck.visitors.FixedRangeCreator;
import quickcheck.visitors.TypeBindFinder;

public class QuickCheck
{
	private static final long DEFAULT_TIMEOUT = 5 * 1000;	// 5s timeout
	private int errorCount = 0;
	private List<QCStrategy> strategies = null;		// Configured to be used
	private List<QCStrategy> disabled = null;		// Known, but not to be used
	private ProofObligationList chosen = null;
	private long timeout = 0;
	
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
	
	public boolean initStrategies(long timeoutSecs)
	{
		this.timeout = (timeoutSecs < 0) ? DEFAULT_TIMEOUT : timeoutSecs * 1000;
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
	
	public ProofObligationList getPOs(ProofObligationList all, List<Integer> poList, List<String> poNames)
	{
		errorCount = 0;
		
		if (poList.isEmpty() && poNames.isEmpty())
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
			
			for (String name: poNames)
			{
				for (ProofObligation po: all)
				{
					if (po.location.module.matches(name) || po.name.matches(name))
					{
						chosen.add(po);
					}
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
				return ClassMapper.getInstance(INNode.MAPPINGS).convertLocal(tcexp);
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
	
	public List<INBindingOverride> getINBindList(INExpression inexp)
	{
		return inexp.apply(new TypeBindFinder(), null);
	}
	
	public StrategyResults getValues(ProofObligation po)
	{
		Map<String, ValueList> union = new HashMap<String, ValueList>();
		
		if (!po.isCheckable)
		{
			return new StrategyResults();
		}
		
		INExpression poexp = getINExpression(po);
		List<INBindingOverride> binds = getINBindList(poexp);
		Context ctxt = Interpreter.getInstance().getInitialContext();
		ctxt = addSelf(po, ctxt);
		IterableContext ictxt = addTypeParams(po, ctxt);
		boolean hasAllValues = false;
		long before = System.currentTimeMillis();
		INBindingGlobals globals = INBindingGlobals.getInstance();
		globals.setTimeout(timeout);	// Strategies can use this
		
		while (ictxt.hasNext())
		{
			ictxt.next();
			
			for (QCStrategy strategy: strategies)
			{
				verbose("Invoking %s strategy\n", strategy.getName());
				StrategyResults sresults = strategy.getValues(po, binds, ictxt);
				
				if (sresults.provedBy != null || sresults.disprovedBy != null)	// No need to go further
				{
					verbose("Obligation (dis)proved by %s\n", strategy.getName());
					sresults.setDuration(System.currentTimeMillis() - before);
					sresults.setBinds(binds);
					sresults.setInExpression(poexp);
					return sresults;
				}

				Map<String, ValueList> cexamples = sresults.counterexamples;

				if (cexamples.isEmpty())
				{
					verbose("No bindings returned by %s\n", strategy.getName());
				}
				else
				{
					for (String bind: cexamples.keySet())
					{
						ValueList values = cexamples.get(bind);
						verbose("%s returned %d values for %s\n", strategy.getName(), values.size(), bind);
						
						if (union.containsKey(bind))
						{
							union.get(bind).addAll(values);
						}
						else
						{
							union.put(bind, values);
						}
					}
				}
				
				hasAllValues = hasAllValues || sresults.hasAllValues;	// At least one has all values
			}
		}
		
		for (INBindingOverride bind: binds)
		{
			ValueList values = union.get(bind.toString());
			
			if (values == null)
			{
				// Generate some values for missing bindings, using the fixed method
				verbose("Generating fixed values for %s\n", bind);
				values = new ValueList();
				values.addAll(bind.getType().apply(new FixedRangeCreator(ictxt), FixedQCStrategy.DEFAULT_LIMIT));
				union.put(bind.toString(), values);
			}
		}
		
		StrategyResults results = new StrategyResults(union, hasAllValues, System.currentTimeMillis() - before);
		results.setBinds(binds);
		results.setInExpression(poexp);
		return results;
	}
	
	public void checkObligation(ProofObligation po, StrategyResults sresults)
	{
		try
		{
			resetErrors();		// Only flag fatal errors
			
			INBindingGlobals globals = INBindingGlobals.getInstance();
			globals.clear();	// Clear before each obligation run

			if (!po.isCheckable)
			{
				infof(POStatus.UNCHECKED, "PO #%d, UNCHECKED\n", po.number);
				return;
			}
			else if (sresults.provedBy != null)
			{
				po.setStatus(POStatus.PROVABLE);
				po.setProvedBy(sresults.provedBy);
				po.setMessage(sresults.message);
				po.setWitness(sresults.witness);
				po.setCounterexample(null);
				infof(POStatus.PROVABLE, "PO #%d, PROVABLE by %s %s %s\n",
						po.number, sresults.provedBy, sresults.message, duration(sresults.duration));
				return;
			}
			else if (sresults.disprovedBy != null)
			{
				po.setStatus(POStatus.FAILED);
				po.setProvedBy(sresults.disprovedBy);
				po.setMessage(sresults.message);
				po.setWitness(null);
				po.setCounterexample(sresults.witness);		// Note: set in counterexample
				
				if (sresults.witness == null)
				{
					infof(POStatus.FAILED, "PO #%d, FAILED by %s %s %s\n",
							po.number, sresults.disprovedBy, sresults.message, duration(sresults.duration));
				}
				else
				{
					infof(POStatus.FAILED, "PO #%d, FAILED by %s %s: Counterexample: %s\n",
							po.number, sresults.disprovedBy, duration(sresults.duration), sresults.witness.toStringLine());
				}
				return;
			}
			
			INExpression poexp = sresults.inExpression;
			List<INBindingOverride> bindings = sresults.binds;

			try
			{
				for (INBindingOverride mbind: bindings)
				{
					ValueList values = sresults.counterexamples.get(mbind.toString());
					
					if (values != null)
					{
						verbose("PO #%d, setting %s, %d values\n", po.number, mbind.toString(), values.size());
						mbind.setBindValues(values);
					}
					else
					{
						errorln("PO #" + po.number + ": No bind values defined for " + mbind);
						errorCount++;
					}
				}
				
				globals.setTimeout(timeout);
				globals.setAllValues(sresults.hasAllValues);
				
				Context ctxt = Interpreter.getInstance().getInitialContext();
				Interpreter.getInstance().setDefaultName(po.location.module);
				
				ctxt = addSelf(po, ctxt);
				IterableContext ictxt = addTypeParams(po, ctxt);
				Value execResult = new BooleanValue(false);
				ContextException execException = null;
				boolean execCompleted = false;
				long before = System.currentTimeMillis();

				try
				{
					verbose("PO #%d, starting evaluation...\n", po.number);
					
					// Suspend annotation execution by the interpreter, because the
					// expressions and statements in the PO can invoke them.
					INAnnotation.suspend(true);
					
					do
					{
						ictxt.next();
						execResult = poexp.eval(ictxt);
					}
					while (ictxt.hasNext() && execResult.boolValue(ctxt));
					
					execCompleted = true;
				}
				catch (ContextException e)
				{
					if (e.rawMessage.equals("Execution cancelled"))
					{
						execResult = null;
					}
					else if (e.number == 4024)	// 'not yet specified' expression reached
					{
						// MAYBE, in effect - execCompleted will be false
						execResult = new BooleanValue(!po.isExistential());
					}
					else
					{
						execResult = new BooleanValue(false);
						execException = e;
					}
				}
				finally
				{
					INAnnotation.suspend(false);
				}
				
				long after = System.currentTimeMillis() + sresults.duration;
				
				if (execResult == null)		// cancelled
				{
					infoln("----");
					printBindings(bindings);
					infoln("----");
					infoln(po);
				}
				else if (execResult instanceof BooleanValue)
				{
					if (execResult.boolValue(ctxt))
					{
						POStatus outcome = null;
						String desc = "";
						po.setWitness(null);
						po.setProvedBy(null);
						
						if (globals.didTimeout())
						{
							outcome = POStatus.TIMEOUT;
						}
						else if (globals.hasMaybe())
						{
							outcome = POStatus.MAYBE;
						}
						else if (po.isExistential())
						{
							outcome = POStatus.PROVABLE;		// An "exists" PO is PROVABLE, if true.
							Context witness = globals.getWitness();
							po.setWitness(witness);
							
							if (witness != null)
							{
								desc = " by witness " + witness.toStringLine();
								po.setProvedBy("witness");
							}
						}
						else if (sresults.hasAllValues && execCompleted)
						{
							outcome = POStatus.PROVABLE;		// All values were tested and passed, so PROVABLE
							desc = " by finite types";
							po.setProvedBy("finite");
						}
						else
						{
							outcome = POStatus.MAYBE;
						}
						
						infof(outcome, "PO #%d, %s%s %s\n", po.number, outcome.toString().toUpperCase(), desc, duration(before, after));
						po.setStatus(outcome);
						po.setCounterexample(null);
						po.setMessage(null);
					}
					else
					{
						if (globals.didTimeout())		// Result would have been true (above), but...
						{
							infof(POStatus.TIMEOUT, "PO #%d, TIMEOUT %s\n", po.number, duration(before, after));
							po.setStatus(POStatus.TIMEOUT);
							po.setCounterexample(null);
							po.setMessage(null);
							po.setWitness(null);
						}
						else if (po.isExistential())	// Principal exp is "exists..."
						{
							if (sresults.hasAllValues)
							{
								infof(POStatus.FAILED, "PO #%d, FAILED (unsatisfiable) %s\n", po.number, duration(before, after));
								po.setStatus(POStatus.FAILED);
								po.setMessage("Unsatisfiable");
								infoln(POStatus.FAILED, "----");
								infoln(POStatus.FAILED, po.toString());
							}
							else
							{
								infof(POStatus.MAYBE, "PO #%d, MAYBE %s\n", po.number, duration(before, after));
								po.setStatus(POStatus.MAYBE);
								po.setMessage(null);
							}
							
							po.setCounterexample(null);
							po.setWitness(null);
						}
						else if (globals.hasMaybe() && execCompleted)
						{
							infof(POStatus.MAYBE, "PO #%d, MAYBE %s\n", po.number, duration(before, after));
							po.setStatus(POStatus.MAYBE);
							po.setMessage(null);
							po.setCounterexample(null);
							po.setWitness(null);
						}
						else
						{
							infof(POStatus.FAILED, "PO #%d, FAILED %s: ", po.number, duration(before, after));
							po.setStatus(POStatus.FAILED);
							printCounterexample(bindings);
							po.setCounterexample(globals.getCounterexample());
							po.setWitness(null);
							
							if (execException != null)
							{
								String msg = "Causes " + execException.getMessage(); 
								infoln(POStatus.FAILED, msg);
								po.setMessage(msg);
							}
							else
							{
								po.setMessage(null);
							}
							
							infoln(POStatus.FAILED, "----");
							infoln(POStatus.FAILED, po.toString());
						}
					}
				}
				else
				{
					String msg = String.format("Error: PO #%d evaluation returns %s?\n", po.number, execResult.kind());
					infoln(msg);
					po.setStatus(POStatus.FAILED);
					po.setCounterexample(null);
					po.setMessage(msg);
					infoln("----");
					printBindings(bindings);
					infoln("----");
					infoln(po);
					errorCount++;
				}
			}
			catch (Exception e)
			{
				String msg = String.format("Exception: PO #%d %s", po.number, e.getMessage());
				infoln(msg);
				po.setStatus(POStatus.FAILED);
				po.setCounterexample(null);
				po.setMessage(msg);
				infoln("----");
				printBindings(bindings);
				infoln("----");
				infoln(po);
				errorCount++;
			}
			finally		// Clear everything, to be safe
			{
				for (INBindingOverride mbind: bindings)
				{
					mbind.setBindValues(null);
				}
				
				globals.clear();
			}
		}
		catch (Exception e)
		{
			errorCount++;
			errorln(e);
		}
	}

	private List<String> strategyNames(List<String> arglist)
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
						
						if (qca.qcTypes != null)	// ie. not new C(args)
						{
							int index = 0;
							
							for (TCType ptype: qca.qcTypes)
							{
								Map<TCNameToken, Value> map = ictxt.newMap(index++);
								map.put(qca.qcParam.name, new ParameterValue(ptype));
							}
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
	
	private Context addSelf(ProofObligation po, Context ctxt)
	{
		if (Settings.dialect != Dialect.VDM_SL)
		{
			try
			{
				if (po.annotations != null)
				{
					for (POAnnotation a: po.annotations)
					{
						if (a instanceof POQuickCheckAnnotation)
						{
							POQuickCheckAnnotation qca = (POQuickCheckAnnotation)a;
							
							if (qca.qcConstructor != null)
							{
								// create object and return it... how?
								break;
							}
						}
					}
				}

				ClassInterpreter in = ClassInterpreter.getInstance();
				INClassDefinition classdef = in.getDefaultClass();
				ObjectValue object = classdef.newInstance(null, null, ctxt);

				ctxt = new ObjectContext(
						classdef.name.getLocation(), classdef.name.getName() + "()",
						ctxt, object);

				ctxt.put(classdef.name.getSelfName(), object);
			}
			catch (ValueException e)
			{
				// Use ctxt unchanged?
			}
		}
		
		return ctxt;
	}

	private void printBindings(List<INBindingOverride> bindings)
	{
		int MAXVALUES = 10;
		
		for (INBindingOverride bind: bindings)
		{
			infof("%s = [", bind);
			
			ValueList list = bind.getBindValues();
			int max = (list.size() > MAXVALUES) ? MAXVALUES : list.size();
			String sep = "";
			
			for (int i=0; i<max; i++)
			{
				infof("%s%s", sep, list.get(i).toShortString(20));
				sep = ", ";
			}
			
			if (max < list.size())
			{
				infof("... (%d values)]\n", list.size());
			}
			else
			{
				infof("]\n");
			}
		}
	}

	private void printCounterexample(List<INBindingOverride> bindings)
	{
		if (bindings.isEmpty())
		{
			infoln(POStatus.FAILED, "Obligation is always false");
		}
		else
		{
			Context path = INBindingGlobals.getInstance().getCounterexample();
			String cex = stringOfContext(path);
			
			if (cex == null)
			{
				infoln(POStatus.FAILED, "No counterexample");
			}
			else
			{
				infoln(POStatus.FAILED, "Counterexample: " + cex);
			}
		}
	}

	private String stringOfContext(Context path)
	{
		if (path == null || path.isEmpty())
		{
			return null;
		}
		
		StringBuilder result = new StringBuilder();
		String sep = "";
		Context ctxt = path;
		
		while (ctxt.outer != null)
		{
			for (TCNameToken name: ctxt.keySet())
			{
				result.append(sep);
				result.append(name);
				result.append(" = ");
				result.append(ctxt.get(name));
				sep = ", ";
			}
			
			ctxt = ctxt.outer;
		}
		
		return result.toString();
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

	public void printHelp(String USAGE)
	{
		println(USAGE);
		println("");
		println("  -?|-help           - show command help");
		println("  -q|-v              - run with minimal or verbose output");
		println("  -t <secs>          - timeout in secs");
		println("  -i <status>        - only show this result status");
		println("  -s <strategy>      - enable this strategy (below)");
		println("  -<strategy:option> - pass option to strategy");
		println("  PO# numbers        - only process these POs");
		println("  PO# - PO#          - process a range of POs");
		println("  <pattern>          - process PO names or modules matching");
		println("");
		println("Enabled strategies:");
		
		for (QCStrategy strategy: getEnabledStrategies())
		{
			println("  " + strategy.help());
		}
		
		if (!getDisabledStrategies().isEmpty())
		{
			println("");
			println("Disabled strategies (add with -s <name>):");
			
			for (QCStrategy strategy: getDisabledStrategies())
			{
				println("  " + strategy.help());
			}
		}
	}
}
