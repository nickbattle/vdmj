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
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.Value;

public class INGreaterEqualExpression extends INNumericBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public INGreaterEqualExpression(INExpression left, LexToken op, INExpression right)
	{
		super(left, op, right);
	}

	@Override
	public Value eval(Context ctxt)
	{
		// breakpoint.check(location, ctxt);
		location.hit();		// Mark as covered

		Value lv = left.eval(ctxt);
		Value rv = right.eval(ctxt);

		if (lv.isOrdered() && rv.isOrdered())
		{
			int cmp = lv.compareTo(rv);
			
			if (cmp != Integer.MIN_VALUE)	// Indicates comparable
			{
				return new BooleanValue(cmp >= 0);
			}
		}

		return abort(4172, "Values cannot be compared: " + lv + ", " + rv, ctxt);
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseGreaterEqualExpression(this, arg);
	}
}
