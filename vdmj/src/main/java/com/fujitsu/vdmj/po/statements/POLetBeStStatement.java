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
import com.fujitsu.vdmj.po.patterns.POMultipleBind;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.LetBeExistsObligation;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POForAllContext;
import com.fujitsu.vdmj.pog.POForAllPredicateContext;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.typechecker.Environment;

public class POLetBeStStatement extends POStatement
{
	private static final long serialVersionUID = 1L;
	public final POMultipleBind bind;
	public final POExpression suchThat;
	public final POStatement statement;

	public POLetBeStStatement(LexLocation location,
		POMultipleBind bind, POExpression suchThat, POStatement statement)
	{
		super(location);
		this.bind = bind;
		this.suchThat = suchThat;
		this.statement = statement;
	}

	@Override
	public String toString()
	{
		return "let " + bind +
			(suchThat == null ? "" : " be st " + suchThat) + " in " + statement;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = new ProofObligationList();
		obligations.add(new LetBeExistsObligation(this, ctxt));
		obligations.addAll(bind.getProofObligations(ctxt, pogState, env));

		if (suchThat != null)
		{
			ctxt.push(new POForAllContext(this));
			ProofObligationList oblist = suchThat.getProofObligations(ctxt, pogState, env);
			oblist.stateUpdate(pogState, suchThat);
			obligations.addAll(oblist);
			ctxt.pop();
		}

		ctxt.push(new POForAllPredicateContext(this));
		obligations.addAll(statement.getProofObligations(ctxt, pogState, env));
		ctxt.pop();

		return obligations;
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseLetBeStStatement(this, arg);
	}
}
