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

import com.fujitsu.vdmj.po.annotations.POAnnotationList;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.visitors.POGetMatchingExpressionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
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
	
	public String markObligation()
	{
		for (POContext ctxt: this)
		{
			String message = ctxt.markObligation();
			
			if (message != null)
			{
				return message;
			}
		}
		
		return null;
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
