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
import com.fujitsu.vdmj.po.patterns.POIgnorePattern;
import com.fujitsu.vdmj.pog.CasesExhaustiveObligation;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.util.Utils;

public class POCasesExpression extends POExpression
{
	private static final long serialVersionUID = 1L;
	public final POExpression exp;
	public final POCaseAlternativeList cases;
	public final POExpression others;
	public final TCType expType;

	public POCasesExpression(LexLocation location, POExpression exp,
		POCaseAlternativeList cases, POExpression others, TCType expType)
	{
		super(location);
		this.exp = exp;
		this.cases = cases;
		this.others = others;
		this.expType = expType;
	}

	@Override
	public String toString()
	{
		return "(cases " + exp + " :\n" +
			Utils.listToString("", cases, ",\n", "") +
			(others == null ? "\n" : ",\nothers -> " + others + "\n") + "end)";
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, Environment env)
	{
		ProofObligationList obligations = exp.getProofObligations(ctxt, env);

		int count = 0;
		boolean hasIgnore = false;
		TCNameList hidden = new TCNameList();
		ProofObligationList _obligations = new ProofObligationList();

		for (POCaseAlternative alt: cases)
		{
			if (alt.pattern instanceof POIgnorePattern)
			{
				hasIgnore = true;
			}
			
			hidden.addAll(alt.pattern.getHiddenVariables());	// cumulative
			
			// PONotCaseContext pushed by the POCaseAlternative...
			_obligations.addAll(alt.getProofObligations(ctxt, expType, env));
			count++;
		}
		
		if (others != null)
		{
			_obligations.addAll(others.getProofObligations(ctxt, env));
		}

		for (int i=0; i<count; i++)
		{
			ctxt.pop();
		}

		if (others == null && !hasIgnore)
		{
			_obligations.add(new CasesExhaustiveObligation(this, ctxt));
		}


		if (!hidden.isEmpty())
		{
			_obligations.markUnchecked("Obligation patterns contain hidden variables: " + hidden);
		}

		obligations.addAll(_obligations);
		return obligations;
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseCasesExpression(this, arg);
	}
}
