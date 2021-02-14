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

import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.IterFunctionValue;
import com.fujitsu.vdmj.values.MapValue;
import com.fujitsu.vdmj.values.NumericValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueMap;

public class INStarStarExpression extends INBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public INStarStarExpression(INExpression left, LexToken op, INExpression right)
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
    		Value lv = left.eval(ctxt).deref();
    		Value rv = right.eval(ctxt);

    		if (lv instanceof MapValue)
    		{
    			ValueMap map = lv.mapValue(ctxt);
    			long n = rv.intValue(ctxt);
    			ValueMap result = new ValueMap();

    			for (Value k: map.keySet())
    			{
    				Value r = k;

    				for (int i=0; i<n; i++)
    				{
    					r = map.get(r);
    				}

    				if (r == null)
    				{
						abort(4133, "Map range is not a subset of its domain: " + k, ctxt);
    				}

					Value old = result.put(k, r);

					if (old != null && !old.equals(r))
					{
						abort(4030, "Duplicate map keys have different values: " + k, ctxt);
					}
				}

    			return new MapValue(result);
    		}
    		else if (lv instanceof FunctionValue)
    		{
    			return new IterFunctionValue(
    				lv.functionValue(ctxt), rv.intValue(ctxt));
    		}
    		else if (lv instanceof NumericValue)
    		{
    			double ld = lv.realValue(ctxt);
    			double rd = rv.realValue(ctxt);

    			return NumericValue.valueOf(Math.pow(ld, rd), ctxt);
    		}

    		return abort(4031,
    			"First arg of '**' must be a map, function or number", ctxt);
 		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseStarStarExpression(this, arg);
	}
}
