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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.fujitsu.vdmj.po.annotations.POAnnotationList;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;

abstract public class POContext
{
	abstract public String getContext();
	private Map<POExpression, TCType> knownTypes = new HashMap<POExpression, TCType>();

	public String getName()
	{
		return "";		// Overridden in PONameContext
	}

	public POAnnotationList getAnnotations()
	{
		return null;
	}
	
	public boolean isExistential()
	{
		return false;
	}

	public TCTypeList getTypeParams()
	{
		return null;
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
	
	protected String preconditionCall(TCNameToken name, TCTypeList typeParams, POPatternList paramPatternList, POExpression body)
	{
		List<POPatternList> pplist = new Vector<POPatternList>();
		pplist.add(paramPatternList);
		return preconditionCall(name, typeParams, pplist, body);
	}
	
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
}
