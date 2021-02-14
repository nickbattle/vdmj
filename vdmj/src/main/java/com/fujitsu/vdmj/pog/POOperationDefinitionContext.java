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

import com.fujitsu.vdmj.po.definitions.POClassDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POStateDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCType;

public class POOperationDefinitionContext extends POContext
{
	public final TCNameToken name;
	public final TCOperationType deftype;
	public final POPatternList paramPatternList;
	public final boolean addPrecond;
	public final POExpression precondition;
	public final PODefinition stateDefinition;

	public POOperationDefinitionContext(POImplicitOperationDefinition definition,
		boolean precond, PODefinition stateDefinition)
	{
		this.name = definition.name;
		this.deftype = definition.type;
		this.addPrecond = precond;
		this.paramPatternList = definition.getParamPatternList();
		this.precondition = definition.precondition;
		this.stateDefinition = stateDefinition;
	}

	@Override
	public String getContext()
	{
		StringBuilder sb = new StringBuilder();

		if (!deftype.parameters.isEmpty())
		{
    		sb.append("forall ");
    		String sep = "";
			Iterator<TCType> types = deftype.parameters.iterator();

			for (POPattern p: paramPatternList)
			{
				sb.append(sep);
				sb.append(p.getMatchingExpression());	// Expands anys
				sb.append(":");
				sb.append(types.next());
				sep = ", ";
			}

			if (stateDefinition != null)
			{
				appendStatePatterns(sb);
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

	private void appendStatePatterns(StringBuilder sb)
	{
		if (stateDefinition == null)
		{
			return;
		}
		else if (stateDefinition instanceof POStateDefinition)
		{
			POStateDefinition def = (POStateDefinition)stateDefinition;
			sb.append(", oldstate:");
			sb.append(def.name.getName());
		}
		else
		{
			POClassDefinition def = (POClassDefinition)stateDefinition;
			sb.append(", oldself:");
			sb.append(def.name.getName());
		}
	}
}
