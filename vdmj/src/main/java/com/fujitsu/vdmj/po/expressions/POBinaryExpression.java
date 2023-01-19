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

import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeQualifier;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.typechecker.Environment;

abstract public class POBinaryExpression extends POExpression
{
	private static final long serialVersionUID = 1L;

	public final POExpression left;
	public final POExpression right;
	public final LexToken op;
	public final TCType ltype;
	public final TCType rtype;

	public POBinaryExpression(POExpression left, LexToken op, POExpression right,
		TCType ltype, TCType rtype)
	{
		super(op.location);
		this.left = left;
		this.right = right;
		this.op = op;
		this.ltype = ltype;
		this.rtype = rtype;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, Environment env)
	{
		ProofObligationList obligations = new ProofObligationList();
		obligations.addAll(left.getProofObligations(ctxt, env));
		obligations.addAll(right.getProofObligations(ctxt, env));

		if (left.getExptype().isUnion(location))
		{
			TCUnionType ut = left.getExptype().getUnion();
			TCTypeSet sets = ut.getMatches(getLeftQualifier());
			
			if (sets.size() < ut.types.size())
			{
				obligations.add(new SubTypeObligation(left, sets.getType(location), left.getExptype(), ctxt));
			}
		}
		
		if (right.getExptype().isUnion(location))
		{
			TCUnionType ut = right.getExptype().getUnion();
			TCTypeSet sets = ut.getMatches(getRightQualifier());
			
			if (sets.size() < ut.types.size())
			{
				obligations.add(new SubTypeObligation(right, sets.getType(location), right.getExptype(), ctxt));
			}
		}
		
		return obligations;
	}

	abstract protected TCTypeQualifier getLeftQualifier();
	abstract protected TCTypeQualifier getRightQualifier();

	@Override
	public String toString()
	{
		return "(" + left + " " + op + " " + right + ")";
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseBinaryExpression(this, arg);
	}
}
