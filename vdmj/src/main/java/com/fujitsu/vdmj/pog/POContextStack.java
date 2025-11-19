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

package com.fujitsu.vdmj.pog;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;
import java.util.Vector;

import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.po.definitions.POClassDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.PODefinitionSet;
import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POInheritedDefinition;
import com.fujitsu.vdmj.po.definitions.POInstanceVariableDefinition;
import com.fujitsu.vdmj.po.definitions.PORenamedDefinition;
import com.fujitsu.vdmj.po.definitions.POStateDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;
import com.fujitsu.vdmj.po.patterns.visitors.POGetMatchingExpressionVisitor;
import com.fujitsu.vdmj.po.statements.POExternalClause;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.util.Utils;

public class POContextStack extends Stack<POContext>
{
	/**
	 * Definitions which have had their ALT paths reduced, due to excessive branching,
	 * to meet the value above.
	 */
	private static PODefinitionSet reducedDefinitions = new PODefinitionSet();
	
	public static PODefinitionSet getReducedDefinitions()
	{
		return reducedDefinitions;
	}

	/**
	 * Clear the reduced definitions globally, for a new POG run.
	 */
	public static void reset()
	{
		reducedDefinitions = new PODefinitionSet();
	}

	/**
	 * The pushAt/popTo methods allow a push to record the current stack size and then
	 * pop items back to that size easily. It is used in operation PO handling, where
	 * persistent contexts (like a state update) are not popped symmetrically.
	 */
	public int pushAt(POContext ctxt)
	{
		int size = this.size();
		push(ctxt);
		return size;
	}

	public int pushAll(POContextStack stack)
	{
		int size = this.size();

		for (POContext item: stack)
		{
			push(item);
		}

		return size;
	}
	
	public void popTo(int size)
	{
		while (size() > size)
		{
			pop();
		}
	}
	
	/**
	 * The popInto/copyInto are designed to be used with POAltContexts, extracting a
	 * part of the context stack into an alternative. If this process encounters a
	 * POReturnContext, the alternative is cleared since it does not play any part
	 * in obligations further down the operation.
	 */
	public void popInto(int size, POContextStack into)
	{
		while (size() > size)
		{
			into.add(0, pop());		// Preserve order
		}
	}
	
	public void copyInto(int size, POContextStack into)
	{
		for (int i=size; i < size(); i++)
		{
			into.add(get(i));		// Preserve order
		}
	}
	
	/**
	 * If the stack contains POAltContext items, these produce alternative substacks that
	 * have to be iterated through, before creating a set of obligations. Note that if
	 * a POReturnContext is encountered at the top level, this immediately returns no
	 * stacks, because this path can play no part in further obligations.
	 */
	public List<POContextStack> getAlternatives()
	{
		// exclude return paths by default
		return getAlternatives(true);
	}
	
	public List<POContextStack> getAlternatives(boolean excludeReturns)
	{
		return getAlternatives(excludeReturns, Properties.pog_max_alt_paths);
	}
	
