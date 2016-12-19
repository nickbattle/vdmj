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

public class INRemExpression extends INNumericBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public INRemExpression(INExpression left, LexToken op, INExpression right)
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
			/*
			 * Remainder x rem y and modulus x mod y are the same if the signs of x
			 * and y are the same, otherwise they differ and rem takes the sign of x and
			 * mod takes the sign of y. The formulas for remainder and modulus are:
			 *
			 *		x rem y = x - y * (x div y)
			 *		x mod y = x - y * floor(x/y)
			 *
			 * Hence, -14 rem 3 equals -2 and -14 mod 3 equals 1. One can view these
			 * results by walking the real axis, starting at -14 and making jumps of 3.
			 * The remainder will be the last negative number one visits, because the first
			 * argument corresponding to x is negative, while the modulus will be the first
			 * positive number one visit, because the second argument corresponding to y
			 * is positive.
			 */

    		BigInteger lv = left.eval(ctxt).intValue(ctxt);
    		BigInteger rv = right.eval(ctxt).intValue(ctxt);

    		if (rv.equals(BigInteger.ZERO))
    		{
    			throw new ValueException(4134, "Infinite or NaN trouble", ctxt);
    		}
    		
    		return NumericValue.valueOf(lv.remainder(rv), ctxt);
		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}
}
