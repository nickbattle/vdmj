/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.typechecker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.ConsoleWriter;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.messages.VDMWarning;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionSet;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.statements.TCExternalClause;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.statements.visitors.TCStatementOpCallFinder;


/**
 * The abstract root of all type checker classes.
 */

abstract public class TypeChecker
{
	private static List<VDMError> errors = new Vector<VDMError>();
	private static List<VDMWarning> warnings = new Vector<VDMWarning>();
	private static VDMMessage lastMessage = null;
	private static boolean suspended = false;
	private static int MAX = Properties.tc_max_errors;
	
	public TypeChecker()
	{
		clearErrors();
	}

	abstract public void typeCheck();
	
	/**
	 * Check for cyclic dependencies between the free variables that definitions depend
	 * on and the definition of those variables.
	 */
	protected void cyclicDependencyCheck(TCDefinitionList defs)
	{
		if (Properties.tc_skip_cyclic_check)
		{
			return;		// For now, to allow us to skip if there are issues.
		}
		
		if (getErrorCount() > 0)
		{
			return;		// Can't really check everything until it's clean
		}
		
		Map<TCNameToken, TCNameSet> dependencies = new HashMap<TCNameToken, TCNameSet>();
		TCNameSet skip = new TCNameSet();
		Environment globals = new FlatEnvironment(defs, null);

    	for (TCDefinition def: defs)
    	{
    		Environment empty = new FlatEnvironment(new TCDefinitionList());
			TCNameSet initdeps = def.getDependencies(globals, empty, new AtomicBoolean(false));
			
			if (!initdeps.isEmpty())
			{
    			for (TCNameToken name: def.getVariableNames())
    			{
    				dependencies.put(name.getExplicit(true), initdeps);
    				
    				if (Settings.verbose)
    				{
    					Console.out.println(name.getExplicit(true) + " => " + initdeps);

    					for (TCNameToken freevar: initdeps)
    					{
    						TCDefinition fdef = globals.findName(freevar, NameScope.ANYTHING);
    						
    						if (fdef != null && fdef.name != null &&
    							name.getLocation().isLater(fdef.name.getLocation()))
    						{
    							Console.out.println("WARNING: " + freevar + " declared after " + name);
    						}
    					}
    				}
    			}
			}
			
			// Skipped definition names occur in the cycle path, but are not checked
			// for cycles themselves, because they are not "initializable".
			
			if (def.isTypeDefinition() || def.isOperation())
			{
				if (def.name != null)
				{
					skip.add(def.name);
				}
			}
    	}
    	
		for (TCNameToken sought: dependencies.keySet())
		{
			if (!skip.contains(sought))
			{
    			Stack<TCNameToken> stack = new Stack<TCNameToken>();
    			stack.push(sought);
    			
    			if (reachable(sought, dependencies.get(sought), dependencies, stack))
    			{
    	    		report(3355, "Cyclic dependency detected for " + sought, sought.getLocation());
    	    		detail("Cycle", stack.toString());
    			}
    			
    			stack.pop();
			}
		}
	}

	/**
	 * Return true if the name sought is reachable via the next set of names passed using
	 * the dependency map. The stack passed records the path taken to find a cycle.
	 */
	private boolean reachable(TCNameToken sought, TCNameSet nextset,
		Map<TCNameToken, TCNameSet> dependencies, Stack<TCNameToken> stack)
	{
		if (nextset == null)
		{
			return false;
		}
		
		if (nextset.contains(sought))
		{
			stack.push(sought);
			return true;
		}
		
		for (TCNameToken nextname: nextset)
		{
			if (stack.contains(nextname))	// Been here before!
			{
				return false;
			}
			
			stack.push(nextname);
			
			if (reachable(sought, dependencies.get(nextname), dependencies, stack))
			{
				return true;
			}
			
			stack.pop();
		}
		
		return false;
	}

