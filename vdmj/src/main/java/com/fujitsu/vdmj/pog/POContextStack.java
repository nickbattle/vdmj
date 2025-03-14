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

import java.util.ListIterator;
import java.util.Stack;

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
