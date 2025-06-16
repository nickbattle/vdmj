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

import com.fujitsu.vdmj.ast.lex.LexIntegerToken;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeQualifier;
import com.fujitsu.vdmj.typechecker.Environment;

public class POFieldNumberExpression extends POExpression
{
	private static final long serialVersionUID = 1L;
	public final POExpression tuple;
	public final LexIntegerToken field;
	public final TCType type;

	public POFieldNumberExpression(POExpression tuple, LexIntegerToken field, TCType type)
	{
		super(tuple);
		this.tuple = tuple;
		this.field = field;
		this.type = type;
	}

	@Override
	public String toString()
	{
		return "(" + tuple + ".#" + field + ")";
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList list = tuple.getProofObligations(ctxt, pogState, env);

		TCTypeQualifier qualifier = new TCTypeQualifier()
		{
			@Override
			public boolean matches(TCType member)
			{
				if (member.isProduct(location))
				{
					TCProductType pt = member.getProduct();
					return (pt.types.size() >= field.value);
				}
				else
				{
					return false;
				}
			}
		};

		list.addAll(checkUnionQualifiers(tuple, qualifier, ctxt));
		return list;
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseFieldNumberExpression(this, arg);
	}
}
