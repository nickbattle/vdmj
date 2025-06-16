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

package com.fujitsu.vdmj.in.expressions;

import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.in.patterns.INPatternList;
import com.fujitsu.vdmj.in.types.INInstantiate;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.Value;

public class INLambdaExpression extends INExpression
{
	private static final long serialVersionUID = 1L;

	public final TCFunctionType type;
	public final INPatternList paramPatterns;
	public final INExpression expression;

	public INLambdaExpression(LexLocation location, TCFunctionType type, INPatternList paramPatterns, INExpression expression)
	{
		super(location);
		this.type = type;
		this.paramPatterns = paramPatterns;
		this.expression = expression;
	}

	@Override
	public String toString()
	{
		return "(lambda " + paramPatterns + " & " + expression + ")";
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		// Free variables are everything currently visible from this
		// context (but without the context chain).

		Context free = ctxt.getVisibleVariables();
		TCFunctionType ftype = (TCFunctionType) INInstantiate.instantiate(type, ctxt, ctxt);

		return new FunctionValue(location, "lambda", ftype, paramPatterns, expression, free);
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseLambdaExpression(this, arg);
	}
}
