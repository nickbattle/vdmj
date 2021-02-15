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

package com.fujitsu.vdmj.in.expressions;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.NumericValue;
import com.fujitsu.vdmj.values.Value;

public class INDivideExpression extends INNumericBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public INDivideExpression(INExpression left, LexToken op, INExpression right)
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
				BigInteger lv = l.intValue(ctxt);
				BigInteger rv = r.intValue(ctxt);
				BigInteger[] result = lv.divideAndRemainder(rv);
				
				if (result[1].equals(BigInteger.ZERO))	// Else do BigDecimal
				{
					return NumericValue.valueOf(result[0], ctxt);
				}
			}

			BigDecimal lv = l.realValue(ctxt);
    		BigDecimal rv = r.realValue(ctxt);

    		return NumericValue.valueOf(lv.divide(rv, Settings.precision), ctxt);
        }
        catch (ValueException e)
        {
        	return abort(e);
        }
		catch (Exception e)
		{
			return abort(new ValueException(4134, e.getMessage(), ctxt));
		}
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseDivideExpression(this, arg);
	}
}
