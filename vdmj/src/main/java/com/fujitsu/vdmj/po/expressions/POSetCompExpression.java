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
import com.fujitsu.vdmj.po.patterns.POMultipleBind;
import com.fujitsu.vdmj.po.patterns.POMultipleBindList;
import com.fujitsu.vdmj.po.patterns.POMultipleTypeBind;
import com.fujitsu.vdmj.pog.FiniteSetObligation;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POForAllContext;
import com.fujitsu.vdmj.pog.POForAllPredicateContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.util.Utils;

public class POSetCompExpression extends POSetExpression
{
	private static final long serialVersionUID = 1L;
	public final POExpression first;
	public final POMultipleBindList bindings;
	public final POExpression predicate;
	public final TCSetType settype;

	public POSetCompExpression(LexLocation start,
		POExpression first, POMultipleBindList bindings, POExpression predicate, TCSetType settype)
	{
		super(start);
		this.first = first;
		this.bindings = bindings;
		this.predicate = predicate;
		this.settype = settype;
	}

	@Override
	public String toString()
	{
		return "{" + first + " | " + Utils.listToString(bindings) +
			(predicate == null ? "}" : " & " + predicate + "}");
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, Environment env)
	{
		ProofObligationList obligations = new ProofObligationList();

		ctxt.push(new POForAllPredicateContext(this));
		obligations.addAll(first.getProofObligations(ctxt, env));
		ctxt.pop();

		boolean finiteTest = false;

		for (POMultipleBind mb: bindings)
		{
			obligations.addAll(mb.getProofObligations(ctxt, env));

			if (mb instanceof POMultipleTypeBind)
			{
				finiteTest = true;
			}
		}

		if (finiteTest)
		{
			obligations.add(new FiniteSetObligation(this, settype, ctxt));
		}

		if (predicate != null)
		{
    		ctxt.push(new POForAllContext(this));
    		obligations.addAll(predicate.getProofObligations(ctxt, env));
    		ctxt.pop();
		}

		return obligations;
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSetCompExpression(this, arg);
	}
}
