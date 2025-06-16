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

import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.pog.FuncComposeObligation;
import com.fujitsu.vdmj.pog.MapComposeObligation;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeQualifier;
import com.fujitsu.vdmj.typechecker.Environment;

public class POCompExpression extends POBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public POCompExpression(POExpression left, LexToken op, POExpression right, TCType ltype, TCType rtype)
	{
		super(left, op, right, ltype, rtype);
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = super.getProofObligations(ctxt, pogState, env);

		if (ltype.isFunction(location))
		{
			String pref1 = left.getPreName();
			String pref2 = right.getPreName();

			if (pref1 == null || !pref1.equals(""))
			{
				obligations.addAll(FuncComposeObligation.getAllPOs(this, pref1, pref2, ctxt));
			}
		}

		if (ltype.isMap(location))
		{
			obligations.addAll(MapComposeObligation.getAllPOs(this, ctxt));
		}

		return obligations;
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseCompExpression(this, arg);
	}

	@Override
	protected TCTypeQualifier getLeftQualifier()
	{
		return new TCTypeQualifier()
		{
			@Override
			public boolean matches(TCType member)
			{
				return member.isFunction(location) || member.isMap(location);
			}
		};
	}

	@Override
	protected TCTypeQualifier getRightQualifier()
	{
		return TCTypeQualifier.getAnyQualifier();
	}
}