	private List<POContextStack> getAlternatives(boolean excludeReturns, int limit)
	{
		List<POContextStack> results = new Vector<POContextStack>();
		results.add(new POContextStack());
		
		for (POContext ctxt: this)
		{
			if (ctxt instanceof POAltContext)
			{
				POAltContext alt = (POAltContext)ctxt;
				List<POContextStack> newResults = new Vector<POContextStack>();
				List<POContextStack> dontReturn = new Vector<POContextStack>();
				int remaining = limit;
				
				try
				{
					// First add the returnsEarly originals once, because these won't change
					for (POContextStack original: results)
					{
						if (original.returnsEarly())
						{
							newResults.add(original);
						}
						else
						{
							dontReturn.add(original);
						}				
					}

					// Then append the alternative expansions to the dontReturn remainers
					for (POContextStack substack: alt.alternatives)
					{
						List<POContextStack> subalternatives = substack.getAlternatives(excludeReturns, remaining);

						remaining = remaining - subalternatives.size();
						if (remaining < 1) remaining = 1;

						for (POContextStack alternative: subalternatives)
						{
							for (POContextStack original: dontReturn)
							{
								POContextStack combined = new POContextStack();
								combined.addAll(original);
								combined.addAll(alternative);
								newResults.add(combined);

								if (newResults.size() > limit)
								{
									throw new IllegalArgumentException();	// Too many
								}
							}
						}
					}
				}
				catch (IllegalArgumentException e)		// newResults too large
				{
					if (getDefinition() != null)
					{
						// Add the name to list of definitions that are too complex.
						reducedDefinitions.add(getDefinition());
					}

					// Trim newResults to the limit
					newResults = newResults.subList(0, limit);
				}
				finally		// rebuild the results
				{
					results.clear();
					results.addAll(newResults);
				}
			}
			else
			{
				if (ctxt.stops())
				{
					// An "error" statement is reached, so this control path aborts here and
					// no further obligations are produced.

					return new Vector<POContextStack>();
				}
				else if (ctxt.returnsEarly())
				{
					// This stack plays no part in further obligations, including any
					// alternatives it contains. So immediately return nothing if we
					// are excluding paths that end before they reach the current location.
					
					if (excludeReturns)
					{
						return new Vector<POContextStack>();
					}

					// Else add it as usual...
				}

				for (POContextStack choice: results)
				{
					if (choice.returnsEarly())
					{
						continue;	// skip this choice, as it has already ended
					}
					
					choice.add(ctxt);
				}
			}
		}
		
		return results;
	}

	/**
	 * Calculate the number of alternative paths for the current stack. This is used in
	 * error reporting when an operation is too complex.
	 */
	public long countAlternatives()
	{
		long count = 1;

		for (POContext ctxt: this)
		{
			if (ctxt instanceof POAltContext)
			{
				POAltContext alt = (POAltContext)ctxt;
				long newcount = 0;

				for (POContextStack substack: alt.alternatives)
				{
					newcount += count * substack.countAlternatives();
				}

				count = newcount;
			}
		}

		return count;
	}

	/**
	 * Patch the return points in a context stack. This is to implement "always" behaviour,
	 * where something always happens, even after return points from a block.
	 */
	public boolean patchReturns(POContextStack always)
	{
		if (!always.isEmpty())
		{
			for (int item = 0; item < this.size(); item++)
			{
				if (this.get(item) instanceof POAltContext)
				{
					POAltContext actxt = (POAltContext)this.get(item);

					for (POContextStack alt: actxt.alternatives)
					{
						alt.patchReturns(always);
					}
				}
			}

			if (this.lastElement().returnsEarly())
			{
				POContext ret = this.pop();
				this.pushAll(always);

				if (!always.lastElement().returnsEarly())
				{
					push(ret);		// Put the old return back
				}

				return true;
			}
		}

		return false;	// Didn't patch this level
	}

