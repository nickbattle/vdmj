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
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.typechecker.Environment;

public class POSpecificationStatement extends POStatement
{
	private static final long serialVersionUID = 1L;
	public final POExternalClauseList externals;
	public final POExpression precondition;
	public final POExpression postcondition;
	public final POErrorCaseList errors;

	public POSpecificationStatement(LexLocation location,
		POExternalClauseList externals, POExpression precondition,
		POExpression postcondition, POErrorCaseList errors)
	{
		super(location);

		this.externals = externals;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.errors = errors;
	}

	@Override
	public String toString()
	{
		return "[" +
    		(externals == null ? "" : "\n\text " + externals) +
    		(precondition == null ? "" : "\n\tpre " + precondition) +
    		(postcondition == null ? "" : "\n\tpost " + postcondition) +
    		(errors == null ? "" : "\n\terrs " + errors) + "]";
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POContextStack globals, Environment env)
	{
		ProofObligationList obligations = new ProofObligationList();

		if (errors != null)
		{
			for (POErrorCase err: errors)
			{
				obligations.addAll(err.left.getProofObligations(ctxt, env));
				obligations.addAll(err.right.getProofObligations(ctxt, env));
			}
		}

		if (precondition != null)
		{
			obligations.addAll(precondition.getProofObligations(ctxt, env));
		}

		if (postcondition != null)
		{
			obligations.addAll(postcondition.getProofObligations(ctxt, env));
		}

		return obligations;
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSpecificationStatement(this, arg);
	}
}
