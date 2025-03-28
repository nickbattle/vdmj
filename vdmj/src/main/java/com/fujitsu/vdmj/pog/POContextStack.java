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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.pog;

import java.util.List;
import java.util.ListIterator;
import java.util.Stack;
import java.util.Vector;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.po.annotations.POAnnotationList;
import com.fujitsu.vdmj.po.definitions.POClassDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POInstanceVariableDefinition;
import com.fujitsu.vdmj.po.definitions.POStateDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.visitors.POGetMatchingExpressionVisitor;
import com.fujitsu.vdmj.po.statements.POExternalClause;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;

@SuppressWarnings("serial")
public class POContextStack extends Stack<POContext>
{
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
		boolean returns = false;
		
		while (size() > size)
		{
			POContext popped = pop();
			
			if (popped instanceof POReturnContext)
			{
				returns = true;
			}
			
			into.add(0, popped);		// Preserve order
		}
		
		if (returns) into.clear();		// ie. has no further effect
	}
	
	public void copyInto(int size, POContextStack into)
	{
		boolean returns = false;
		
		for (int i=size; i < size(); i++)
		{
			POContext item = get(i);
			
			if (item instanceof POReturnContext)
			{
				returns = true;
			}
			
			into.add(item);				// Preserve order
		}
		
		if (returns) into.clear();		// ie. has no further effect
	}
	
	/**
	 * If the stack contains POAltContext items, these produce alternative substacks that
	 * have to be iterated through, before creating a set of obligations.
	 */
	public List<POContextStack> getAlternatives()
	{
		List<POContextStack> results = new Vector<POContextStack>();
		results.add(new POContextStack());
		
		for (POContext ctxt: this)
		{
			if (ctxt instanceof POAltContext)
			{
				POAltContext alt = (POAltContext)ctxt;
				List<POContextStack> toAdd = new Vector<POContextStack>();
				
				for (POContextStack substack: alt.alternatives)
				{
					for (POContextStack alternative: substack.getAlternatives())
					{
						for (POContextStack original: results)
						{
							POContextStack combined = new POContextStack();
							combined.addAll(original);
							combined.addAll(alternative);
							toAdd.add(combined);
						}
					}
				}
				
				results.clear();
				results.addAll(toAdd);
			}
			else
			{
				for (POContextStack choice: results)
				{
					choice.add(ctxt);
				}
			}
		}
		
		return results;
	}
	
	/**
	 * Operation calls may cause ambiguities in the state. This is affected by whether
	 * they are pure or have ext clauses.
	 */
	public void addOperationCall(LexLocation from, PODefinition called)
	{
		if (called == null)	// Assumed to update something
		{
			push(new POAmbiguousContext("operation call", getStateVariables(), from));
		}
		else if (called.accessSpecifier.isPure)
		{
			return;			// No updates, by definition
		}
		else if (called instanceof POImplicitOperationDefinition)
		{
			POImplicitOperationDefinition imp = (POImplicitOperationDefinition)called;
			
			if (imp.externals != null)
			{
				for (POExternalClause ext: imp.externals)
				{
					if (ext.mode.is(Token.WRITE))
					{
						push(new POAmbiguousContext("operation ext clause", ext.identifiers, from));
					}
				}
			}
			else
			{
				push(new POAmbiguousContext("operation call", getStateVariables(), from));
			}
		}
		else if (called instanceof POExplicitOperationDefinition)
		{
			push(new POAmbiguousContext("operation call", getStateVariables(), from));
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

			if (po.length() > 0)
			{
				result.append(indent);
				result.append("(");
				result.append(indentNewLines(po, indent));
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
	
	public POAnnotationList getAnnotations()
	{
		for (POContext ctxt: this)
		{
			POAnnotationList annotations = ctxt.getAnnotations();
			
			if (annotations != null && !annotations.isEmpty())
			{
				return annotations;
			}
		}
		
		return null;
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
	
	public TCNameList getStateVariables()
	{
		TCNameList names = new TCNameList();
		
		for (POContext ctxt: this)
		{
			if (ctxt instanceof POOperationDefinitionContext)
			{
				POOperationDefinitionContext opdef = (POOperationDefinitionContext)ctxt;
				
				if (opdef.stateDefinition instanceof POStateDefinition)
				{
					POStateDefinition state = (POStateDefinition)opdef.stateDefinition;
					
					for (TCField field: state.fields)
					{
						names.add(field.tagname);
					}
				}
				else if (opdef.stateDefinition instanceof POClassDefinition)
				{
					POClassDefinition clazz = (POClassDefinition)opdef.stateDefinition;
					
					for (PODefinition def: clazz.definitions)
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

	public TCTypeList getTypeParams()
	{
		for (POContext ctxt: this)
		{
			TCTypeList params = ctxt.getTypeParams();
			
			if (params != null && !params.isEmpty())
			{
				return params;
			}
		}
		
		return null;
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
	
	public boolean hasAmbiguousState(TCNameSet varlist)
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

	private String indentNewLines(String line, String indent)
	{
		StringBuilder sb = new StringBuilder();
		String[] parts = line.split("\n\\s*");
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
}