	/**
	 * An operation CallStatement has been made. The updatable state names are calculated, and these
	 * added as a "forall" of possibilities. Then the postcondition is considered, and added as a "=>"
	 * qualification, if possible.
	 */
	public boolean makeOperationCall(LexLocation from, PODefinition called,
		POExpressionList args, TCNameToken resultVar, boolean canReturn, POGState pogState, Environment env)
	{
		if (called == null)		// Called from an apply expression?
		{
			push(new POAmbiguousContext("operation call", getStateVariables(), from));
			return false;
		}
		else if (called.getDefiniteExceptions() != null)
		{
			String opname = called.name.toExplicitString(from);
			push(new POAmbiguousContext(opname + " throws exceptions", getStateVariables(), from));
			return false;
		}
		else
		{
			String opname = called.name.toExplicitString(from);
			
			if (called instanceof PORenamedDefinition)
			{
				PORenamedDefinition rdef = (PORenamedDefinition)called;
				called = rdef.def;
			}
			else if (called instanceof POInheritedDefinition)
			{
				POInheritedDefinition idef = (POInheritedDefinition)called;
				called = idef.superdef;
			}

			if (called instanceof POImplicitOperationDefinition)
			{
				POImplicitOperationDefinition imp = (POImplicitOperationDefinition)called;
				TCNameList names = new TCNameList();

				if (imp.location.sameModule(from))	// A local call
				{
					if (imp.externals != null)
					{
						for (POExternalClause ext: imp.externals)
						{
							if (ext.mode.is(Token.WRITE))
							{
								names.addAll(ext.identifiers);
							}
						}
					}
					else if (!imp.accessSpecifier.isPure)
					{
						names.addAll(imp.transitiveUpdates.matching(from.module));
					}
				}
				else // remote module
				{
					if (!imp.accessSpecifier.isPure)
					{
						names.addAll(imp.transitiveUpdates.matching(from.module));
					}
				}
				
				if (imp.type.result.isReturn())
				{
					if (resultVar == null)
					{
						resultVar = new TCNameToken(from, from.module, imp.result.pattern.toString());
					}

					names.add(resultVar);
					env = new FlatEnvironment(new TCLocalDefinition(from, resultVar, imp.result.type), env);
				}
				
				// Only save old state if we need it
				if (imp.predef != null)
				{
					push(new POSaveStateContext(imp.predef, from, imp.postdef != null));
				}
				else if (imp.postdef != null)
				{
					push(new POSaveStateContext(imp.postdef, from, imp.postdef != null));
				}

				push(new POForAllContext(names, getPostQualifier(from, imp.predef, imp.postdef, args, resultVar), env));
				setComment("Call to " + opname);

				if (canReturn)
				{
					POExpression result = new POVariableExpression(resultVar, null);
					push(new POReturnContext(pogState.getResultPattern(), pogState.getResultType(), result));
				}
			}
			else if (called instanceof POExplicitOperationDefinition)
			{
				POExplicitOperationDefinition exop = (POExplicitOperationDefinition)called;
				TCNameList names = new TCNameList();

				if (exop.location.sameModule(from))		// A local call
				{
					if (!exop.accessSpecifier.isPure)
					{
						names.addAll(exop.transitiveUpdates.matching(from.module));
					}
				}
				else // remote module
				{
					if (!exop.accessSpecifier.isPure)
					{
						names.addAll(exop.transitiveUpdates.matching(from.module));
					}
				}

				if (exop.type.result.isReturn())
				{
					if (resultVar == null)
					{
						resultVar = TCNameToken.getResult(from);	// "RESULT"
					}

					names.add(resultVar);
					env = new FlatEnvironment(new TCLocalDefinition(from, resultVar, exop.type.result), env);
				}
					
				// Only save old state if we need it
				if (exop.predef != null)
				{
					push(new POSaveStateContext(exop.predef, from, exop.postdef != null));
				}
				else if (exop.postdef != null)
				{
					push(new POSaveStateContext(exop.postdef, from, exop.postdef != null));
				}

				push(new POForAllContext(names, getPostQualifier(from, exop.predef, exop.postdef, args, resultVar), env));
				setComment("Call to " + opname);

				if (canReturn)
				{
					POExpression result = new POVariableExpression(resultVar, null);
					push(new POReturnContext(pogState.getResultPattern(), pogState.getResultType(), result));
				}
			}

			return true;
		}
	}

