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
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POStateDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCType;

public class POOperationDefinitionContext extends POContext
{
	public final TCNameToken name;
	public final TCOperationType deftype;
	public final POPatternList paramPatternList;
	public final boolean addPrecond;
	public final String precondition;
	public final PODefinition definition;
	public final PODefinition stateDefinition;
	public final boolean expandState;
	public final POExpression preExp;
	
	public POOperationDefinitionContext(POExplicitOperationDefinition definition,
		boolean precond, PODefinition stateDefinition, boolean expandState)
	{
		this.name = definition.name;
		this.deftype = definition.type;
		this.addPrecond = precond;
		this.paramPatternList = definition.parameterPatterns;
		this.precondition = preconditionCall(name, paramPatternList, stateDefinition);
		this.stateDefinition = stateDefinition;
		this.definition = definition;
		this.expandState = expandState;
		this.preExp = definition.precondition;
	}

	public POOperationDefinitionContext(POImplicitOperationDefinition definition,
		boolean precond, PODefinition stateDefinition, boolean expandState)
	{
		this.name = definition.name;
		this.deftype = definition.type;
		this.addPrecond = precond;
		this.paramPatternList = definition.getParamPatternList();
		this.precondition = preconditionCall(name, paramPatternList, stateDefinition);
		this.stateDefinition = stateDefinition;
		this.definition = definition;
		this.expandState = expandState;
		this.preExp = definition.precondition;
	}
	
	@Override
	public PODefinition getDefinition()
	{
		return definition;
	}
	
	@Override
	public TCNameSet reasonsAbout()
	{
		if (addPrecond && preExp != null)
		{
			return preExp.getVariableNames();
		}
		
		return super.reasonsAbout();
	}

	@Override
	public String getSource()
	{
		StringBuilder sb = new StringBuilder();

		if (!deftype.parameters.isEmpty() || stateDefinition != null)
		{
    		sb.append("forall ");
    		String sep = "";
			Iterator<TCType> types = deftype.parameters.iterator();

			for (POPattern p: paramPatternList)
			{
				sb.append(sep);
				sb.append(p.removeIgnorePatterns());
				sb.append(":");
				sb.append(types.next().toExplicitString(name.getLocation()));
				sep = ", ";
			}

			if (stateDefinition != null)
			{
				sb.append(sep);
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
			
			if (expandState)
			{
				sb.append(def.toPattern());
				sb.append(":");
				sb.append(def.name.getName());
			}
			else
			{
				sb.append("oldstate:");
				sb.append(def.name.getName());
			}
		}
		else
		{
			POClassDefinition def = (POClassDefinition)stateDefinition;

			if (expandState)
			{
				sb.append(def.toPattern());
				sb.append(":");
				sb.append(def.name.getName());
			}
			else
			{
				sb.append("oldself:");
				sb.append(def.name.getName());
			}
		}
	}
}
