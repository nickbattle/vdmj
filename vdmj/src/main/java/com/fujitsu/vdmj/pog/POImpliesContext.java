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

import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.tc.lex.TCNameSet;

public class POImpliesContext extends POContext
{
	public final String exp;
	private final TCNameSet reasonsAbout;

	public POImpliesContext(String op, POExpression... conditions)
	{
		this.reasonsAbout = new TCNameSet();
		StringBuilder sb = new StringBuilder();
		String sep = "";
		
		for (POExpression condition: conditions)
		{
			if (condition != null)	// null for missing loop invariant
			{
				sb.append(sep);
				sb.append(condition.toString().replaceAll("~", "\\$"));
				sep = " " + op + " ";

				this.reasonsAbout.addAll(condition.getVariableNames());
			}
		}
		
		this.exp = sb.toString();
	}

	public POImpliesContext(POExpression... conditions)
	{
		this("and", conditions);
	}

	public POImpliesContext(POExplicitOperationDefinition def)
	{
		this.exp = preconditionCall(def.name, null, def.predef.getParamPatternList(), def.precondition);
		this.reasonsAbout = def.precondition.getVariableNames();
	}

	public POImpliesContext(POImplicitOperationDefinition def)
	{
		this.exp = preconditionCall(def.name, null, def.predef.getParamPatternList(), def.precondition);
		this.reasonsAbout = def.precondition.getVariableNames();
	}
	
	@Override
	public TCNameSet reasonsAbout()
	{
		return reasonsAbout;
	}

	@Override
	public String getSource()
	{
		StringBuilder sb = new StringBuilder();

		sb.append(exp);
		sb.append(" =>");

		return sb.toString();
	}
}
