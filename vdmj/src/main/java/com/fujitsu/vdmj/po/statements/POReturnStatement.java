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

package com.fujitsu.vdmj.po.statements;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.POAmbiguousContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POReturnContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class POReturnStatement extends POStatement
{
	private static final long serialVersionUID = 1L;
	public final POExpression expression;

	public POReturnStatement(LexLocation location, POExpression expression)
	{
		super(location);
		this.expression = expression;
	}

	@Override
	public String toString()
	{
		return "return" + (expression == null ? "" : " (" + expression + ")");
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = new ProofObligationList();
		TCType rtype = null;

		POExpression extracted = null;
		
		if (expression != null)
		{
			// Attempt to extract operation calls from the RHS
			extracted = extractOpCalls(expression, pogState, ctxt, env);

			pogState.setAmbiguous(false);
			obligations.addAll(extracted.getProofObligations(ctxt, pogState, env));
			
			PODefinition definition = ctxt.getDefinition();
			
			if (definition instanceof POExplicitOperationDefinition)
			{
				POExplicitOperationDefinition opdef = (POExplicitOperationDefinition)definition;
				rtype = opdef.type.result;
			}
			else if (definition instanceof POImplicitOperationDefinition)
			{
				POImplicitOperationDefinition opdef = (POImplicitOperationDefinition)definition;
				rtype = opdef.type.result;
			}
			
			if (rtype != null && !TypeComparator.isSubType(getStmttype(), rtype))
			{
				obligations.addAll(SubTypeObligation.getAllPOs(extracted, rtype, getStmttype(), ctxt));
			}
		}
		
		// Identify this (sub)stack as having a return
		
		if (rtype != null && rtype.isReturn())
		{
			POPattern result = pogState.getResultPattern();
			TCType resultType = pogState.getResultType();

			if (pogState.isAmbiguous())		// extracted still has ambiguous values
			{
				ctxt.push(new POAmbiguousContext("return", result.getVariableNames(), location));
				pogState.setAmbiguous(false);
			}

			ctxt.push(new POReturnContext(result, resultType, extracted));
		}
		else
		{
			ctxt.push(new POReturnContext());
		}

		return obligations;
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseReturnStatement(this, arg);
	}
}
