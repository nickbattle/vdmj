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

import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.MapValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueMap;

public class INMapUnionExpression extends INBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public INMapUnionExpression(INExpression left, LexToken op, INExpression right)
	{
		super(left, op, right);
	}

	@Override
	public Value eval(Context ctxt)
	{
		// breakpoint.check(location, ctxt);
		location.hit();		// Mark as covered

		ValueMap lm = null;
		ValueMap rm = null;

		try
		{
			lm = left.eval(ctxt).mapValue(ctxt);
			rm = right.eval(ctxt).mapValue(ctxt);
		}
		catch (ValueException e)
		{
			return abort(e);
		}

		ValueMap result = new ValueMap();
		result.putAll(lm);

		for (Value k: rm.keySet())
		{
			Value rng = rm.get(k);
			Value old = result.put(k, rng);

			if (old != null && !old.equals(rng))
			{
				abort(4021, "Duplicate map keys have different values: " + k, ctxt);
			}
		}

		return new MapValue(result);
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMapUnionExpression(this, arg);
	}
}