	/**
	 * Populate the transitiveUpdates field of operation definitions, to include every
	 * localUpdates set by itself and the operations it calls transitively.
	 */
	protected void populateTransitiveUpdates(TCDefinitionList alldefs)
	{
		if (getErrorCount() > 0)
		{
			return;		// Can't populate everything until it's clean
		}

		Environment globals = new FlatEnvironment(alldefs, null);

		for (TCDefinition def: alldefs)
		{
			if (def instanceof TCExplicitOperationDefinition)
			{
				TCExplicitOperationDefinition exop = (TCExplicitOperationDefinition)def;
				populateTransitiveUpdates(exop, globals);
			}
			else if (def instanceof TCImplicitOperationDefinition)
			{
				TCImplicitOperationDefinition imop = (TCImplicitOperationDefinition)def;
				populateTransitiveUpdates(imop, globals);
			}
		}

		// If strict mode enabled, check for various things using the transitive sets.

		if (Settings.strict)
		{
			checkTransitiveExtClauses(alldefs);
			checkTransitivePures(alldefs);
		}
		
		return;
	}

	/**
	 * Check that "pure" operations have empty transitive update sets.
	 */
	private void checkTransitivePures(TCDefinitionList alldefs)
	{
		for (TCDefinition def: alldefs)
		{
			if (def.accessSpecifier.isPure)
			{
				TCNameSet transitiveUpdates = null;

				if (def instanceof TCImplicitOperationDefinition)
				{
					TCImplicitOperationDefinition imop = (TCImplicitOperationDefinition)def;
					transitiveUpdates = imop.transitiveUpdates;
				}
				else if (def instanceof TCExplicitOperationDefinition)
				{
					TCExplicitOperationDefinition exop = (TCExplicitOperationDefinition)def;
					transitiveUpdates = exop.transitiveUpdates;
				}

				if (transitiveUpdates != null && !transitiveUpdates.isEmpty())
				{
					warning(5047, "Strict: pure operation updates state", def.location);
					detail("Updates", transitiveUpdates.toString());
				}
			}
		}
	}

