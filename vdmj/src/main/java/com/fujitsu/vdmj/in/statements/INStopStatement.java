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

import java.util.List;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.statements.visitors.INStatementVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.scheduler.ObjectThread;
import com.fujitsu.vdmj.scheduler.PeriodicThread;
import com.fujitsu.vdmj.scheduler.SchedulableThread;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueSet;
import com.fujitsu.vdmj.values.VoidValue;

public class INStopStatement extends INStatement
{
	private static final long serialVersionUID = 1L;
	public final INExpression objects;

	public INStopStatement(LexLocation location, INExpression obj)
	{
		super(location);
		this.objects = obj;
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		try
		{
			Value value = objects.eval(ctxt);

			if (value.isType(SetValue.class))
			{
				ValueSet set = value.setValue(ctxt);

				for (Value v: set)
				{
					ObjectValue target = v.objectValue(ctxt);
					stop(target, ctxt);
				}
			}
			else
			{
				ObjectValue target = value.objectValue(ctxt);
				stop(target, ctxt);
			}

			// Cause a reschedule so that this thread is stopped, if necessary
			SchedulableThread th = (SchedulableThread) Thread.currentThread();
			th.reschedule(ctxt, location);
			
			return new VoidValue();
		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}

	private void stop(ObjectValue target, Context ctxt) throws ValueException
	{
		List<SchedulableThread> threads = SchedulableThread.findThreads(target);
		int count = 0;
		
		if (target.getCPU() != ctxt.threadState.CPU)
		{
			abort(4161,
					"Cannot stop object " + target.objectReference +
					" on CPU " + target.getCPU().getName() +
					" from CPU " + ctxt.threadState.CPU, ctxt);
		}
		
		for (SchedulableThread th: threads)
		{
			if (th instanceof ObjectThread || th instanceof PeriodicThread)
			{
				if (th.stopThread())	// This may stop current thread at next reschedule
				{
					count++;
				}
			}
		}

		if (count == 0)
		{
			abort(4160,
				"Object #" + target.objectReference + " is not running a thread to stop", ctxt);
		}
	}

	@Override
	public String toString()
	{
		return "stop(" + objects + ")";
	}

	@Override
	public <R, S> R apply(INStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseStopStatement(this, arg);
	}
}
