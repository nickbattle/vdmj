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

import java.math.BigInteger;

import com.fujitsu.vdmj.in.annotations.INLoopAnnotations;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.statements.visitors.INStatementVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.values.IntegerValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.VoidValue;

public class INForIndexStatement extends INStatement
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken var;
	public final INExpression from;
	public final INExpression to;
	public final INExpression by;
	public final INStatement statement;
	public final INLoopAnnotations invariants;

	public INForIndexStatement(LexLocation location,
		TCNameToken var, INExpression from, INExpression to, INExpression by,
		INStatement body, INLoopAnnotations invariants)
	{
		super(location);
		this.var = var;
		this.from = from;
		this.to = to;
		this.by = by;
		this.statement = body;
		this.invariants = invariants;
	}

	@Override
	public String toString()
	{
		return "for " + var + " = " + from + " to " + to +
					(by == null ? "" : " by " + by) + "\n" + statement;
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		try
		{
			BigInteger fval = from.eval(ctxt).intValue(ctxt);
			BigInteger tval = to.eval(ctxt).intValue(ctxt);
			BigInteger bval = (by == null) ? BigInteger.ONE : by.eval(ctxt).intValue(ctxt);

			if (bval.equals(BigInteger.ZERO))
			{
				abort(4038, "Loop, from " + fval + " to " + tval + " by " + bval +
						" will never terminate", ctxt);
			}

			invariants.before(ctxt);
			invariants.check(ctxt);

			for (BigInteger value = fval;
				 (bval.compareTo(BigInteger.ZERO) > 0 && value.compareTo(tval) <= 0) ||
				 (bval.compareTo(BigInteger.ZERO) < 0 && value.compareTo(tval) >= 0);
				 value = value.add(bval))
			{
				Context evalContext = new Context(location, "for index", ctxt);
				evalContext.put(var, new IntegerValue(value));

				invariants.check(evalContext);
				Value rv = statement.eval(evalContext);
				invariants.check(ctxt);

				if (!rv.isVoid())
				{
					return rv;
				}
			}
		}
		catch (ValueException e)
		{
			abort(e);
		}
		finally
		{
			invariants.after(ctxt);
		}

		return new VoidValue();
	}

	@Override
	public <R, S> R apply(INStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseForIndexStatement(this, arg);
	}
}
