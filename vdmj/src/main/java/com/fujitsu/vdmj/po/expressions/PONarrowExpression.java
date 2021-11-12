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
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class PONarrowExpression extends POExpression
{
	private static final long serialVersionUID = 1L;
	public final TCType basictype;
	public final TCNameToken typename;
	public final POExpression test;
	public final PODefinition typedef;
	public final TCType testtype;

	public PONarrowExpression(LexLocation location, TCType basictype, TCNameToken typename, POExpression test,
		PODefinition typedef, TCType testtype)
	{
		super(location);
		this.basictype = basictype;
		this.typename = typename;
		this.test = test;
		this.typedef = typedef;
		this.testtype = testtype;
	}

	@Override
	public String toString()
	{
		return "narrow_(" + test + ", " + (typename == null ? basictype : typename) + ")";
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, Environment env)
	{
		ProofObligationList obligations = new ProofObligationList();
		
		TCType expected = (typedef == null ? basictype : typedef.getType());
		ctxt.noteType(test, expected);

		if (!TypeComparator.isSubType(testtype, expected))
		{
			obligations.add(new SubTypeObligation(test, expected, testtype, ctxt));
		}

		obligations.addAll(test.getProofObligations(ctxt, env));
		return obligations;
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseNarrowExpression(this, arg);
	}
}
