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

package com.fujitsu.vdmj.in.statements;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.statements.visitors.INStatementVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.scheduler.SchedulableThread;
import com.fujitsu.vdmj.values.Value;

public class INDurationStatement extends INStatement
{
	private static final long serialVersionUID = 1L;
	public final INExpression duration;
	public final INStatement statement;

	public INDurationStatement(LexLocation location, INExpression duration, INStatement stmt)
	{
		super(location);
		this.duration = duration;
		this.statement = stmt;
	}

	@Override
	public String toString()
	{
		return "duration (" + duration + ") " + statement;
	}

	@Override
	public Value eval(Context ctxt)
	{
		location.hit();
		duration.location.hit();

		assertNotInit(ctxt);
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
				// We disable the swapping and time (RT) as duration evaluation should be "free".
				long step;
				
				try
				{
					ctxt.threadState.setAtomic(true);
					step = duration.eval(ctxt).intValue(ctxt).longValue();
				}
				finally
				{
					ctxt.threadState.setAtomic(false);
				}

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
		return visitor.caseDurationStatement(this, arg);
	}
}
