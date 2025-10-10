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

import com.fujitsu.vdmj.ast.lex.LexIntegerToken;
import com.fujitsu.vdmj.ast.lex.LexKeywordToken;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.po.annotations.POLoopAnnotations;
import com.fujitsu.vdmj.po.annotations.POLoopInvariantList;
import com.fujitsu.vdmj.po.definitions.POAssignmentDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.expressions.POAndExpression;
import com.fujitsu.vdmj.po.expressions.POEqualsExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POGreaterExpression;
import com.fujitsu.vdmj.po.expressions.POIntegerLiteralExpression;
import com.fujitsu.vdmj.po.expressions.POLessEqualExpression;
import com.fujitsu.vdmj.po.expressions.POPlusExpression;
import com.fujitsu.vdmj.po.expressions.PORemExpression;
import com.fujitsu.vdmj.po.expressions.POSubtractExpression;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.LoopInvariantObligation;
import com.fujitsu.vdmj.pog.POAltContext;
import com.fujitsu.vdmj.pog.POAmbiguousContext;
import com.fujitsu.vdmj.pog.POCommentContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POForAllContext;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POImpliesContext;
import com.fujitsu.vdmj.pog.POLetDefContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class POForIndexStatement extends POStatement
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken var;
	public final POExpression from;
	public final POExpression to;
	public final POExpression by;
	public final POStatement statement;
	public final PODefinition vardef;
	public final POLoopAnnotations invariants;

	public POForIndexStatement(LexLocation location,
		TCNameToken var, POExpression from, POExpression to, POExpression by, POStatement body,
		PODefinition vardef, POLoopAnnotations invariants)
	{
		super(location);
		this.var = var;
		this.from = from;
		this.to = to;
		this.by = by;
		this.statement = body;
		this.vardef = vardef;
		this.invariants = invariants;
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
		pogState.setAmbiguous(false);
		ProofObligationList obligations = new ProofObligationList();

		POExpression efrom = extractOpCalls(from, obligations, pogState, ctxt, env);
		POExpression eto   = extractOpCalls(to, obligations, pogState, ctxt, env);
		POExpression eby   = null;

		obligations.addAll(efrom.getProofObligations(ctxt, pogState, env));
		obligations.addAll(eto.getProofObligations(ctxt, pogState, env));

		if (by != null)
		{
			eby = extractOpCalls(by, obligations, pogState, ctxt, env);
			obligations.addAll(eby.getProofObligations(ctxt, pogState, env));
		}

		boolean varAmbiguous = pogState.isAmbiguous();
		POLoopInvariantList annotations = invariants.getList();
		TCNameSet updates = statement.updatesState(ctxt);
		POExpression invariant = null;

		if (!annotations.isEmpty())
		{
			invariant = annotations.combine(true);
		}

		POAltContext altCtxt = new POAltContext();

		if (varAmbiguous)
		{
			ctxt.push(new POAmbiguousContext("loop var", var, location));
		}

		if (invariant == null)
		{
			ctxt.push(new POCommentContext("Missing @LoopInvariant, assuming true", location));
			PODefinition loopinv = getLoopInvDef();
			ctxt.push(new POLetDefContext(loopinv));
			invariant = new POVariableExpression(loopinv.name, loopinv);
		}

		int popto = ctxt.size();	// Includes missing invariant above

		/**
		 * The initial case verifies that the invariant is true before the loop.
		 */
		obligations.addAll(LoopInvariantObligation.getAllPOs(invariant.location, ctxt, invariant));
		obligations.lastElement().setMessage("check invariant before for-loop");

		TCLocalDefinition tcdef = new TCLocalDefinition(location, var, vardef.getType());
		Environment local = new FlatCheckedEnvironment(tcdef, env, NameScope.NAMES);
		updates.add(var);

		if (!annotations.isEmpty())
		{
			invariant = annotations.combine(false);	// Don't exclude loop vars now
		}

		/**
		 * Push an implication that the loop range is not empty. This applies to everything
		 * from here on, since the loop only has effects/POs if it is entered. At the end, we cover
		 * the isEmpty() case in another altpath.
		 */
		ctxt.push(new POImpliesContext(isNotEmpty()));

		/**
		 * The start of the loop verifies that the first "from" value can start the loop and
		 * will meet the invariant.
		 */
		POAssignmentDefinition def = new POAssignmentDefinition(var, vardef.getType(), efrom, vardef.getType());
		ctxt.push(new POLetDefContext(def));						// eg. let x = 1 in
		obligations.addAll(LoopInvariantObligation.getAllPOs(invariant.location, ctxt, invariant));
		obligations.lastElement().setMessage("check invariant for first for-loop");
		ctxt.pop();

		/**
		 * The preservation case verifies that if the invariant is true at X, then it is true at X+by
		 */
		ctxt.push(new POForAllContext(updates, local));								// forall <changed values> and vars
		ctxt.push(new POImpliesContext(varIsValid(efrom, eto, eby), invariant));	// valid index && invariant => ...
		obligations.addAll(statement.getProofObligations(ctxt, pogState, env));

		def = new POAssignmentDefinition(var, vardef.getType(), varPlusBy(eby), vardef.getType());
		ctxt.add(new POLetDefContext(def));							// let x = x + by in
		obligations.addAll(LoopInvariantObligation.getAllPOs(statement.location, ctxt, invariant));
		obligations.lastElement().setMessage("invariant preservation for next for-loop");

		/**
		 * The context stack now contains everything from the statement block, but we want to
		 * suppress this, since context beyond the loop only includes the invariant statement.
		 * But we can't just discard it, because it may include some returns, which are subsequently
		 * needed by postcondition checks. So we extract the substack, and reduce it to just the
		 * paths with returnsEarly().
		 */
		POContextStack stack = new POContextStack();
		ctxt.popInto(popto, stack);
		altCtxt.addAll(stack.reduce());

		/**
		 * The context stack beyond the loop just contains the loop invariant unless there are
		 * return paths from the above. Invariant clauses that mention the loop variable are omitted.
		 */
		updates.remove(var);

		if (!annotations.isEmpty())
		{
			invariant = annotations.combine(true);
		}

		ctxt.push(new POImpliesContext(isNotEmpty()));		// from <= to =>
		ctxt.push(new POForAllContext(updates, env));		// forall <changed variables>
		ctxt.push(new POImpliesContext(invariant));			// invariant => ...
		ctxt.popInto(popto, altCtxt.add());

		/**
		 * Finally, the loop may not have been entered if the range is empty, so we create
		 * another alternative path with this condition and nothing else.
		 */
		ctxt.push(new POImpliesContext(isEmpty()));
		ctxt.push(new POCommentContext("Did not enter loop", location));
		ctxt.push(new POImpliesContext(invariant));			// invariant => ...
		ctxt.popInto(popto, altCtxt.add());

		// The three alternatives in one added.
		ctxt.push(altCtxt);
		
		return obligations;
	}

	/**
	 * Produce "from > to"
	 */
	private POExpression isEmpty()
	{
		TCRealType real = new TCRealType(location);

		return new POGreaterExpression(
			from,
			new LexKeywordToken(Token.GT, location),
			to,
			real, real);
	}

	/**
	 * Produce "from <= to"
	 */
	private POExpression isNotEmpty()
	{
		TCRealType real = new TCRealType(location);

		return new POLessEqualExpression(
			from,
			new LexKeywordToken(Token.LE, location),
			to,
			real, real);
	}

	private POExpression varIsValid(POExpression efrom, POExpression eto, POExpression eby)
	{
		TCRealType real = new TCRealType(location);
		POExpression vexp = new POVariableExpression(var, vardef);
		
		POExpression ge = new POLessEqualExpression(vexp, new LexKeywordToken(Token.GE, location), efrom, real, real);		// x >= A
		POExpression le = new POLessEqualExpression(vexp, new LexKeywordToken(Token.LE, location), eto, real, real);		// x <= B
		POExpression range = new POAndExpression(ge, new LexKeywordToken(Token.AND, location), le, real, real);				// x >= A and x <= B
		
		if (eby != null)
		{
			POExpression diff = new POSubtractExpression(vexp, new LexKeywordToken(Token.MINUS, location), from, real, real);	// (x-A)
			POExpression rem = new PORemExpression(diff, new LexKeywordToken(Token.REM, location), eby, real, real);			// (x-A) rem C
			POExpression zero = new POIntegerLiteralExpression(LexIntegerToken.ZERO);
			POExpression equals = new POEqualsExpression(rem, new LexKeywordToken(Token.EQUALS, location), zero, real, real);	// (x-A) rem C == 0

			return new POAndExpression(equals, new LexKeywordToken(Token.AND, location), range, real, real);	// x >= A and x <= B and (x-A) rem C == 0
		}
		else
		{
			return range;
		}
	}

	private POExpression varPlusBy(POExpression eby)
	{
		POExpression vexp = new POVariableExpression(var, vardef);
		TCType bytype = (eby == null) ? new TCIntegerType(location) : eby.getExptype();
		POExpression _by = (eby == null) ? new POIntegerLiteralExpression(LexIntegerToken.ONE) : eby;

		return new POPlusExpression(
			vexp,
			new LexKeywordToken(Token.PLUS, location),
			_by,
			from.getExptype(), bytype);
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseForIndexStatement(this, arg);
	}
}
