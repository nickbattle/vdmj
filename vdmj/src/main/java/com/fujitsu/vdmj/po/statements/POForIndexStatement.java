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
import com.fujitsu.vdmj.pog.POForAllSequenceContext;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POScopeContext;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
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

	public POForIndexStatement(LexLocation location,
		TCNameToken var, POExpression from, POExpression to, POExpression by, POStatement body)
	{
		super(location);
		this.var = var;
		this.from = from;
		this.to = to;
		this.by = by;
		this.statement = body;
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
		obligations.markIfUpdated(pogState, from);
		obligations.addAll(to.getProofObligations(ctxt, pogState, env).markIfUpdated(pogState, to));

		if (by != null)
		{
			obligations.addAll(by.getProofObligations(ctxt, pogState, env).markIfUpdated(pogState, by));
		}

		ctxt.push(new POScopeContext());
		ctxt.push(new POForAllSequenceContext(var, from, to, by));
		ProofObligationList loops = statement.getProofObligations(ctxt, pogState, env);
		if (statement.updatesState()) loops.markUnchecked(ProofObligation.LOOP_STATEMENT);
		obligations.addAll(loops);
		ctxt.pop();
		ctxt.pop();

		return obligations;
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseForIndexStatement(this, arg);
	}
}
