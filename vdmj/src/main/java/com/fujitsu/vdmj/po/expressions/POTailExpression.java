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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.po.expressions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.pog.NonEmptySeqObligation;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.types.TCSeq1Type;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeQualifier;
import com.fujitsu.vdmj.typechecker.Environment;

public class POTailExpression extends POUnaryExpression
{
	private static final long serialVersionUID = 1L;
	private final TCType etype;

	public POTailExpression(LexLocation location, POExpression exp, TCType etype)
	{
		super(location, exp);
		this.etype = etype;
	}

	@Override
	public String toString()
	{
		return "(tl " + exp + ")";
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = super.getProofObligations(ctxt, pogState, env);
		
		if (!etype.isAlways(TCSeq1Type.class, location))
		{
			obligations.addAll(NonEmptySeqObligation.getAllPOs(exp, ctxt));
		}
		
		return obligations;
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseTailExpression(this, arg);
	}

	@Override
	protected TCTypeQualifier getQualifier()
	{
		return TCTypeQualifier.getSeqQualifier();
	}
}
