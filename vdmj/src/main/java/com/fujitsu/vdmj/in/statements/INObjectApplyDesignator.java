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

package com.fujitsu.vdmj.in.statements;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.MapValue;
import com.fujitsu.vdmj.values.OperationValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.UpdatableValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueMap;

public class INObjectApplyDesignator extends INObjectDesignator
{
	private static final long serialVersionUID = 1L;
	public final INObjectDesignator object;
	public final INExpressionList args;

	public INObjectApplyDesignator(INObjectDesignator object, INExpressionList args)
	{
		super(object.location);
		this.object = object;
		this.args = args;
	}

	@Override
	public String toString()
	{
		return "(" + object + "(" + Utils.listToString(args) + "))";
	}

	@Override
	public Value eval(Context ctxt)
	{
		try
		{
			Value uv = object.eval(ctxt);
			Value v = uv.deref();

			if (v instanceof MapValue)
			{
				ValueMap mv = v.mapValue(ctxt);
				Value a = args.get(0).eval(ctxt);
				Value rv = mv.get(a);

				if (rv == null && uv instanceof UpdatableValue)
				{
					// Not already in map - get listener from root object
					UpdatableValue ur = (UpdatableValue)uv;
					rv = UpdatableValue.factory(ur.listeners);
					mv.put(a, rv);
				}

				return rv;
			}
			else if (v instanceof SeqValue)
			{
				ValueList seq = v.seqValue(ctxt);
				Value a = args.get(0).eval(ctxt);
				int i = (int)a.intValue(ctxt).intValue()-1;

				if (!seq.inbounds(i))
				{
					abort(4042, "Sequence does not contain key: " + a, ctxt);
				}

				return seq.get(i);
			}
			else if (v instanceof FunctionValue)
			{
				ValueList argvals = new ValueList();

				for (INExpression arg: args)
				{
					argvals.add(arg.eval(ctxt));
				}

				FunctionValue fv = v.functionValue(ctxt);
				return fv.eval(location, argvals, ctxt);
			}
			else if (v instanceof OperationValue)
			{
				ValueList argvals = new ValueList();

				for (INExpression arg: args)
				{
					argvals.add(arg.eval(ctxt));
				}

				OperationValue ov = v.operationValue(ctxt);
				return ov.eval(location, argvals, ctxt);
			}
			else
			{
				return abort(4043,
					"Object designator is not a map, sequence, operation or function", ctxt);
			}
		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}
}
