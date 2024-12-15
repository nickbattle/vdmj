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
import com.fujitsu.vdmj.pog.NonZeroObligation;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class PODivExpression extends PONumericBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public PODivExpression(POExpression left, LexToken op, POExpression right,
		TCType ltype, TCType rtype)
	{
		super(left, op, right, ltype, rtype);
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = super.getProofObligations(ctxt, pogState, env);
		
		TCType rtype = right.getExptype();

		// A simple problem here is that constant values, like "X:nat = 123" are defined as nat
		// even though they are nat1. We don't want POs for these, so we try to discover the
		// actual type, via the Environment.

		if (right instanceof POVariableExpression)
		{
			POVariableExpression vexp = (POVariableExpression)right;
			TCDefinition def = env.findName(vexp.name, NameScope.NAMESANDSTATE);
			
			if (def instanceof TCLocalDefinition)
			{
				TCLocalDefinition ldef = (TCLocalDefinition)def;
				
				if (ldef.valueDefinition != null)
				{
					rtype = ldef.valueDefinition.getExpType();
				}
			}
		}

		if (!rtype.isAlways(TCNaturalOneType.class, location))
		{
			obligations.add(new NonZeroObligation(location, right, ctxt));
		}
		
		return obligations;
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseDivExpression(this, arg);
	}
}
