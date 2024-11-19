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
import com.fujitsu.vdmj.po.patterns.POIgnorePattern;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POGStateList;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;

public class POCasesStatement extends POStatement
{
	private static final long serialVersionUID = 1L;
	public final POExpression exp;
	public final POCaseStmtAlternativeList cases;
	public final POStatement others;
	public final TCType expType;

	public POCasesStatement(LexLocation location,
		POExpression exp, POCaseStmtAlternativeList cases, POStatement others, TCType expType)
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
		StringBuilder sb = new StringBuilder();
		sb.append("cases " + exp + " :\n");

		for (POCaseStmtAlternative csa: cases)
		{
			sb.append("  ");
			sb.append(csa.toString());
		}

		if (others != null)
		{
			sb.append("  others -> ");
			sb.append(others.toString());
		}

		sb.append("esac");
		return sb.toString();
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = exp.getProofObligations(ctxt, pogState, env);
		obligations.markIfUpdated(pogState, exp);		
		
		POGStateList stateList = new POGStateList();
		boolean hasIgnore = false;

		for (POCaseStmtAlternative alt: cases)
		{
			if (alt.pattern instanceof POIgnorePattern)
			{
				hasIgnore = true;
			}

			// Pushes PONotCaseContext
			obligations.addAll(alt.getProofObligations(ctxt, stateList.addCopy(pogState), expType, env));
		}

		if (others != null && !hasIgnore)
		{
			obligations.addAll(others.getProofObligations(ctxt, stateList.addCopy(pogState), env));
		}

		for (int i=0; i<cases.size(); i++)
		{
			ctxt.pop();
		}
		
		stateList.combineInto(pogState);

		return obligations;
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseCasesStatement(this, arg);
	}
}
