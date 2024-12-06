/*******************************************************************************
 *
 *	Copyright (c) 2024 Fujitsu Services Ltd.
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

import com.fujitsu.vdmj.po.definitions.POAssignmentDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.tc.types.TCType;

public class POAssignmentContext extends POContext
{
	public final PODefinitionList assignmentDefs;
	public final String pattern;
	public final TCType type;
	public final POExpression expression;
	public final String tooComplex;

	public POAssignmentContext(PODefinitionList assignmentDefs)
	{
		this.assignmentDefs = assignmentDefs;
		this.pattern = null;
		this.type = null;
		this.expression = null;
		this.tooComplex = null;
	}

	public POAssignmentContext(String pattern, TCType type, POExpression expression, boolean tooComplex)
	{
		this.assignmentDefs = null;
		this.pattern = pattern;
		this.type = type;
		this.expression = expression;
		this.tooComplex = tooComplex ? ProofObligation.COMPLEX_ASSIGNMENT : null;
	}

	@Override
	public boolean isScopeBoundary()
	{
		return true;
	}
	
	@Override
	public String markObligation()
	{
		return tooComplex;
	}

	@Override
	public String getSource()
	{
		StringBuilder sb = new StringBuilder();

		String sep = "";
		
		if (assignmentDefs == null)
		{
			sb.append("let ");
			sb.append(pattern);
			sb.append(" : ");
			sb.append(type.toExplicitString(expression.location));
			sb.append(" = ");
			sb.append(expression);
			sb.append(" in");
		}
		else if (!assignmentDefs.isEmpty())
		{
			sb.append("let ");

			for (PODefinition def: assignmentDefs)
			{
				sb.append(sep);
				POAssignmentDefinition adef = (POAssignmentDefinition)def;
				sb.append(adef.name);
				sb.append(" : ");
				sb.append(adef.type.toExplicitString(adef.location));
				sb.append(" = ");
				sb.append(adef.expression);
				sep = ", ";
			}

			sb.append(" in");
		}

		return sb.toString();
	}
}
