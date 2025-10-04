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
import com.fujitsu.vdmj.po.annotations.POLoopAnnotations;
import com.fujitsu.vdmj.po.annotations.POLoopInvariantList;
import com.fujitsu.vdmj.po.annotations.POLoopMeasureAnnotation;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.PONotExpression;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.LoopInvariantObligation;
import com.fujitsu.vdmj.pog.LoopMeasureObligation;
import com.fujitsu.vdmj.pog.POAltContext;
import com.fujitsu.vdmj.pog.POCommentContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POForAllContext;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POImpliesContext;
import com.fujitsu.vdmj.pog.POLetDefContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.typechecker.Environment;

public class POWhileStatement extends POStatement
{
	private static final long serialVersionUID = 1L;
	public final POExpression exp;
	public final POStatement statement;
	public final POLoopAnnotations invariants;

	public POWhileStatement(LexLocation location, POExpression exp, POStatement body, POLoopAnnotations invariants)
	{
		super(location);
		this.exp = exp;
		this.statement = body;
		this.invariants = invariants;
	}

	@Override
	public String toString()
	{
		return "while " + exp + " do " + statement;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = exp.getProofObligations(ctxt, pogState, env);

		POLoopInvariantList annotations = invariants.getList();
		POLoopMeasureAnnotation measure = invariants.getMeasure();
		TCNameSet updates = statement.updatesState(ctxt);
		POExpression invariant = null;
		
		if (!annotations.isEmpty())
		{
			invariant = annotations.combine(false);
		}

		if (measure != null)
		{
			int popto = ctxt.size();

			ctxt.push(new POForAllContext(updates, env));				// forall <changed variables>
			ctxt.push(new POImpliesContext(invariant, this.exp));		// while invariant && C => ...
			ctxt.push(new POLetDefContext(measure.getDefinition()));	// let loop_measure_n = <exp> in ...

			statement.getProofObligations(ctxt, pogState, env);			// build context, ignore POs
			obligations.addAll(LoopMeasureObligation.getAllPOs(statement.location, ctxt, measure));
			obligations.lastElement().setMessage("check measure after each while body");

			ctxt.popTo(popto);
		}
		
		POAltContext altCtxt = new POAltContext();

		if (invariant == null)
		{
			ctxt.push(new POCommentContext("Missing @LoopInvariant, assuming true", location));
			PODefinition loopinv = getLoopInvDef();
			ctxt.push(new POLetDefContext(loopinv));
			invariant = new POVariableExpression(loopinv.name, loopinv);
		}

		int popto = ctxt.size();	// Includes missing invariant above

		obligations.addAll(LoopInvariantObligation.getAllPOs(invariant.location, ctxt, invariant));
		obligations.lastElement().setMessage("check invariant before while condition");
		
		ctxt.push(new POImpliesContext(this.exp));					// while C => ...
		obligations.addAll(LoopInvariantObligation.getAllPOs(statement.location, ctxt, invariant));
		obligations.lastElement().setMessage("check invariant before first while body");
		ctxt.pop();

		ctxt.push(new POForAllContext(updates, env));				// forall <changed variables>
		ctxt.push(new POImpliesContext(invariant, this.exp));		// invariant && while C => ...
		obligations.addAll(statement.getProofObligations(ctxt, pogState, env));
		obligations.addAll(LoopInvariantObligation.getAllPOs(statement.location, ctxt, invariant));
		obligations.lastElement().setMessage("check invariant after each while body");

		/**
		 * The context stack now contains everything from the statement block, but we want to
		 * suppress this, since context beyond the loop only includes the invariant statement and
		 * the failed loop condition. But we can't just discard it, because it may include some
		 * returns, which are subsequently needed by postcondition checks. So we extract the substack,
		 * and reduce it to just the paths with returnsEarly().
		 */
		POContextStack stack = new POContextStack();
		ctxt.popInto(popto, stack);
		altCtxt.addAll(stack.reduce());

		/**
		 * The context stack beyond the loop just contains the loop invariant and failed loop condition,
		 * unless there are return paths from the above.
		 */
		POExpression negated = new PONotExpression(location, this.exp);
		ctxt.push(new POForAllContext(updates, env));			// forall <changed variables>
		ctxt.push(new POImpliesContext(invariant, negated));	// invariant && not C => ...
		ctxt.popInto(popto, altCtxt.add());

		// The two alternatives in one added.
		ctxt.push(altCtxt);

		return obligations;
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseWhileStatement(this, arg);
	}
}
