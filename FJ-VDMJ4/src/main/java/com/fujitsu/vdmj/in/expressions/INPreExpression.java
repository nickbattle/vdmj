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

import java.util.Iterator;

import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.CompFunctionValue;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.IterFunctionValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INPreExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final INExpression function;
	public final INExpressionList args;

	public INPreExpression(LexLocation location,
		INExpression function, INExpressionList args)
	{
		super(location);
		this.function = function;
		this.args = args;
	}

	@Override
	public String toString()
	{
		return "pre_(" + function + (args.isEmpty() ? "" : ", " + Utils.listToString(args)) + ")";
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		Value fv = function.eval(ctxt);

		if (fv instanceof FunctionValue)
		{
			FunctionValue tfv = (FunctionValue)fv;

			while (true)
			{
    			if (tfv instanceof CompFunctionValue)
    			{
    				tfv = ((CompFunctionValue)tfv).ff1;
    				continue;
    			}

    			if (tfv instanceof IterFunctionValue)
    			{
    				tfv = ((IterFunctionValue)tfv).function;
    				continue;
    			}

    			break;
			}

			FunctionValue pref = tfv.precondition;

			if (pref == null)
			{
				return new BooleanValue(true);
			}

			if (pref.type.parameters.size() <= args.size())
			{
				try
				{
    				ValueList argvals = new ValueList();
    				Iterator<INExpression> aiter = args.iterator();

    				for (@SuppressWarnings("unused") TCType t: pref.type.parameters)
    				{
    					argvals.add(aiter.next().eval(ctxt));
    				}

					return pref.eval(location, argvals, ctxt);
				}
				catch (ValueException e)
				{
					abort(e);
				}
			}

			// else true, below.
		}

		return new BooleanValue(true);
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.casePreExpression(this, arg);
	}
}
