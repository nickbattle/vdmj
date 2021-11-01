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
import com.fujitsu.vdmj.po.expressions.PONotYetSpecifiedExpression;
import com.fujitsu.vdmj.po.expressions.POSubclassResponsibilityExpression;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.po.types.POPatternListTypePair;

public class FuncPostConditionObligation extends ProofObligation
{
	public FuncPostConditionObligation(POExplicitFunctionDefinition func, POContextStack ctxt)
	{
		super(func.location, POType.FUNC_POST_CONDITION, ctxt);

		StringBuilder params = new StringBuilder();
		String sep = "";

		for (POPatternList pl: func.paramPatternList)
		{
			params.append(sep);
			params.append(pl.getMatchingExpressionList());
			sep = ", ";
		}

		String body = null;

		if (func.body instanceof PONotYetSpecifiedExpression ||
			func.body instanceof POSubclassResponsibilityExpression)
		{
			// We have to say "f(a)" because we have no expression yet

			StringBuilder sb = new StringBuilder();
			sb.append(func.name.getName());
			sb.append("(");
			sb.append(params);
			sb.append(")");
			body = sb.toString();
		}
		else
		{
			body = func.body.toString();
		}

		value = ctxt.getObligation(generate(func.predef, func.postdef, params, body));
	}

	public FuncPostConditionObligation(POImplicitFunctionDefinition func, POContextStack ctxt)
	{
		super(func.location, POType.FUNC_POST_CONDITION, ctxt);

		StringBuilder params = new StringBuilder();

		for (POPatternListTypePair pl: func.parameterPatterns)
		{
			params.append(pl.patterns.getMatchingExpressionList());
		}

		String body = null;

		if (func.body == null)
		{
			body = func.result.pattern.toString();
		}
		else if (func.body instanceof PONotYetSpecifiedExpression ||
				 func.body instanceof POSubclassResponsibilityExpression)
		{
			// We have to say "f(a)" because we have no expression yet

			StringBuilder sb = new StringBuilder();
			sb.append(func.name.getName());
			sb.append("(");
			sb.append(params);
			sb.append(")");
			body = sb.toString();
		}
		else
		{
			body = func.body.toString();
		}

		value = ctxt.getObligation(generate(func.predef, func.postdef, params, body));
	}

	private String generate(
		POExplicitFunctionDefinition predef,
		POExplicitFunctionDefinition postdef,
		StringBuilder params, String body)
	{
		StringBuilder sb = new StringBuilder();

		if (predef != null)
		{
			sb.append(predef.name.getName());
			sb.append("(");
			sb.append(params);
			sb.append(") => ");
		}

		sb.append(postdef.name.getName());
		sb.append("(");
		
		if (params.length() != 0)	// ie. fn has args
		{
			sb.append(params);
			sb.append(", ");
		}
		
		sb.append(body);
		sb.append(")");

		return sb.toString();
	}
}
