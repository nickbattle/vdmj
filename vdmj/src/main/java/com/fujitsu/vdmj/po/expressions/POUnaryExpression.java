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
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeQualifier;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.typechecker.Environment;

abstract public class POUnaryExpression extends POExpression
{
	private static final long serialVersionUID = 1L;
	public final POExpression exp;

	public POUnaryExpression(LexLocation location, POExpression exp)
	{
		super(location);
		this.exp = exp;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList list = exp.getProofObligations(ctxt, pogState, env);
		
		if (exp.getExptype() != null && exp.getExptype().isUnion(location))
		{
			TCUnionType ut = exp.getExptype().getUnion();
			TCTypeSet sets = ut.getMatches(getQualifier());
			
			if (sets.size() < ut.types.size())
			{
				list.add(new SubTypeObligation(exp, sets.getType(location), exp.getExptype(), ctxt));
			}
		}
		
		return list;
	}

	abstract protected TCTypeQualifier getQualifier();

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseUnaryExpression(this, arg);
	}

	protected TCTypeQualifier getSetQualifier()
	{
		return new TCTypeQualifier()
		{
			@Override
			public boolean matches(TCType member)
			{
				return member.isSet(location);
			}
		};
	}
}
