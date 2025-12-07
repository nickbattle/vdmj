/*******************************************************************************
 *
 *	Copyright (c) 2025 Nick Battle.
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

package com.fujitsu.vdmj.po.annotations;

import com.fujitsu.vdmj.po.definitions.POAssignmentDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;

public class POOperationMeasureAnnotation extends POAnnotation
{
	private static final long serialVersionUID = 1L;

	public final TCNameToken ghostName;
	public final POExpression expression;
	public final TCType type;
	
	public POOperationMeasureAnnotation(TCIdentifierToken name, POExpressionList args, TCNameToken ghostName, TCType type)
	{
		super(name, args);
		this.expression = args.get(0);
		this.ghostName = ghostName;
		this.type = type;
	}

	
	public POAssignmentDefinition getDefinition()	// let MEASURE_? = <expression> in...
	{
			return new POAssignmentDefinition(
				ghostName, expression.getExptype(), expression, expression.getExptype());
	}
		
	// TODO Remove me?

	// public ProofObligationList getProofObligations(POApplyExpression apply, POContextStack ctxt)
	// {
	// 	// Add assignments for each parameter and then re-evaluate the measure

	// 	PODefinitionList defs = new PODefinitionList();

	// 	if (apply.opdef instanceof POExplicitOperationDefinition)
	// 	{
	// 		POExplicitOperationDefinition exop = (POExplicitOperationDefinition)apply.opdef;
	// 		int nargs = exop.parameterPatterns.size();

	// 		for (int a = 0; a < nargs; a++)
	// 		{
	// 			POPattern param = exop.parameterPatterns.get(a);
	// 			TCType type = exop.type.parameters.get(a);
	// 			POExpression arg = apply.args.get(a);
	// 			POValueDefinition vdef = new POValueDefinition(
	// 				null, param, type, arg, arg.getExptype(), null);

	// 			defs.add(vdef);
	// 		}
	// 	}
	// 	else if (apply.opdef instanceof POImplicitOperationDefinition)
	// 	{
	// 		POImplicitOperationDefinition imop = (POImplicitOperationDefinition)apply.opdef;
	// 		int a = 0;

	// 		for (POPatternListTypePair pair: imop.parameterPatterns)
	// 		{
	// 			for (POPattern param: pair.patterns)
	// 			{
	// 				POExpression arg = apply.args.get(a++);
	// 				POValueDefinition vdef = new POValueDefinition(
	// 					null, param, pair.type, arg, arg.getExptype(), null);

	// 				defs.add(vdef);
	// 			}
	// 		}
	// 	}

	// 	// This simulates a call to "measure_op(args)", updating the param values

	// 	ctxt.add(new POLetDefContext(defs));
	// 	ctxt.setComment("Arguments to recursive call");
	// 	ProofObligationList result = RecursiveObligation.getAllPOs(location, this, ctxt);
	// 	ctxt.pop();

	// 	return result;
	// }
}
