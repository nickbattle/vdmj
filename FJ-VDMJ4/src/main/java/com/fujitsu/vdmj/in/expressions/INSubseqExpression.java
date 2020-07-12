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

import java.math.RoundingMode;
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INSubseqExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final INExpression seq;
	public final INExpression from;
	public final INExpression to;

	public INSubseqExpression(INExpression seq, INExpression from, INExpression to)
	{
		super(seq);
		this.seq = seq;
		this.from = from;
		this.to = to;
	}

	@Override
	public String toString()
	{
		return "(" + seq + "(" + from + ", ... ," + to + "))";
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		try
		{
    		ValueList list = seq.eval(ctxt).seqValue(ctxt);
    		int fi = from.eval(ctxt).realValue(ctxt).setScale(0, RoundingMode.CEILING).intValue();
    		int ti = to.eval(ctxt).realValue(ctxt).setScale(0, RoundingMode.FLOOR).intValue();

    		if (fi < 1)
    		{
    			fi = 1;
    		}

    		if (ti > list.size())
    		{
    			ti = list.size();
    		}

    		ValueList result = new ValueList();

    		if (fi <= ti)
    		{
        		result.addAll(list.subList(fi-1, ti));
    		}

    		return new SeqValue(result);
		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}

	@Override
	public TCNameList getOldNames()
	{
		TCNameList list = seq.getOldNames();
		list.addAll(from.getOldNames());
		list.addAll(to.getOldNames());
		return list;
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSubseqExpression(this, arg);
	}
}
