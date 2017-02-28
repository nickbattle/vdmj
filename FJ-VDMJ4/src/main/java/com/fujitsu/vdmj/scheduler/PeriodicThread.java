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

package com.fujitsu.vdmj.scheduler;

import java.lang.reflect.InvocationTargetException;
import java.util.Random;

import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.debug.DebugLink;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ObjectContext;
import com.fujitsu.vdmj.runtime.RootContext;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.OperationValue;
import com.fujitsu.vdmj.values.TransactionValue;
import com.fujitsu.vdmj.values.ValueList;

public class PeriodicThread extends SchedulableThread
{
	private static final long serialVersionUID = 1L;
	private final OperationValue operation;
	private final long period;
	private final long jitter;
	private final long delay;
	private final long offset;
	private final long expected;
	private final boolean sporadic;

	private final boolean first;
	private final static Random PRNG = new Random();

	public PeriodicThread(
		ObjectValue self, OperationValue operation,
		long period, long jitter, long delay, long offset, long expected, boolean sporadic)
	{
		super(self.getCPU().resource, self, operation.getPriority(), true, expected);

		setName("Periodic-" + object.type.name.getName() + "-" + object.objectReference);

		this.operation = operation;
		this.period = period;
		this.jitter = jitter;
		this.delay = delay;
		this.offset = offset;
		this.sporadic = sporadic;

		if (expected == 0)
		{
			this.first = true;
			this.expected = SystemClock.getWallTime();
		}
		else
		{
			this.first = false;
			this.expected = expected;
		}
	}

	@Override
	public void start()
	{
		super.start();

		// Here we put the thread into ALARM state (rather than RUNNABLE) and
		// set the time at which we want to be runnable to the expected start,
		// which may have an offset.

		long wakeUpTime = expected;

		if (first)
		{
			if (sporadic)
			{
    			long noise = (jitter == 0) ? 0 : Math.abs(PRNG.nextLong() % jitter);
    			wakeUpTime = offset + noise;
			}
			else
			{
    			if (offset > 0 || jitter > 0)
    			{
        			long noise = (jitter == 0) ? 0 :
        				Math.abs(PRNG.nextLong() % (jitter + 1));

        			wakeUpTime = offset + noise;
    			}
			}
		}

		alarming(wakeUpTime);
	}

	@Override
	protected void body()
	{
		RootContext global = ClassInterpreter.getInstance().getInitialContext();
		LexLocation from = object.type.classdef.location;
		Context ctxt = new ObjectContext(from, "async", global, object);

		new PeriodicThread(
			getObject(), operation, period, jitter, delay, 0,
			nextTime(), sporadic).start();

//		if (Settings.usingDBGP)
//		{
//			runDBGP(ctxt);
//		}
//		else
		{
			runCmd(ctxt);
		}
	}

	private void runCmd(Context ctxt)
	{
		try
		{
			int overlaps = object.incPeriodicCount();

			if (Properties.rt_max_periodic_overlaps > 0 &&
				overlaps >= Properties.rt_max_periodic_overlaps)
			{
				abort(68, "Periodic threads overlapping", ctxt, operation.name.getLocation());
			}

    		ctxt.setThreadState(object.getCPU());

    		operation.localEval(
    			operation.name.getLocation(), new ValueList(), ctxt, true);

    		object.decPeriodicCount();
		}
		catch (ValueException e)
		{
			suspendOthers();
			ResourceScheduler.setException(e);
			DebugLink.getInstance().stopped(e.ctxt, e.ctxt.location);
		}
		catch (ContextException e)
		{
			suspendOthers();
			ResourceScheduler.setException(e);
			DebugLink.getInstance().stopped(e.ctxt, e.location);
		}
		catch (Exception e)
		{
			while (e instanceof InvocationTargetException)
			{
				e = (Exception)e.getCause();
			}
			
			ResourceScheduler.setException(e);
			SchedulableThread.signalAll(Signal.SUSPEND);
		}
		finally
		{
			TransactionValue.commitAll();
		}
	}

	private long nextTime()
	{
		if (sporadic)
		{
			long noise = (jitter == 0) ? 0 : Math.abs(PRNG.nextLong() % jitter);
			return SystemClock.getWallTime() + delay + noise;
		}
		else
		{
			// "expected" was last run time, the next is one "period" away, but this
			// is influenced by jitter as long as it's at least "delay" since
			// "expected".
	
			long noise = (jitter == 0) ? 0 : PRNG.nextLong() % (jitter + 1);
			long next = SystemClock.getWallTime() + period + noise;
	
			if (delay > 0 && next - expected < delay)	// Too close?
			{
				next = expected + delay;
			}
	
			return next;
		}
	}

	public static void reset()
	{
		PRNG.setSeed(123);
	}

	@Override
	public boolean isActive()
	{
		// The initial ALARM wait does not count as a deadlock wait

		return state == RunState.TIMESTEP || state == RunState.WAITING;
	}
}
