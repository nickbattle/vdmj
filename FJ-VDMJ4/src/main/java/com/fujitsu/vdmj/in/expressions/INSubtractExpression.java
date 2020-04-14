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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.expressions;

import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.NumericValue;
import com.fujitsu.vdmj.values.Value;

public class INSubtractExpression extends INNumericBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public INSubtractExpression(INExpression left, LexToken op, INExpression right)
	{
		super(left, op, right);
	}

	@Override
	public Value eval(Context ctxt)
	{
		// breakpoint.check(location, ctxt);
		location.hit();		// Mark as covered

		try
		{
    		Value l = left.eval(ctxt);
    		Value r = right.eval(ctxt);

			if (NumericValue.areIntegers(l, r))
			{
				long lv = l.intValue(ctxt);
				long rv = r.intValue(ctxt);
				long diff = subtractExact(lv, rv, ctxt);
				return NumericValue.valueOf(diff, ctxt);
			}
			else
			{
				double lv = l.realValue(ctxt);
				double rv = r.realValue(ctxt);
	    		return NumericValue.valueOf(lv - rv, ctxt);
			}
		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}
	
	// This is included in Java 8 Math.java
	private long subtractExact(long x, long y, Context ctxt) throws ValueException
	{
		long r = x - y;
		// HD 2-12 Overflow iff the arguments have different signs and
		// the sign of the result is different than the sign of x

		if (((x ^ y) & (x ^ r)) < 0)
		{
			throw new ValueException(4169, "Arithmetic overflow", ctxt);
		}

		return r;
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSubtractExpression(this, arg);
	}
}
