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

package com.fujitsu.vdmj.po.statements;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.POAmbiguousContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POReturnContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
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
		boolean needRESULT = false;

		if (expression != null)
		{
			pogState.setAmbiguous(false);
			obligations.addAll(expression.getProofObligations(ctxt, pogState, env));
			
			PODefinition definition = ctxt.getDefinition();
			TCType result = null;
			
			if (definition instanceof POExplicitOperationDefinition)
			{
				POExplicitOperationDefinition opdef = (POExplicitOperationDefinition)definition;
				result = opdef.type.result;
				needRESULT = true;
			}
			else if (definition instanceof POImplicitOperationDefinition)
			{
				POImplicitOperationDefinition opdef = (POImplicitOperationDefinition)definition;
				result = opdef.type.result;
				needRESULT = false;		// RESULT is explicit in the definition
			}
			
			if (result != null && !TypeComparator.isSubType(getStmttype(), result))
			{
				obligations.addAll(SubTypeObligation.getAllPOs(expression, result, getStmttype(), ctxt));
			}
		}
		
		// Identify this (sub)stack as having a return
		
		if (needRESULT)
		{
			if (pogState.isAmbiguous())		// expression has ambiguous values
			{
				TCNameToken result = TCNameToken.getResult(location);
				ctxt.push(new POAmbiguousContext("return", new TCNameSet(result), location));
				pogState.setAmbiguous(false);
			}

			ctxt.push(new POReturnContext(expression));
		}
		else
		{
			ctxt.push(new POReturnContext(null));
		}

		return obligations;
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseReturnStatement(this, arg);
	}
}
