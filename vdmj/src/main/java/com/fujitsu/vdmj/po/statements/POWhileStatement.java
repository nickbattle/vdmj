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
import com.fujitsu.vdmj.po.annotations.POLoopInvariantAnnotation;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.PONotExpression;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.LoopInvariantObligation;
import com.fujitsu.vdmj.pog.POAmbiguousContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POForAllContext;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POImpliesContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
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
		
		if (annotation == null)		// No loop invariant defined
		{
			obligations.add(new LoopInvariantObligation(location, ctxt));
			
			int popto = ctxt.size();
			ctxt.push(new POImpliesContext(this.exp));	// while C => ...
			ProofObligationList loops = statement.getProofObligations(ctxt, pogState, env);
			ctxt.popTo(popto);

			if (statement.getStmttype().hasReturn())
			{
				updates.add(TCNameToken.getResult(location));
			}
			
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
			obligations.addAll(LoopInvariantObligation.getAllPOs(annotation.location, ctxt, annotation.invariant));
			obligations.lastElement().setMessage("check before while condition");
			
			int popto = ctxt.size();
			
			ctxt.push(new POImpliesContext(this.exp));								// while C => ...
			obligations.addAll(LoopInvariantObligation.getAllPOs(statement.location, ctxt, annotation.invariant));
			obligations.lastElement().setMessage("check before each while body");
			ctxt.pop();

			if (!updates.isEmpty())	ctxt.push(new POForAllContext(updates, env));	// forall <changed variables>
			ctxt.push(new POImpliesContext(annotation.invariant, this.exp));		// invariant && while C => ...
			obligations.addAll(statement.getProofObligations(ctxt, pogState, env));
			obligations.addAll(LoopInvariantObligation.getAllPOs(statement.location, ctxt, annotation.invariant));
			obligations.lastElement().setMessage("check after each while body");
			ctxt.popTo(popto);
			
			// Leave implication for following POs
			POExpression negated = new PONotExpression(location, this.exp);
			if (!updates.isEmpty()) ctxt.push(new POForAllContext(updates, env));	// forall <changed variables>
			ctxt.push(new POImpliesContext(annotation.invariant, negated));			// invariant && not C => ...
			
			return obligations;
		}
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseWhileStatement(this, arg);
	}
}
