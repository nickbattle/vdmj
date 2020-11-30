/*******************************************************************************
 *
 *	Copyright (c) 2008, 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.in.statements;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.statements.visitors.INStatementVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.scheduler.SchedulableThread;
import com.fujitsu.vdmj.values.Value;

public class INCyclesStatement extends INStatement
{
	private static final long serialVersionUID = 1L;
	public final INExpression cycles;
	public final INStatement statement;

	public INCyclesStatement(LexLocation location, INExpression cycles, INStatement stmt)
	{
		super(location);
		this.cycles = cycles;
		this.statement = stmt;
	}

	@Override
	public String toString()
	{
		return "cycles (" + cycles + ") " + statement;
	}

	@Override
	public Value eval(Context ctxt)
	{
		location.hit();
		cycles.location.hit();

		SchedulableThread me = (SchedulableThread)Thread.currentThread();

		if (me.inOuterTimestep())
		{
			// Already in a timed step, so ignore nesting
			return statement.eval(ctxt);
		}
		else
		{
			try
			{
				// We disable the swapping and time (RT) as cycles evaluation should be "free".
				long value;
				
				try
				{
					ctxt.threadState.setAtomic(true);
					value = cycles.eval(ctxt).intValue(ctxt);
				}
				finally
				{
					ctxt.threadState.setAtomic(false);
				}

				long step = ctxt.threadState.CPU.getDuration(value);
				me.inOuterTimestep(true);
				Value rv = statement.eval(ctxt);
				me.inOuterTimestep(false);
				me.duration(step, ctxt, location);
				return rv;
			}
			catch (ValueException e)
			{
				abort(e);
				return null;
			}
		}
	}

	@Override
	public <R, S> R apply(INStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseCyclesStatement(this, arg);
	}
}