	/**
	 * Generate a pre/postcondition call pair, passing the arguments given, and calculating the
	 * names of the preserved state and the new state vector. This is only done for SL!
	 */
	private String getPostQualifier(LexLocation from, POExplicitFunctionDefinition predef,
		POExplicitFunctionDefinition postdef, POExpressionList args, TCNameToken resultVar)
	{
		StringBuilder postArgs = new StringBuilder(Utils.listToString(args));
		StringBuilder preArgs  = new StringBuilder(Utils.listToString(args));
		
		if (resultVar != null)
		{
			if (postArgs.length() > 0) postArgs.append(", ");
			postArgs.append(resultVar.getName());
		}

		POStateDefinition stateDefinition =
			predef != null ? predef.stateDefinition :
			postdef != null ? postdef.stateDefinition : null;

		POClassDefinition classDefinition =
			predef != null ? predef.classDefinition :
			postdef != null ? postdef.classDefinition : null;

		if (stateDefinition != null)
		{
			if (preArgs.length() > 0) preArgs.append(", ");
			preArgs.append(POSaveStateContext.getOldName());

			if (postArgs.length() > 0) postArgs.append(", ");
			postArgs.append(POSaveStateContext.getOldName());
			postArgs.append(", ");

			if (stateDefinition.location.sameModule(from))
			{
				postArgs.append(stateDefinition.toPattern(false, from));
			}
			else
			{
				postArgs.append(POSaveStateContext.getNewName());
			}
		}
		else if (classDefinition != null)
		{
			return null;	// Can't handle VDM++/RT pre/post function state
		}

		/*
		 * Create "pre_op(args[, oldstate]) and post_op(args[, result][, oldstate, newstate])"
		 * Note that we can't call the pre/post of operations in external modules, because
		 * we can't calculate their state vectors to pass. So external calls can only quantify
		 * over all local state and the result, without help from the pre/post.
		 */

		StringBuilder result = new StringBuilder();

		if (predef != null)
		{
			// Comment this in/out to include preconditions
			result.append(predef.name.toExplicitString(from));
			result.append("(");
			result.append(preArgs);
			result.append(")");
		}

		if (postdef != null)
		{
			if (result.length() > 0) result.append(" and ");
			result.append(postdef.name.toExplicitString(from));
			result.append("(");
			result.append(postArgs);
			result.append(")");
		}

		return result.length() == 0 ? null : result.toString();
	}

	/**
	 * Add a comment to the last item on the stack - usually the one last pushed.
	 */
	public void setComment(String comment)
	{
		if (!isEmpty())
		{
			peek().setComment(comment);
		}
	}

	/**
	 * The name is typically the name of the top level definition that this context
	 * belongs to, like the function or operation name. It is only used for labelling.
	 */
	public String getName()
	{
		StringBuilder result = new StringBuilder();
		String prefix = "";

		for (POContext ctxt: this)
		{
			String name = ctxt.getName();

			if (name.length() > 0)
			{
				result.append(prefix);
				result.append(name);
				prefix = "; ";
			}
		}

		return result.toString();
	}

	/**
	 * Get the full VDM-SL source of the obligation, including the context stack and
	 * the base obligation source passed in.
	 */
	public String getSource(String poSource)
	{
		POGetMatchingExpressionVisitor.init();	// Reset the "any" count, before stack

		StringBuilder result = new StringBuilder();
		String spacing = "  ";
		String indent = "";
		StringBuilder tail = new StringBuilder();

		for (POContext ctxt: this)
		{
			String po = ctxt.getSource();
			String comment = ctxt.getComment();

			if (po.length() > 0)
			{
				result.append(indent);
				result.append("(");
				result.append(indentNewLines(po, indent));

				if (comment != null)
				{
					result.append(" -- ");
					result.append(comment);
				}

				result.append("\n");
				indent = indent + spacing;
				tail.append(")");
			}
		}

		result.append(indent);
		result.append(indentNewLines(poSource, indent));
		result.append(tail);
		result.append("\n");

		return result.toString();
	}
	
	public PODefinition getDefinition()
	{
		for (POContext ctxt: this)
		{
			PODefinition definition = ctxt.getDefinition();
			
			if (definition != null)
			{
				return definition;
			}
		}
		
		return null;
	}
	
	public PODefinition getStateDefinition()
	{
		for (POContext ctxt: this)
		{
			if (ctxt instanceof POOperationDefinitionContext)
			{
				POOperationDefinitionContext opdef = (POOperationDefinitionContext)ctxt;
				
				if (opdef.stateDefinition != null)
				{
					return opdef.stateDefinition;
				}
				else
				{
					return opdef.classDefinition;
				}
			}
		}

		return null;
	}
	
