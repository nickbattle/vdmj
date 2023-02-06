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

package com.fujitsu.vdmj.po.expressions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POImpliesContext;
import com.fujitsu.vdmj.pog.PONotImpliesContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.typechecker.Environment;

public class POIfExpression extends POExpression
{
	private static final long serialVersionUID = 1L;
	public final POExpression ifExp;
	public final POExpression thenExp;
	public final POElseIfExpressionList elseList;
	public final POExpression elseExp;

	public POIfExpression(LexLocation location,
		POExpression ifExp, POExpression thenExp, POElseIfExpressionList elseList,
		POExpression elseExp)
	{
		super(location);
		this.ifExp = ifExp;
		this.thenExp = thenExp;
		this.elseList = elseList;
		this.elseExp = elseExp;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("(if " + ifExp + "\nthen " + thenExp);

		for (POElseIfExpression s: elseList)
		{
			sb.append("\n");
			sb.append(s.toString());
		}

		if (elseExp != null)
		{
			sb.append("\nelse ");
			sb.append(elseExp.toString());
		}

		sb.append(")");

		return sb.toString();
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, Environment env)
	{
		ProofObligationList obligations = ifExp.getProofObligations(ctxt, env);

		ctxt.push(new POImpliesContext(ifExp));
		obligations.addAll(thenExp.getProofObligations(ctxt, env));
		// obligations.addAll(checkUnionQualifiers(thenExp, TCTypeQualifier.getBoolQualifier(), ctxt));
		ctxt.pop();

		ctxt.push(new PONotImpliesContext(ifExp));	// not (ifExp) =>

		for (POElseIfExpression exp: elseList)
		{
			obligations.addAll(exp.getProofObligations(ctxt, env));
			ctxt.push(new PONotImpliesContext(exp.elseIfExp));
		}

		obligations.addAll(elseExp.getProofObligations(ctxt, env));

		for (int i=0; i<elseList.size(); i++)
		{
			ctxt.pop();
		}

		ctxt.pop();

		return obligations;
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseIfExpression(this, arg);
	}
}
