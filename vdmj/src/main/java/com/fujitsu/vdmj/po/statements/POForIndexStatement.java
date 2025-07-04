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

import java.util.List;

import com.fujitsu.vdmj.ast.lex.LexIntegerToken;
import com.fujitsu.vdmj.ast.lex.LexKeywordToken;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.po.annotations.POLoopInvariantAnnotation;
import com.fujitsu.vdmj.po.definitions.POAssignmentDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.expressions.POAndExpression;
import com.fujitsu.vdmj.po.expressions.POEqualsExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POIntegerLiteralExpression;
import com.fujitsu.vdmj.po.expressions.POLessEqualExpression;
import com.fujitsu.vdmj.po.expressions.POPlusExpression;
import com.fujitsu.vdmj.po.expressions.PORemExpression;
import com.fujitsu.vdmj.po.expressions.POSubtractExpression;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.LoopInvariantObligation;
import com.fujitsu.vdmj.pog.POAmbiguousContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POForAllContext;
import com.fujitsu.vdmj.pog.POForAllSequenceContext;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POImpliesContext;
import com.fujitsu.vdmj.pog.POLetDefContext;
import com.fujitsu.vdmj.pog.POScopeContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;

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

		List<POLoopInvariantAnnotation> invariants = annotations.getInstances(POLoopInvariantAnnotation.class);
		TCNameSet updates = statement.updatesState();
		
		if (invariants.isEmpty())		// No loop invariants defined
		{
			int popto = ctxt.pushAt(new POScopeContext());
			ctxt.push(new POForAllSequenceContext(var, from, to, by));
			ProofObligationList loops = statement.getProofObligations(ctxt, pogState, env);
			ctxt.popTo(popto);
	
			if (statement.getStmttype().hasReturn())
			{
				updates.add(TCNameToken.getResult(location));
			}
			
			if (!updates.isEmpty())
			{
				ctxt.push(new POAmbiguousContext("for loop", updates, location));
			}
			
			obligations.addAll(loops);
			return obligations;
		}
		else
		{
			POExpression invariant = POLoopInvariantAnnotation.combine(invariants, null);

			/*
			 * The initial case verifies that the invariant is true for the loop "from" value.
			 */
			POAssignmentDefinition def = new POAssignmentDefinition(var, vardef.getType(), from, vardef.getType());
			ctxt.add(new POLetDefContext(def));		// let x = 1 in
			obligations.addAll(LoopInvariantObligation.getAllPOs(invariant.location, ctxt, invariant));
			obligations.lastElement().setMessage("check before for-loop");
			ctxt.pop();

			int popto = ctxt.size();

			/*
			 * A preservation case verifies that if the invariant is true at X, then it is true at X+1
			 */
			TCLocalDefinition tcdef = new TCLocalDefinition(location, var, vardef.getType());
			Environment local = new FlatCheckedEnvironment(tcdef, env, null);
			updates.add(var);

			ctxt.push(new POForAllContext(updates, local));							// forall <changed values> and vars
			ctxt.push(new POImpliesContext(varIsValid(), invariant));	// valid index && invariant => ...
			obligations.addAll(statement.getProofObligations(ctxt, pogState, env));

			def = new POAssignmentDefinition(var, vardef.getType(), varPlusBy(), vardef.getType());
			ctxt.add(new POLetDefContext(def));		// let x = x + 1 in
			obligations.addAll(LoopInvariantObligation.getAllPOs(statement.location, ctxt, invariant));
			obligations.lastElement().setMessage("preservation for next for-loop");

			ctxt.popTo(popto);
			
			/*
			 * Leave implication for following POs, which uses the LoopInvariants that exclude "var"
			 */
			if (!updates.isEmpty()) ctxt.push(new POForAllContext(updates, env));	// forall <changed variables>
			ctxt.push(new POImpliesContext(POLoopInvariantAnnotation.combine(invariants, var)));	// invariant => ...
			
			return obligations;
		}
	}

	private POExpression varIsValid()
	{
		TCRealType real = new TCRealType(location);
		POExpression vexp = new POVariableExpression(var, vardef);
		
		POExpression ge = new POLessEqualExpression(vexp, new LexKeywordToken(Token.GE, location), from, real, real);		// x >= A
		POExpression le = new POLessEqualExpression(vexp, new LexKeywordToken(Token.LE, location), to, real, real);			// x <= B
		POExpression range = new POAndExpression(ge, new LexKeywordToken(Token.AND, location), le, real, real);				// x >= A and x <= B
		
		if (by != null)
		{
			POExpression diff = new POSubtractExpression(vexp, new LexKeywordToken(Token.MINUS, location), from, real, real);	// (x-A)
			POExpression rem = new PORemExpression(diff, new LexKeywordToken(Token.REM, location), by, real, real);				// (x-A) rem C
			POExpression zero = new POIntegerLiteralExpression(new LexIntegerToken(0L, location));
			POExpression equals = new POEqualsExpression(rem, new LexKeywordToken(Token.EQUALS, location), zero, real, real);	// (x-A) rem C == 0

			return new POAndExpression(equals, new LexKeywordToken(Token.AND, location), range, real, real);	// x >= A and x <= B and (x-A) rem C == 0
		}
		else
		{
			return range;
		}
	}

	private POExpression varPlusBy()
	{
		POExpression vexp = new POVariableExpression(var, vardef);
		POExpression one = new POIntegerLiteralExpression(new LexIntegerToken(1L, location));
		POExpression _by = (by == null) ? one : by;
		return new POPlusExpression(vexp, new LexKeywordToken(Token.PLUS, location), _by, from.getExptype(), new TCIntegerType(location));
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseForIndexStatement(this, arg);
	}
}
