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

import java.math.BigInteger;

import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.NumericValue;
import com.fujitsu.vdmj.values.Value;

public class INDivExpression extends INNumericBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public INDivExpression(INExpression left, LexToken op, INExpression right)
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
    		BigInteger lv = left.eval(ctxt).intValue(ctxt);
    		BigInteger rv = right.eval(ctxt).intValue(ctxt);

    		if (rv.signum() == 0)
    		{
    			throw new ValueException(4134, "Infinite or NaN trouble", ctxt);
    		}

    		return NumericValue.valueOf(lv.divide(rv), ctxt);
        }
        catch (ValueException e)
        {
        	return abort(e);
        }
	}

	static public long div(double lv, double rv)
	{
		/*
		 * There is often confusion on how integer division, remainder and modulus
		 * work on negative numbers. In fact, there are two valid answers to -14 div
		 * 3: either (the intuitive) -4 as in the Toolbox, or -5 as in e.g. Standard
		 * ML [Paulson91]. It is therefore appropriate to explain these operations in
		 * some detail.
		 *
		 * Integer division is defined using floor and real number division:
		 *
		 *		x/y < 0:	x div y = -floor(abs(-x/y))
		 *		x/y >= 0:	x div y = floor(abs(x/y))
		 *
		 * Note that the order of floor and abs on the right-hand side makes a difference,
		 * the above example would yield -5 if we changed the order. This is
		 * because floor always yields a smaller (or equal) integer, e.g. floor (14/3) is
		 * 4 while floor (-14/3) is -5.
		 */

		if (lv/rv < 0)
		{
			return (long)-Math.floor(Math.abs(lv/rv));
		}
		else
		{
			return (long)Math.floor(Math.abs(-lv/rv));
		}
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseDivExpression(this, arg);
	}
}