	public TCNameList getStateVariables()
	{
		TCNameList names = new TCNameList();
		
		for (POContext ctxt: this)
		{
			if (ctxt instanceof POOperationDefinitionContext)
			{
				POOperationDefinitionContext opdef = (POOperationDefinitionContext)ctxt;
				
				if (opdef.stateDefinition != null)
				{
					for (TCField field: opdef.stateDefinition.fields)
					{
						names.add(field.tagname);
					}
				}
				else if (opdef.classDefinition != null)
				{
					for (PODefinition def: opdef.classDefinition.definitions)
					{
						if (def instanceof POInstanceVariableDefinition)
						{
							POInstanceVariableDefinition iv = (POInstanceVariableDefinition)def;
							names.add(iv.name);
						}
					}
				}
				
				break;
			}
		}
		
		return names;
	}
	
	public TCNameSet getReasonsAbout()
	{
		TCNameSet set = new TCNameSet();
		
		for (POContext ctxt: this)
		{
			TCNameSet r = ctxt.reasonsAbout();
			
			if (r != null)
			{
				set.addAll(r);
			}
		}
		
		return set;
	}

	/**
	 * Look through the stack for POAmbiguousContexts at this point.
	 */
	public TCNameSet getAmbiguousVariables()
	{
		TCNameSet set = new TCNameSet();

		for (POContext ctxt: this)	// In push order
		{
			set.addAll(ctxt.ambiguousVariables());
			set.removeAll(ctxt.resolvedVariables());
		}
		
		return set;
	}
	
	public boolean hasAmbiguous(TCNameSet varlist)
	{
		if (varlist.isEmpty())
		{
			return false;
		}

		TCNameSet ambiguous = getAmbiguousVariables();
		
		for (TCNameToken var: varlist)
		{
			if (ambiguous.contains(var))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public boolean isAmbiguous(TCNameToken var)
	{
		return getAmbiguousVariables().contains(var);
	}

	/**
	 * Get the initial POStatus from the stack, by looking for POUncheckedStatus entries.
	 */
	public POStatus getInitialStatus()
	{
		for (POContext ctxt: this)
		{
			if (ctxt instanceof POUncheckedContext)
			{
				return POStatus.UNCHECKED;
			}
		}

		return POStatus.UNPROVED;
	}

	private String indentNewLines(String line, String indent)
	{
		StringBuilder sb = new StringBuilder();
		String[] parts = line.split("\\n\\s*");
		String prefix = "";

		for (int i=0; i<parts.length; i++)
		{
			sb.append(prefix);
			sb.append(parts[i]);
			prefix = " ";	// Make everything in a PO one line, was "\n" + indent;
		}

		return sb.toString();
	}

	public void noteType(POExpression exp, TCType type)
	{
		this.peek().noteType(exp, type);
	}

	public TCType checkType(POExpression exp, TCType expected)
	{
		ListIterator<POContext> p = this.listIterator(size());

		while (p.hasPrevious())
		{
			POContext c = p.previous();

			if (c.isScopeBoundary())
			{
				break;		// Change of name scope for expressions.
			}

			TCType t = c.checkType(exp);

			if (t != null)
			{
				return t;
			}
		}

		return expected;
	}

	private boolean returnsEarly()
	{
		if (isEmpty())
		{
			return false;
		}
		else
		{
			return lastElement().returnsEarly();
		}
	}

	/**
	 * Reduce this stack to just those paths which returnsEarly(). This is used in
	 * loop processing to remove paths from the body, unless they are needed for
	 * postconditions on every "return".
	 */
	public List<POContextStack> reduce()
	{
		List<POContextStack> paths = this.getAlternatives(false);		// Include returns
		Iterator<POContextStack> iter = paths.iterator();

		while (iter.hasNext())
		{
			POContextStack path = iter.next();

			if (!path.returnsEarly())
			{
				iter.remove();
			}
		}

		return paths;
	}
}
