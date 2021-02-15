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
import com.fujitsu.vdmj.values.MapValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueMap;

public class INPlusPlusExpression extends INBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public INPlusPlusExpression(INExpression left, LexToken op, INExpression right)
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
    			ValueMap lm = new ValueMap(lv.mapValue(ctxt));
    			ValueMap rm = rv.mapValue(ctxt);

    			for (Value k: rm.keySet())
    			{
					lm.put(k, rm.get(k));
				}

    			return new MapValue(lm);
    		}
    		else
    		{
    			ValueList seq = lv.seqValue(ctxt);
    			ValueMap map = rv.mapValue(ctxt);
    			ValueList result = new ValueList(seq);

    			for (Value k: map.keySet())
    			{
					int iv = k.intValue(ctxt).intValue();

					if (iv < 1 || iv > seq.size())
					{
						abort(4025, "Map key not within sequence index range: " + k, ctxt);
					}

					result.set(iv-1, map.get(k));
    			}

    			return new SeqValue(result);
    		}
		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.casePlusPlusExpression(this, arg);
	}
}
