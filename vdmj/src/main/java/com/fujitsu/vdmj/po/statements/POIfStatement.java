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
import com.fujitsu.vdmj.pog.POAltContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POGStateList;
import com.fujitsu.vdmj.pog.POImpliesContext;
import com.fujitsu.vdmj.pog.PONotImpliesContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.typechecker.Environment;

public class POIfStatement extends POStatement
{
	private static final long serialVersionUID = 1L;
	public final POExpression ifExp;
	public final POStatement thenStmt;
	public final POElseIfStatementList elseList;
	public final POStatement elseStmt;
	
	public POIfStatement(LexLocation location, POExpression ifExp, POStatement thenStmt,
		POElseIfStatementList elseList, POStatement elseStmt)
	{
		super(location);
		this.ifExp = ifExp;
		this.thenStmt = thenStmt;
		this.elseList = elseList;
		this.elseStmt = elseStmt;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("if " + ifExp + "\nthen\n" + thenStmt);

		for (POElseIfStatement s: elseList)
		{
			sb.append(s.toString());
		}

		if (elseStmt != null)
		{
			sb.append("else\n");
			sb.append(elseStmt.toString());
		}

		return sb.toString();
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		POGStateList stateList = new POGStateList();
		POAltContext altContext = new POAltContext();

		ProofObligationList obligations = ifExp.getProofObligations(ctxt, pogState, env);
		obligations.markIfAmbiguous(pogState, ifExp);
		
		int base = ctxt.pushAt(new POImpliesContext(ifExp));
		obligations.addAll(thenStmt.getProofObligations(ctxt, stateList.addCopy(pogState), env));
		ctxt.popInto(base, altContext.add());

		ctxt.push(new PONotImpliesContext(ifExp));	// not (ifExp) =>

		for (POElseIfStatement stmt: elseList)
		{
			ProofObligationList oblist = stmt.elseIfExp.getProofObligations(ctxt, pogState, env);
			oblist.markIfAmbiguous(pogState, stmt.elseIfExp);

			int popto = ctxt.pushAt(new POImpliesContext(stmt.elseIfExp));
			oblist.addAll(stmt.thenStmt.getProofObligations(ctxt, stateList.addCopy(pogState), env));
			ctxt.copyInto(base, altContext.add());
			ctxt.popTo(popto);
			obligations.addAll(oblist);

			ctxt.push(new PONotImpliesContext(stmt.elseIfExp));
		}

		if (elseStmt != null)
		{
			int popto = ctxt.size();
			obligations.addAll(elseStmt.getProofObligations(ctxt, stateList.addCopy(pogState), env));
			ctxt.copyInto(base, altContext.add());
			ctxt.popTo(popto);
		}
		else
		{
			ctxt.copyInto(base, altContext.add());	// eg. for an if with no else
		}

		ctxt.popTo(base);
		stateList.combineInto(pogState, false);
		// ctxt.push(new POAmbiguousContext("if statement", pogState, location));
		ctxt.push(altContext);
		
		return obligations;
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseIfStatement(this, arg);
	}
}
