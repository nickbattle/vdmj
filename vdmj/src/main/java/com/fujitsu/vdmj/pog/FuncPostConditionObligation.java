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
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.expressions.PONotYetSpecifiedExpression;
import com.fujitsu.vdmj.po.expressions.POSubclassResponsibilityExpression;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.po.patterns.visitors.POGetMatchingExpressionVisitor;
import com.fujitsu.vdmj.po.types.POPatternListTypePair;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCType;

public class FuncPostConditionObligation extends ProofObligation
{
	private static final String NOT_YET_SPECIFIED = "not yet specified";
	
	public FuncPostConditionObligation(POExplicitFunctionDefinition func, POContextStack ctxt)
	{
		super(func.location, POType.FUNC_POST_CONDITION, ctxt);
		String body = null;

		if (func.body instanceof PONotYetSpecifiedExpression ||
			func.body instanceof POSubclassResponsibilityExpression)
		{
			body = NOT_YET_SPECIFIED;
		}
		else
		{
			body = func.body.toString();
		}

		source = ctxt.getSource(generate(func.predef, func.postdef, body, func.type));
		// reasonsAbout ?
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
			body = NOT_YET_SPECIFIED;
		}
		else
		{
			body = func.body.toString();
		}

		source = ctxt.getSource(generate(func.predef, func.postdef, body, func.type));
		// reasonsAbout ?
	}

	private String generate(
		POExplicitFunctionDefinition predef,
		POExplicitFunctionDefinition postdef,
		String body, TCFunctionType ftype)
	{
		StringBuilder sb = new StringBuilder();

		if (predef != null)
		{
			sb.append(functionCall(predef, null));
			sb.append(" => ");
		}

		if (body == NOT_YET_SPECIFIED)
		{
			sb.append("exists RESULT : ");
			sb.append(ftype.result);
			sb.append(" & ");
			sb.append(functionCall(postdef, "RESULT"));
		}
		else
		{
			sb.append(functionCall(postdef, body));
		}

		return sb.toString();
	}
	
	private String functionCall(POExplicitFunctionDefinition def, String addResult)
	{
		StringBuilder sb = new StringBuilder();
		int size = def.paramPatternList.size();
		sb.append(def.name.getName());
		
		if (def.typeParams != null)
		{
			sb.append("[");
			String sep = "";
			
			for (TCType p: def.typeParams)
			{
				sb.append(sep);
				sb.append(p);
				sep = ", ";
			}
			
			sb.append("]");
		}

		POGetMatchingExpressionVisitor.init();
		
		for (int i=0; i<size; i++)
		{
			POPatternList pl = def.paramPatternList.get(i);
			sb.append("(");
			
			if (addResult != null && i == size - 1)
			{
				POExpressionList exps = pl.getMatchingExpressionList();
				exps.removeElementAt(exps.size() - 1);
				
				if (!exps.isEmpty())
				{
					sb.append(exps);
					sb.append(", ");
				}
				
				sb.append(addResult);
			}
			else
			{
				sb.append(pl.getMatchingExpressionList());
			}
			
			sb.append(")");
		}
		
		return sb.toString();
	}
}
