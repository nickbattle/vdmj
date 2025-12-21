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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package quickcheck;

import static com.fujitsu.vdmj.plugins.PluginConsole.errorln;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;
import static quickcheck.commands.QCConsole.infof;
import static quickcheck.commands.QCConsole.infoln;
import static quickcheck.commands.QCConsole.verbose;
import static quickcheck.commands.QCConsole.verboseln;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.PatternSyntaxException;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.lex.LexBooleanToken;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.annotations.INAnnotation;
import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.in.expressions.INBooleanLiteralExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INBindingGlobals;
import com.fujitsu.vdmj.in.patterns.INBindingOverride;
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
import com.fujitsu.vdmj.values.UndefinedValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

import quickcheck.annotations.IterableContext;
import quickcheck.annotations.po.POQuickCheckAnnotation;
import quickcheck.strategies.FixedQCStrategy;
import quickcheck.strategies.QCStrategy;
import quickcheck.strategies.StrategyResults;
import quickcheck.visitors.ExpressionTypeBindOverrider;
import quickcheck.visitors.FixedRangeCreator;

public class QuickCheck
{
	public static final long DEFAULT_TIMEOUT = 5000;	// 5s timeout
	
	private int errorCount = 0;
	private List<QCStrategy> strategies = null;		// Configured to be used
	private List<QCStrategy> disabled = null;		// Known, but not to be used
	private ProofObligationList chosenPOs = null;

	private boolean undefinedEvals = true;			// Use undefinedEval for some bools, by default
	
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
					QCStrategy instance = (QCStrategy) ctor.newInstance(argv);
					
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
						
						if (argv != null && argvSize != argv.size())
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
				catch (InvocationTargetException e)
				{
					errorln("Strategy " + classname + ": " + e.getTargetException());
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
			verbose("------------------------ Initializing %s strategy\n", strategy.getName());

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

		verbose("------------------------ Initialized all strategies\n");
		return doChecks;
	}
	
	private List<QCStrategy> getEnabledStrategies()
	{
		return strategies;
	}
	
	private List<QCStrategy> getDisabledStrategies()
	{
		return disabled;
	}
	
	public ProofObligationList getChosenPOs()
	{
		return chosenPOs;
	}
	
