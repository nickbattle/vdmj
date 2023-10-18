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
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.types.TCTypeQualifier;
import com.fujitsu.vdmj.typechecker.Environment;

public class POElementsExpression extends POUnaryExpression
{
	private static final long serialVersionUID = 1L;

	public POElementsExpression(LexLocation location, POExpression exp)
	{
		super(location, exp);
	}

	@Override
	public String toString()
	{
		return "(elems (" + exp + "))";
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, Environment env)
	{
		ProofObligationList obligations = exp.getProofObligations(ctxt, env);
		obligations.addAll(checkUnionQualifiers(exp, TCTypeQualifier.getSetQualifier(), ctxt));
		return obligations;
	}
	
	@Override
	protected TCTypeQualifier getQualifier()
	{
		return TCTypeQualifier.getSetQualifier();
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseElementsExpression(this, arg);
	}
}
