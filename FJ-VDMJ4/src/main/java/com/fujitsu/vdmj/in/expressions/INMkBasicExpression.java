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

import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.types.TCTokenType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.TokenValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INMkBasicExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final TCType type;
	public final INExpression arg;

	public INMkBasicExpression(TCType type, INExpression arg)
	{
		super(type.location);
		this.type = type;
		this.arg = arg;
	}

	@Override
	public String toString()
	{
		return "mk_" + type + "(" + arg + ")";
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		Value v = arg.eval(ctxt);

		if (type instanceof TCTokenType)
		{
			return new TokenValue(v);
		}
		else
		{
			try
			{
				v = v.convertTo(type, ctxt);
			}
			catch (ValueException e)
			{
				abort(4022, "mk_ type argument is not " + type, ctxt);
			}
		}

		return v;
	}

	@Override
	public INExpression findExpression(int lineno)
	{
		INExpression found = super.findExpression(lineno);
		if (found != null) return found;

		return arg.findExpression(lineno);
	}

	@Override
	public ValueList getValues(Context ctxt)
	{
		return arg.getValues(ctxt);
	}

	@Override
	public TCNameList getOldNames()
	{
		return arg.getOldNames();
	}
}
