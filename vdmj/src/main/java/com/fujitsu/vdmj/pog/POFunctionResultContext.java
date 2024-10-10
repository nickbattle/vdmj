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

import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POIdentifierPattern;
import com.fujitsu.vdmj.po.types.POPatternTypePair;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCFunctionType;

public class POFunctionResultContext extends POContext
{
	public final TCNameToken name;
	public final TCFunctionType deftype;
	public final String precondition;
	public final POExpression body;
	public final POPatternTypePair result;
	public final boolean implicit;

	public POFunctionResultContext(POExplicitFunctionDefinition definition)
	{
		this.name = definition.name;
		this.deftype = definition.type;
		this.precondition = preconditionCall(name, definition.typeParams, definition.paramPatternList, definition.precondition);
		this.body = definition.body;
		this.implicit = false;
		
		TCFunctionType lastFunc = definition.type;
		
		for (int i=0; i<definition.paramDefinitionList.size(); i++)		// find last curried func
		{
			if (lastFunc.result instanceof TCFunctionType)
			{
				lastFunc = (TCFunctionType) lastFunc.result;
			}
		}

		this.result = new POPatternTypePair(
			new POIdentifierPattern(
				new TCNameToken(
					definition.location, definition.name.getModule(), "RESULT")),
					lastFunc.result);
	}

	public POFunctionResultContext(POImplicitFunctionDefinition definition)
	{
		this.name = definition.name;
		this.deftype = definition.type;
		this.precondition = preconditionCall(name, definition.typeParams, definition.getParamPatternList(), definition.precondition);
		this.body = definition.body;
		this.implicit = true;
		this.result = definition.result;
	}

	@Override
	public String getSource()
	{
		StringBuilder sb = new StringBuilder();

		if (precondition != null)
		{
			sb.append(precondition);
			sb.append(" => ");
		}

		if (implicit)
		{
			sb.append("forall ");
			sb.append(result);
			sb.append(" & ");
		}
		else
		{
			sb.append("let ");
			sb.append(result);
			sb.append(" = ");
			sb.append(body);
			sb.append(" in ");
		}

		return sb.toString();
	}
}
