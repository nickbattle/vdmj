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

import java.util.Iterator;
import java.util.List;

import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCType;

public class POFunctionDefinitionContext extends POContext
{
	public final TCNameToken name;
	public final TCFunctionType deftype;
	public final List<POPatternList> paramPatternList;
	public final boolean addPrecond;
	public final String precondition;

	public POFunctionDefinitionContext(
		POExplicitFunctionDefinition definition, boolean precond)
	{
		this.name = definition.name;
		this.deftype = definition.type;
		this.paramPatternList = definition.paramPatternList;
		this.addPrecond = precond;
		this.precondition = preconditionCall(name, paramPatternList, definition.precondition);
	}

	public POFunctionDefinitionContext(
		POImplicitFunctionDefinition definition, boolean precond)
	{
		this.name = definition.name;
		this.deftype = definition.type;
		this.addPrecond = precond;
		this.paramPatternList = definition.getParamPatternList();
		this.precondition = preconditionCall(name, paramPatternList, definition.precondition);
	}

	@Override
	public String getContext()
	{
		StringBuilder sb = new StringBuilder();

		if (!deftype.parameters.isEmpty())
		{
    		sb.append("forall ");
    		String sep = "";
    		TCFunctionType ftype = deftype;

    		for (POPatternList pl: paramPatternList)
    		{
    			Iterator<TCType> types = ftype.parameters.iterator();

    			for (POPattern p: pl)
    			{
					sb.append(sep);
					sb.append(p.getMatchingExpression());	// Expands anys
					sb.append(":");
					TCType ptype = types.next();
					sb.append(ptype.toExplicitString(name.getLocation()));
					sep = ", ";
    			}

    			if (ftype.result instanceof TCFunctionType)
    			{
    				ftype = (TCFunctionType)ftype.result;
    			}
    			else
    			{
    				break;
    			}
    		}

    		sb.append(" &");

    		if (addPrecond && precondition != null)
    		{
    			sb.append(" ");
    			sb.append(precondition);
    			sb.append(" =>");
    		}
		}

		return sb.toString();
	}
}
