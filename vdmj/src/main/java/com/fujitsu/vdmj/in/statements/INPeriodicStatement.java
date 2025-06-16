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
import com.fujitsu.vdmj.in.statements.visitors.INStatementVisitor;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.Value;

public class INPeriodicStatement extends INStatement
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken opname;
	public final INExpressionList args;

	public long[] values = new long[4];

	public INPeriodicStatement(TCNameToken opname, INExpressionList args)
	{
		super(opname.getLocation());
		this.opname = opname;
		this.args = args;
	}

	@Override
	public Value eval(Context ctxt)
	{
		int i = 0;
		
		for (INExpression arg: args)
		{
			Value argval = null;
			
			try
			{
				arg.location.hit();
				argval = arg.eval(ctxt);
				values[i] = argval.intValue(ctxt);

				if (values[i] < 0)
				{
					abort(4157, "Expecting +ive integer in periodic argument " + (i+1) + ", was " + values[i], ctxt);
				}
			}
			catch (ValueException e)
			{
				abort(4157, "Expecting +ive integer in periodic argument " + (i+1) + ", was " + argval, ctxt);
			}

			i++;
		}

		if (values[0] == 0)
		{
			abort(4158, "Period argument must be non-zero, was " + values[0], ctxt);
		}

		if (args.size() == 4)
		{
			if (values[2] >= values[0])
			{
				abort(4159, "Delay argument (" + values[2] + ") must be less than the period (" + values[0] + ")", ctxt);
			}
		}
		
		return null;	// Not actually used - see TCStartStatement
	}

	@Override
	public String toString()
	{
		return "periodic(" + Utils.listToString(args) + ")(" + opname + ")";
	}

	@Override
	public <R, S> R apply(INStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.casePeriodicStatement(this, arg);
	}
}
