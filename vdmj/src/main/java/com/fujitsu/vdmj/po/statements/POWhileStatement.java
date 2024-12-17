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
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.WhileLoopObligation;
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
		obligations.add(new WhileLoopObligation(this, ctxt));
		
		obligations.addAll(exp.getProofObligations(ctxt, pogState, env));
		obligations.markIfAmbiguous(pogState, exp);
		
		int popto = ctxt.size();
		POGState copy = pogState.getCopy();
		ProofObligationList loops = statement.getProofObligations(ctxt, copy, env);
		pogState.combineWith(copy);
		ctxt.popTo(popto);

		if (!statement.updatesState().isEmpty())
		{
			loops.markUnchecked(ProofObligation.LOOP_STATEMENT);
		}

		obligations.addAll(loops);
		return obligations;
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseWhileStatement(this, arg);
	}
}
