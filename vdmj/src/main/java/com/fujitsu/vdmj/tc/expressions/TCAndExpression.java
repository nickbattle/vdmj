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

package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCAndExpression extends TCBooleanBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public TCAndExpression(TCExpression left, LexToken op, TCExpression right)
	{
		super(left, op, right);
	}
	
	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		ltype = left.typeCheck(env, null, scope, null);
		TCDefinitionList qualified = left.getQualifiedDefs(env);
		Environment qenv = env;
		
		if (!qualified.isEmpty())
		{
			qenv = new FlatEnvironment(qualified, env);
		}

		rtype = right.typeCheck(qenv, null, scope, null);	// RHS qualified, using LHS
		TCType expected = new TCBooleanType(location);

		if (!ltype.isType(expected.getClass(), location))
		{
			report(3065, "Left hand of " + op + " is not " + expected);
		}

		if (!rtype.isType(expected.getClass(), location))
		{
			report(3066, "Right hand of " + op + " is not " + expected);
		}

		return checkConstraint(constraint, expected);
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseAndExpression(this, arg);
	}
}
