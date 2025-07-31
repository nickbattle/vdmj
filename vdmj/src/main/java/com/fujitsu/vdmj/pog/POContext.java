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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fujitsu.vdmj.po.definitions.POClassDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.POStateDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;

abstract public class POContext
{
	private Map<POExpression, TCType> knownTypes = new HashMap<POExpression, TCType>();
	private String comment = null;

	/**
	 * Generate the VDM source of the context.
	 */
	abstract public String getSource();

	public String getComment()
	{
		return comment;
	}

	protected void setComment(String comment)
	{
		this.comment = comment;
	}

	public String getName()
	{
		return "";		// Overridden in PONameContext
	}

	public boolean isExistential()
	{
		return false;
	}

	public TCNameSet reasonsAbout()
	{
		return null;
	}

	public TCNameSet ambiguousVariables()
	{
		return new TCNameSet();
	}

	public TCNameSet resolvedVariables()
	{
		return new TCNameSet();
	}

	public boolean isScopeBoundary()
	{
		return false;
	}

	public void noteType(POExpression exp, TCType type)
	{
		knownTypes.put(exp, type);
	}

	public TCType checkType(POExpression exp)
	{
		return knownTypes.get(exp);
	}

	/**
	 * Generate a precondition check for a function, possibly with type parameters.
	 */
	protected String preconditionCall(TCNameToken name, TCTypeList typeParams, List<POPatternList> paramPatternList, POExpression body)
	{
		if (body == null)
		{
			return null;
		}
		
		StringBuilder call = new StringBuilder();
		call.append(name.getPreName(name.getLocation()));
		
		if (typeParams != null && !typeParams.isEmpty())
		{
			call.append("[");
			String sep = "";
			
			for (TCType param: typeParams)
			{
				call.append(sep);
				call.append(param);
				sep = ", ";
			}
			
			call.append("]");
		}

		for (POPatternList plist: paramPatternList)
		{
			call.append("(" + plist.removeIgnorePatterns() + ")");
		}

		return call.toString();
	}
	
	/**
	 * Generate a precondition call for an operation, passing the state (ie. a StateDefinition or ClassDefinition).
	 */
	protected String preconditionCall(TCNameToken name, POPatternList paramPatternList, PODefinition state)
	{
		StringBuilder call = new StringBuilder();
		call.append(name.getPreName(name.getLocation()));
		call.append("(");
		String sep = "";
		
		for (POPattern param: paramPatternList)
		{
			call.append(sep);
			call.append(param.removeIgnorePatterns());
			sep = ", ";
		}

		if (state instanceof POStateDefinition)
		{
			call.append(sep);
			call.append(state.toPattern(false));
		}
		else if (state instanceof POClassDefinition)
		{
			POClassDefinition cdef = (POClassDefinition)state;
			call.append(sep);
			call.append(cdef.toNew());
		}
		
		call.append(")");
		return call.toString();
	}

	/**
	 * Return the PODefinition of the enclosing definition (eg. func or operation).
	 */
	public PODefinition getDefinition()
	{
		return null;	// See fn/op definition contexts
	}

	/**
	 * True if this context causes the flow of control to abort, currently overridden
	 * for POReturnContext and POExitContexts.
	 */
	public boolean returnsEarly()
	{
		return false;
	}
}