	/**
	 * Check for operations that have "ext" clauses which are narrower than their
	 * transitive update sets.
	 */
	private void checkTransitiveExtClauses(TCDefinitionList alldefs)
	{
		for (TCDefinition def: alldefs)
		{
			if (def instanceof TCImplicitOperationDefinition)
			{
				TCImplicitOperationDefinition imop = (TCImplicitOperationDefinition)def;

				if (imop.externals != null && imop.transitiveUpdates != null)
				{
					for (TCExternalClause ext: imop.externals)
					{
						if (ext.mode.is(Token.WRITE))
						{
							TCNameSet updates = new TCNameSet();
							updates.addAll(imop.transitiveUpdates);
							updates.removeAll(ext.identifiers);

							if (!updates.isEmpty())
							{
								warning(5046, "Strict: ext clause missing 'wr' updates", imop.location);
								detail("Missing", updates.toString());
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Populate transitive calls and updates. The method starts by populating transitiveCalls,
	 * by recursing into the call graph. Then it loops through the transitiveCalls, adding
	 * the localUpdates to the transitiveUpdates for this operation.
	 */
	private void populateTransitiveUpdates(Environment globals, TCStatement body,
		TCDefinitionSet transitiveCalls, TCNameSet transitiveUpdates, TCNameSet localUpdates)
	{
		TCDefinitionSet exCalls = new TCDefinitionSet();
		TCDefinitionSet imCalls = new TCDefinitionSet();

		for (TCNameToken op: body.apply(new TCStatementOpCallFinder(), globals))
		{
			TCDefinition def = globals.findName(op, NameScope.GLOBAL);

			if (def instanceof TCExplicitOperationDefinition)
			{
				exCalls.add(def);
			}
			else if (def instanceof TCImplicitOperationDefinition)
			{
				imCalls.add(def);
			}
		}

		transitiveCalls.addAll(exCalls);	// add local calls
		transitiveCalls.addAll(imCalls);

		for (TCDefinition def: exCalls)
		{
			TCExplicitOperationDefinition exop = (TCExplicitOperationDefinition)def;
			populateTransitiveUpdates(exop, globals);
			transitiveCalls.addAll(exop.transitiveCalls);
		}

		for (TCDefinition def: imCalls)
		{
			TCImplicitOperationDefinition imop = (TCImplicitOperationDefinition)def;
			populateTransitiveUpdates(imop, globals);
			transitiveCalls.addAll(imop.transitiveCalls);
		}

		// transitiveCalls is now complete for opdef and all of the ops it calls. So now
		// go through the transitiveCalls to add the localUpdates for each to
		// transitiveUpdates.

		transitiveUpdates.addAll(localUpdates);		// add local updates

		for (TCDefinition def: transitiveCalls)
		{
			if (def instanceof TCExplicitOperationDefinition)
			{
				TCExplicitOperationDefinition exop = (TCExplicitOperationDefinition)def;
				transitiveUpdates.addAll(exop.localUpdates);
			}
			else if (def instanceof TCImplicitOperationDefinition)
			{
				TCImplicitOperationDefinition imop = (TCImplicitOperationDefinition)def;
				transitiveUpdates.addAll(imop.localUpdates);
			}
		}
	}

	/**
	 * Populate the transitive updates of one explicit opcall.
	 */
	private void populateTransitiveUpdates(TCExplicitOperationDefinition opdef, Environment globals)
	{
		if (opdef.transitiveCalls == null)		// Not done yet
		{
			opdef.transitiveCalls = new TCDefinitionSet();
			opdef.transitiveUpdates = new TCNameSet();

			populateTransitiveUpdates(globals, opdef.body,
				opdef.transitiveCalls, opdef.transitiveUpdates, opdef.localUpdates);
		}
	}

	/**
	 * Populate the transitive updates of one implicit opcall.
	 */
	private void populateTransitiveUpdates(TCImplicitOperationDefinition opdef, Environment globals)
	{
		if (opdef.transitiveCalls == null)		// Not done yet
		{
			opdef.transitiveCalls = new TCDefinitionSet();
			opdef.transitiveUpdates = new TCNameSet();

			if (opdef.body != null)
			{
				populateTransitiveUpdates(globals, opdef.body,
					opdef.transitiveCalls, opdef.transitiveUpdates, opdef.localUpdates);
			}
			else if (opdef.externals == null && opdef.stateDefinition != null)
			{
				// No body, so if there is no ext clause, we assume all state is modifiable
				opdef.transitiveUpdates = opdef.stateDefinition.getStateNames();
			}
		}
	}

	/**
	 * Report a TC error.
	 */
	public static void report(int number, String problem, LexLocation location)
	{
		if (suspended) return;	
		VDMError error = new VDMError(number, problem, location);

		if (!errors.contains(error))
		{
			if (errors.size() < MAX)
			{
				errors.add(error);
				lastMessage = error;
	
	    		if (errors.size() == MAX)
	    		{
	    			errors.add(new VDMError(10, "Too many type checking errors", location));
	    			// throw new InternalException(10, "Too many type checking errors");
	    		}
			}
		}
		else
		{
			lastMessage = null;
		}
	}

	/**
	 * Report a TC warning.
	 */
	public static void warning(int number, String problem, LexLocation location)
	{
		if (suspended) return;
		VDMWarning warning = new VDMWarning(number, problem, location);

		if (!warnings.contains(warning))
		{
			if (warnings.size() < MAX)
			{
				warnings.add(warning);
				lastMessage = warning;

				if (warnings.size() == MAX)
	    		{
	    			warnings.add(new VDMWarning(10, "Too many type checking warnings", location));
	    			// throw new InternalException(10, "Too many type checking errors");
	    		}
			}
		}
		else
		{
			lastMessage = null;
		}
	}

	public static void detail(String tag, Object obj)
	{
		if (suspended) return;

		if (lastMessage != null)
		{
			lastMessage.add(tag + ": " + obj);
		}
	}

	public static void detail2(String tag1, Object obj1, String tag2, Object obj2)
	{
		detail(tag1, obj1);
		detail(tag2, obj2);
	}

	public static void clearErrors()
	{
		errors.clear();
		warnings.clear();
	}

	public static int getErrorCount()
	{
		return errors.size();
	}

	public static int getWarningCount()
	{
		return warnings.size();
	}

	public static List<VDMError> getErrors()
	{
		return errors;
	}

	public static List<VDMWarning> getWarnings()
	{
		return warnings;
	}

	public static void printErrors(ConsoleWriter out)
	{
		for (VDMError e: errors)
		{
			out.println(e.toString());
		}
	}

	public static void printWarnings(ConsoleWriter out)
	{
		for (VDMWarning w: warnings)
		{
			out.println(w.toString());
		}
	}

	public static void suspend(boolean suspend)
	{
		suspended  = suspend;
	}
}
