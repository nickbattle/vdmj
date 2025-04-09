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
import com.fujitsu.vdmj.po.annotations.POLoopInvariantAnnotation;
import com.fujitsu.vdmj.po.definitions.POAssignmentDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.LoopInvariantObligation;
import com.fujitsu.vdmj.pog.POAmbiguousContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POForAllSequenceContext;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POImpliesContext;
import com.fujitsu.vdmj.pog.POLetDefContext;
import com.fujitsu.vdmj.pog.POScopeContext;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.typechecker.Environment;

public class POForIndexStatement extends POStatement
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken var;
	public final POExpression from;
	public final POExpression to;
	public final POExpression by;
	public final POStatement statement;
	public final PODefinition vardef;

	public POForIndexStatement(LexLocation location,
		TCNameToken var, POExpression from, POExpression to, POExpression by, POStatement body,
		PODefinition vardef)
	{
		super(location);
		this.var = var;
		this.from = from;
		this.to = to;
		this.by = by;
		this.statement = body;
		this.vardef = vardef;
	}

	@Override
	public String toString()
	{
		return "for " + var + " = " + from + " to " + to +
					(by == null ? "" : " by " + by) + "\n" + statement;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = from.getProofObligations(ctxt, pogState, env);
		obligations.addAll(to.getProofObligations(ctxt, pogState, env));

		if (by != null)
		{
			obligations.addAll(by.getProofObligations(ctxt, pogState, env));
		}

		POLoopInvariantAnnotation annotation = annotations.getInstance(POLoopInvariantAnnotation.class);
		TCNameSet updates = statement.updatesState();
		
		if (annotation == null)		// No loop invariant defined
		{
			int popto = ctxt.pushAt(new POScopeContext());
			ctxt.push(new POForAllSequenceContext(var, from, to, by));
			POGState copy = pogState.getCopy();
			ProofObligationList loops = statement.getProofObligations(ctxt, copy, env);
			pogState.combineWith(copy);
			ctxt.popTo(popto);
	
			if (!updates.isEmpty())
			{
				ctxt.push(new POAmbiguousContext("for loop", updates, location));
			}
			
			obligations.addAll(loops);
			return obligations;
		}
		else
		{
			POAssignmentDefinition assign = new POAssignmentDefinition(var, vardef.getType(), from, vardef.getType());
			ctxt.push(new POLetDefContext(assign));
			ProofObligation initial = new LoopInvariantObligation(annotation.location, ctxt, annotation.invariant);
			initial.setMessage("check initial for-loop");
			obligations.add(initial);
			ctxt.pop();
			
			int popto = ctxt.size();
			POGState copy = pogState.getCopy();
			
			ctxt.push(new POForAllSequenceContext(var, from, to, by));
			ProofObligation before = new LoopInvariantObligation(statement.location, ctxt, annotation.invariant);
			before.setMessage("check before for-loop");
			obligations.add(before);
			
			obligations.addAll(statement.getProofObligations(ctxt, copy, env));
			
			ProofObligation after = new LoopInvariantObligation(statement.location, ctxt, annotation.invariant);
			after.setMessage("check after for-loop");
			obligations.add(after);

			pogState.combineWith(copy);
			ctxt.popTo(popto);
			
//			POExpression end = new POEqualsExpression(
//					new POVariableExpression(var, vardef),
//					new LexKeywordToken(Token.EQUALS, location),
//					to, vardef.getType(), vardef.getType());
			
			// Leave implication for following POs
			ctxt.push(new POImpliesContext(annotation.invariant));	// invariant => ...
			
			return obligations;
		}
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseForIndexStatement(this, arg);
	}
}