	public ProofObligationList getPOs(ProofObligationList all, List<Integer> poList, List<String> poNames)
	{
		errorCount = 0;
		
		if (poList.isEmpty() && poNames.isEmpty())
		{
			chosenPOs = new ProofObligationList();
			String def = Interpreter.getInstance().getDefaultName();
			
			for (ProofObligation po: all)
			{
				if (po.location.module.equals(def))
				{
					chosenPOs.add(po);
				}
			}
			
			return chosenPOs;	// No PO#s specified, so use default class/module's POs
		}
		else
		{
			chosenPOs = new ProofObligationList();
			
			for (Integer n: poList)
			{
				if (n == 0)
				{
					continue;	// Zero used for dummy "Missing POs"
				}
				else if (n > 0 && n <= all.size())
				{
					chosenPOs.add(all.get(n-1));
				}
				else
				{
					errorln("PO# " + n + " unknown. Must be between 1 and " + all.size());
					errorCount++;
				}
			}
			
			for (String name: poNames)
			{
				try
				{
					for (ProofObligation po: all)
					{
						if (po.location.module.matches(name) || po.name.matches(name))
						{
							chosenPOs.add(po);
						}
					}
				}
				catch (PatternSyntaxException e)
				{
					errorln("Pattern syntax error: " + name);
					errorCount++;
				}
			}
			
			return errorCount > 0 ? null : chosenPOs;
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
				return new INBooleanLiteralExpression(LexBooleanToken.TRUE);
			}
		}
		catch (Exception e)
		{
			errorln("getINExpression: " + e);
			return new INBooleanLiteralExpression(LexBooleanToken.FALSE);
		}
	}
	
	public List<INBindingOverride> getINBindList(INExpression inexp)
	{
		return inexp.apply(new ExpressionTypeBindOverrider(), null);
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

		try
		{
			// This is used by some strategies to find the module state
			Interpreter.getInstance().setDefaultName(po.location.module);
		}
		catch (Exception e)
		{
			// Can't happen?
		}
		
		while (ictxt.hasNext())
		{
			ictxt.next();
			
			for (QCStrategy strategy: strategies)
			{
				verbose("------------------------ Invoking %s strategy on PO #%d\n", strategy.getName(), po.number);
				StrategyResults sresults = null;
				
				try
				{
					// Suspend annotation execution by the interpreter, because the
					// expansion of invariant types can invoke them.
					INAnnotation.suspend(true);

					sresults = strategy.getValues(po, binds, ictxt);
				}
				catch (QuickCheckException e)
				{
					po.markUnchecked(e.getMessage());	// Controlled abort, unsets isCheckable
					break;
				}
				catch (Throwable t)
				{
					errorln("Strategy " + strategy.getName() + " failed to generate values: " + t);
					continue;
				}
				finally
				{
					INAnnotation.suspend(false);
				}
				
				if (sresults.updater != null)	// No need to go further
				{
					verbose("Obligation resolved by %s updater\n", strategy.getName());
					sresults.setDetails(poexp, binds);
					return sresults;
				}

				Map<String, ValueList> cexamples = sresults.possibleValues;

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
				
				hasAllValues = hasAllValues || sresults.hasAllValues;	// At least one strategy has all values
			}
			
			verboseln("------------------------- Strategies complete.");
		}
		
		if (po.isCheckable)		// Can be unset by a QuickCheckException
		{
			for (INBindingOverride bind: binds)
			{
				ValueList values = union.get(bind.toString());
				
				if (values == null)
				{
					// Generate some values for missing bindings, using the fixed method
					verbose("Generating fixed values for %s\n", bind);
					values = new ValueList();
					values.addAll(bind.getType(ictxt).apply(new FixedRangeCreator(ictxt), FixedQCStrategy.DEFAULT_LIMIT));
					union.put(bind.toString(), values);
				}
			}
		}
		
		StrategyResults results = new StrategyResults(union, hasAllValues);
		results.setDetails(poexp, binds);
		return results;
	}
	
	public void checkObligation(ProofObligation po, StrategyResults sresults)
	{
		try
		{
			verbose("------------------------ Checking PO #%d\n", po.number);

			if (!po.isCheckable)	// Probably set via QuickCheckExceptions earlier
			{
				verbose("PO is UNCHECKED");
				return;
			}

			resetErrors();			// Only flag fatal errors
			po.clearAnalysis();		// Clears fields to be set by QC

			if (sresults.updater != null)
			{
				verbose("Calling updater");
				sresults.updater.updateProofObligation(po);	
			}
			else
			{
				verbose("Trying possible values from strategies\n");
				tryPossibleValues(po, sresults);
			}
		}
		catch (Exception e)
		{
			po.setStatus(POStatus.FAILED);
			po.setMessage(e.getMessage());
			errorCount++;
		}
	}

	private void tryPossibleValues(ProofObligation po, StrategyResults sresults) throws Exception
	{
		INBindingGlobals globals = INBindingGlobals.getInstance();
		globals.clear();		// Clear before each obligation run

		Value execResult = new BooleanValue(false);
		ContextException execException = null;
		boolean execCompleted = false;
		boolean timedOut = false;

		try
		{
			for (INBindingOverride mbind: sresults.binds)
			{
				ValueList values = sresults.possibleValues.get(mbind.toString());
				
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
			
			globals.setAllValues(sresults.hasAllValues);
			Context ctxt = Interpreter.getInstance().getInitialContext();
			Interpreter.getInstance().setDefaultName(po.location.module);
			
			ctxt = addSelf(po, ctxt);
			IterableContext ictxt = addTypeParams(po, ctxt);

			verbose("PO #%d, starting evaluation...\n", po.number);
			
			// Suspend annotation execution by the interpreter, because the
			// expressions and statements in the PO can invoke them.
			INAnnotation.suspend(true);

			// Allow some error cases in booleans to return undefined?
			Properties.in_undefined_evals = undefinedEvals;
			
			do
			{
				ictxt.next();
				execResult = sresults.inExpression.eval(ictxt);
			}
			while (ictxt.hasNext() && execResult.isDefined() && execResult.boolValue(ctxt));
			
			execCompleted = true;
		}
		catch (ContextException e)
		{
			verbose("PO #%d, exception %s.\n", po.number, e.getMessage());

			if (e.isUserCancel())
			{
				execResult = new BooleanValue(false);
				timedOut = true;
			}
			else if (maybeException(e))
			{
				// MAYBE, in effect - execCompleted will be false
				execResult = new BooleanValue(!po.isExistential());
			}
			else if (internalException(e))	// PP/RT issues
			{
				execResult = new UndefinedValue();	// Gives MAYBE
				po.clearAnalysis();
				po.markUnchecked(ProofObligation.UNCHECKED_VDMPP);
			}
			else
			{
				execResult = new BooleanValue(false);
				execException = e;
				globals.setCounterexample(e.ctxt);
			}
		}
		catch (QuickCheckException e)
		{
			execResult = new UndefinedValue();		// Gives MAYBE
			po.clearAnalysis();
			po.markUnchecked(e.getMessage());
		}
		catch (Throwable e)
		{
			if (internalException(e))
			{
				execResult = new UndefinedValue();	// Gives MAYBE
				po.clearAnalysis();
				po.markUnchecked(ProofObligation.TOO_COMPLEX);
			}
			else
			{
				execResult = new BooleanValue(false);
				execException = new ContextException(78, "Exception: " + e.getMessage(), po.location, null);
			}
		}
		finally
		{
			analyseResult(po, sresults, globals,
				execResult, execException, execCompleted, timedOut);

			// Clear everything, to be safe
			
			for (INBindingOverride mbind: sresults.binds)
			{
				mbind.setBindValues(null);
			}
			
			INBindingGlobals.getInstance().clear();
			verbose("PO #%d, stopped evaluation.\n", po.number);
			INAnnotation.suspend(false);
			Properties.in_undefined_evals = false;		// Always disable on restore
		}
	}

	/**
	 * These exceptions should cause a MAYBE result, because they indicate that something
	 * about the specification is incomplete or "a known issue"
	 */
	private boolean maybeException(ContextException e)
	{
		switch (e.number)
		{
			case 4024:	// 'not yet specified' expression reached
			case 4051:	// Cannot apply implicit function
			case 4:		// 'Cannot get bind values for type' (from a func call?)
				return true;

			default:
				return false;
		}
	}

	/**
	 * Until we can solve some tricky problems with VDM++/RT state handling, POs from these
	 * dialects can raise problems due to missing func/op fields in objects and some other
	 * specific error types. These are checked here and result in the PO being marked as
	 * MAYBE, if the dialect is not SL.
	 */
	private boolean internalException(ContextException e)
	{
		if (Settings.dialect == Dialect.VDM_SL)
		{
			return false;	// Special cases are only PP and RT
		}

		if (e.number >= 4089 && e.number <= 4105)	// Can't get <type>> value of undefined
		{
			return e.rawMessage.endsWith("undefined");
		}

		if (e.number == 4172 && e.rawMessage.endsWith(", undefined"))
		{
			return true;	// Error 4172: Values cannot be compared: <type>, undefined
		}

		switch (e.number)
		{
			case 4006:	// Type <class> has no field <opname>
			case 4034:	// Name 'opname(args)' not in scope
			case 4132:	// Using undefined value
			case 4177:	// Not in a SchedulableThread
				return true;

			default:
				return false;
		}
	}

	/**
	 * These problems indicate the PO analysis is too complicated.
	 */
	private boolean internalException(Throwable e)
	{
		return
			e instanceof OutOfMemoryError ||
			e instanceof StackOverflowError;
	}

	private void analyseResult(ProofObligation po, StrategyResults sresults, INBindingGlobals globals,
		Value execResult, ContextException execException, boolean execCompleted, boolean timedOut)
	{
		if (execResult.isUndefined())
		{
			po.setStatus(POStatus.MAYBE);
		}
		else if (execResult instanceof BooleanValue)
		{
			BooleanValue result = (BooleanValue)execResult;
			
			if (result.value)	// ie. true
			{
				if (timedOut)	// Result would be false (below), but...
				{
					po.setStatus(POStatus.TIMEOUT);
				}
				else if (po.isExistential())
				{
					po.setStatus(POStatus.PROVABLE);		// An "exists" PO is PROVABLE, if true.
					Context witness = globals.getWitness();
					
					if (witness != null)
					{
						po.setWitness(witness);
						po.setQualifier("by witness");
						po.setProvedBy("witness");
						po.setExplanation(getExplanation(po, witness, null, result.value));
					}
				}
				else if (sresults.hasAllValues && execCompleted)
				{
					po.setStatus(POStatus.PROVABLE);		// All values were tested and passed, so PROVABLE
					
					if (sresults.binds.isEmpty())
					{
						po.setQualifier("in all cases");
						po.setProvedBy("fixed");
					}
					else
					{
						po.setQualifier("by finite types");
						po.setProvedBy("finite");
					}
				}
				else
				{
					po.setStatus(POStatus.MAYBE);
				}
				
				applyHeuristics(po);
			}
			else
			{
				if (timedOut)
				{
					po.setStatus(POStatus.TIMEOUT);
				}
				else if (po.isExistential())	// Principal exp is "exists..."
				{
					if (sresults.hasAllValues)
					{
						po.setStatus(POStatus.FAILED);
						po.setQualifier("unsatisfiable");
					}
					else
					{
						po.setStatus(POStatus.MAYBE);
					}
				}
				else
				{
					po.setStatus(POStatus.FAILED);
					
					if (sresults.binds.isEmpty())		// Failed with no binds - eg. Test() with no params
					{
						po.setCounterexample(new Context(po.location, "Empty", null));
					}
					else
					{
						Context path = globals.getCounterexample();
						po.setCounterexample(path);
						po.setExplanation(getExplanation(po, path, execException, result.value));
					}
				}
				
				applyHeuristics(po);
			}
		}
		else
		{
			po.setStatus(POStatus.FAILED);
			po.setMessage("PO evaluation returns " + execResult.kind());
			errorCount++;
		}
	}
	
	/**
	 * Generate a list of context strings, for every layer of the path, in execution order.
	 * Interleave these with the source of the PO and add any exceptions at the end.
	 */
	private String getExplanation(ProofObligation po,
		Context path, ContextException execException, boolean returns)
	{
		TreeMap<Integer, String> contexts = new TreeMap<Integer, String>();
		String[] source = po.source.split("\n");
		StringBuilder explanation = new StringBuilder();
		int lastLine = 0;

		// Note that PO sources are in file "console".

		while (path != null)
		{
			if (path.outer != null)		// Don't add global frame
			{
				if (path.title.equals("type params"))
				{
					if (!path.isEmpty())
					{
						explanation.append("Types: ");
						explanation.append(stringOfContext(path));
						explanation.append("\n");
					}
				}
				else if (path.location.file.getName().equals("console"))
				{
					String ctxt = stringOfContext(path);

					if (ctxt != null)	// Not an empty path
					{
						int line = path.location.startLine;

						if (contexts.containsKey(line))		// eg. exists, all on one line
						{
							ctxt = ctxt + ", " + contexts.get(line);
						}
						
						contexts.put(line, ctxt);

						if (line > lastLine) lastLine = line;
					}
				}
			}

			path = path.outer;
		}

		// List each line of the PO, together with any context values or exceptions
		int exLine = 0;

		if (execException != null &&
			execException.location.file.getName().equals("console") &&
			execException.location.startLine <= source.length)
		{
			exLine = execException.location.startLine;
		}

		String indent = source[0].startsWith("(") ? " " : "";
		int lineNo = 1;

		for (String poLine: source)
		{
			explanation.append(poLine);
			explanation.append("\n");

			if (contexts.containsKey(lineNo))
			{
				explanation.append(indent);
				explanation.append("--> ");
				explanation.append(contexts.get(lineNo));
				explanation.append("\n");
			}
			
			if ((exLine == lineNo) ||
				(exLine == 0 && execException != null && lineNo == lastLine))
			{
				explanation.append(indent);
				explanation.append("--> ");
				explanation.append(execException.toString());
				explanation.append("\n");
			}

			indent = indent + "  ";		// Matches the PO getSource indent
			lineNo++;
		}

		if (execException == null)
		{
			indent = indentOf(source[source.length - 1]);
			explanation.append(indent);
			explanation.append("--> returns ");
			explanation.append(returns);
			explanation.append("\n");
		}

		return explanation.toString();
	}

	/**
	 * The whitespace prefix to a line.
	 */
	private String indentOf(String s)
	{
		StringBuilder sb = new StringBuilder();

		while (s.charAt(sb.length()) == ' ')
		{
			sb.append(" ");
		}

		return sb.toString();
	}

	private void applyHeuristics(ProofObligation po)
	{
		if (po.status == POStatus.MAYBE)
		{
			for (QCStrategy strategy: strategies)
			{
				strategy.maybeHeuristic(po);
			}
		}
	}

	private List<String> strategyNames(List<String> arglist)
	{
		List<String> names = new Vector<String>();
		
		if (arglist != null && !arglist.isEmpty())
		{
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
		}
		
		return names;
	}

	private IterableContext addTypeParams(ProofObligation po, Context ctxt)
	{
		IterableContext ictxt = new IterableContext(po.location, "type params", ctxt);

		if (po.getTypeParams() != null)
		{
			if (po.getAnnotations() != null)
			{
				for (POAnnotation a: po.getAnnotations())
				{
					if (a instanceof POQuickCheckAnnotation)
					{
						POQuickCheckAnnotation qca = (POQuickCheckAnnotation)a;
						
						// A map of @T names to lists of types is created from each @QuickCheck.
						// The IterableContexts are then vertical "slices" through this, selecting
						// all of the first types, then all of the second and so on. Lastly,
						// any missing cases are filled in with "real" types.
						
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
			
			for (TCType type: po.getTypeParams())
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
				ClassInterpreter in = ClassInterpreter.getInstance();
				INClassDefinition classdef = in.getDefaultClass();
				ObjectValue object = null;

				if (po.getAnnotations() != null)
				{
					for (POAnnotation a: po.getAnnotations())
					{
						if (a instanceof POQuickCheckAnnotation)
						{
							POQuickCheckAnnotation qca = (POQuickCheckAnnotation)a;
							
							if (qca.tc != null)
							{
								if (qca.newexp == null)		// cached
								{
									qca.newexp = ClassMapper.getInstance(INNode.MAPPINGS).convert(qca.tc.qcConstructor);
								}
								
								object = (ObjectValue) qca.newexp.eval(in.getInitialContext());
								break;
							}
						}
					}
				}

				if (object == null)
				{
					object = classdef.newInstance(null, null, ctxt);
	
					ctxt = new ObjectContext(
							classdef.name.getLocation(), classdef.name.getName() + "()",
							ctxt, object);
				}
	
				ctxt.put(classdef.name.getSelfName(), object);
			}
			catch (ValueException e)
			{
				// Use ctxt unchanged?
			}
			catch (Exception e)
			{
				// Problem with "new"?
			}
		}
		
		return ctxt;
	}
	
	/**
	 * Produce output (subject to include/quiet flags) for a standard QuickCheck command line.
	 * The format is as follows, with several fields being optional:
	 * 
	 * PO #&lt;number&gt;, &lt;status&gt; &lt;qualifier&gt; in &lt;time&gt;
	 * &lt;message&gt;
	 * &lt;counterexample&gt;|&lt;witness&gt;
	 * ----
	 * &lt;source&gt;
	 * 
	 * For example:
	 * 
	 * PO #1, MAYBE in 0.028s
	 * PO #2, FAILED in 0.003s
	 * Counterexample:
	 * (forall i:nat, s:seq of real &amp; pre_f(i, s) =&gt;
	 *  ==&gt; i = 1, s = [1.25]
	 *   is_nat(s(i)))
	 *   ==&gt; returns false
	 */
	public void printQuickCheckResult(ProofObligation po, double duration, boolean nominal)
	{
		infof("PO #%d, %s", po.number, po.status.toString().toUpperCase());
		
		if (po.qualifier != null && !nominal)
		{
			infof(" %s", po.qualifier);
		}
		
		if (po.status != POStatus.UNCHECKED)
		{
			infof(" in %ss", duration);
		}
		
		infoln("");
		
		if (!nominal)
		{
			if (po.message != null)
			{
				infoln(po.message);
			}
			
			if (po.status == POStatus.FAILED && po.counterexample != null)
			{
				String cex = po.getExplanation();

				if (cex == null)
				{
					cex = stringOfContext(po.counterexample);
				}
				
				if (cex == null)
				{
					infoln("No counterexample");
				}
				else
				{
					infof("Counterexample:\n%s\n%s\n", po.toTitle(), cex);
				}
			}
			
			if (po.status == POStatus.PROVABLE && po.witness != null)
			{
				String wit = po.getExplanation();

				if (wit == null)
				{
					wit = stringOfContext(po.witness);
				}
				
				if (wit == null)
				{
					infoln("No witness");
				}
				else
				{
					infof("Witness:\n%s\n%s\n", po.toTitle(), wit);
				}
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

		for (TCNameToken name: path.keySet())
		{
			if (name.getName().equals("self"))
			{
				continue;	// Confuses the output otherwise
			}
			
			result.append(sep);
			result.append(name);
			result.append(" = ");
			result.append(path.get(name));
			sep = ", ";
		}
		
		return result.toString();
	}

	public void printHelp(String USAGE)
	{
		println(USAGE);
		println("");
		println("  -?|-help           - show command help");
		println("  -q|-v|-n           - run with minimal, verbose, basic output");
		println("  -t <msecs>         - timeout in millisecs");
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

	public void setUndefinedEvals(boolean undefinedEvals)
	{
		this.undefinedEvals = undefinedEvals;
	}
}
