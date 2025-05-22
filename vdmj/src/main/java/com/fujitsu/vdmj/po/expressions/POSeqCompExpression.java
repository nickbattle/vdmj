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
import com.fujitsu.vdmj.po.patterns.POBind;
import com.fujitsu.vdmj.pog.POForAllPredicateContext;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POForAllContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.types.TCTypeQualifier;
import com.fujitsu.vdmj.typechecker.Environment;

public class POSeqCompExpression extends POSeqExpression
{
	private static final long serialVersionUID = 1L;
	public final POExpression first;
	public final POBind bind;
	public final POExpression predicate;

	public POSeqCompExpression(LexLocation start,
		POExpression first, POBind bind, POExpression predicate)
	{
		super(start);
		this.first = first;
		this.bind = bind;
		this.predicate = predicate;
	}

	@Override
	public String toString()
	{
		return "[" + first + " | " + bind +
			(predicate == null ? "]" : " & " + predicate + "]");
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = new ProofObligationList();

		ctxt.push(new POForAllPredicateContext(this));
		obligations.addAll(first.getProofObligations(ctxt, pogState, env));
		ctxt.pop();

		obligations.addAll(bind.getProofObligations(ctxt, pogState, env));

		if (predicate != null)
		{
    		ctxt.push(new POForAllContext(this));
    		obligations.addAll(predicate.getProofObligations(ctxt, pogState, env));
    		obligations.addAll(checkUnionQualifiers(predicate, TCTypeQualifier.getBoolQualifier(), ctxt));
    		ctxt.pop();
		}

		return obligations;
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSeqCompExpression(this, arg);
	}
}
