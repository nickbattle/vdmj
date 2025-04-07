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
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.PONotExpression;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.LoopInvariantObligation;
import com.fujitsu.vdmj.pog.POAmbiguousContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POImpliesContext;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.typechecker.Environment;

public class POWhileStatement extends POStatement
{
	private static final long serialVersionUID = 1L;
	public final POExpression exp;
	public final POStatement statement;

	public POWhileStatement(LexLocation location, POExpression exp, POStatement body)
	{
		super(location);
		this.exp = exp;
		this.statement = body;
	}

	@Override
	public String toString()
	{
		return "while " + exp + " do " + statement;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = new ProofObligationList();
		obligations.addAll(exp.getProofObligations(ctxt, pogState, env));

		POLoopInvariantAnnotation annotation = annotations.getInstance(POLoopInvariantAnnotation.class);
		TCNameSet updates = statement.updatesState();
		String warning = null;
		
		if (annotation != null)
		{
			TCNameSet reasonsAbout = annotation.invariant.getVariableNames();
			
			if (!reasonsAbout.containsAll(updates))
			{
				// Invariant doesn't reason about some variable updated, so delete the
				// annotation and make things ambiguous :-)
				annotation = null;
				TCNameSet missing = new TCNameSet();
				missing.addAll(updates);
				missing.removeAll(reasonsAbout);
				warning = "@LoopInvariant does not reason about " + missing;
			}
		}
		
		if (annotation == null)		// No loop invariant defined
		{
			ProofObligation loop = new LoopInvariantObligation(location, ctxt);
			loop.setMessage(warning);
			obligations.add(loop);
			
			int popto = ctxt.size();
			POGState copy = pogState.getCopy();
			ProofObligationList loops = statement.getProofObligations(ctxt, copy, env);
			pogState.combineWith(copy);
			ctxt.popTo(popto);

			if (!updates.isEmpty())
			{
				ctxt.push(new POAmbiguousContext("while loop", updates, location));
			}

			obligations.addAll(loops);
			return obligations;
		}
		else
		{
			// Note: location of first loop check is the @LoopInvariant itself.
			ProofObligation initial = new LoopInvariantObligation(annotation.location, ctxt, annotation.invariant);
			initial.setMessage("check before while condition");
			obligations.add(initial);
			
			int popto = ctxt.size();
			POGState copy = pogState.getCopy();
			
			ctxt.push(new POImpliesContext(this.exp));	// while C => ...
			ProofObligation before = new LoopInvariantObligation(statement.location, ctxt, annotation.invariant);
			before.setMessage("check before while body");
			obligations.add(before);
			ctxt.pop();
			ctxt.push(new POImpliesContext(annotation.invariant, this.exp));	// invariant && while C => ...
			
			obligations.addAll(statement.getProofObligations(ctxt, copy, env));
			
			ProofObligation after = new LoopInvariantObligation(statement.location, ctxt, annotation.invariant);
			after.setMessage("check after while body");
			obligations.add(after);

			pogState.combineWith(copy);
			ctxt.popTo(popto);
			
			// Leave implication for following POs
			POExpression negated = new PONotExpression(location, this.exp);
			ctxt.push(new POImpliesContext(annotation.invariant, negated));	// invariant && not C => ...
			
			return obligations;
		}
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseWhileStatement(this, arg);
	}
}
