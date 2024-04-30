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

package com.fujitsu.vdmj.scheduler;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Exchanger;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.debug.DebugCommand;
import com.fujitsu.vdmj.debug.DebugLink;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.messages.RTLogger;
import com.fujitsu.vdmj.messages.VDMThreadDeath;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ExceptionHandler;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.ObjectValue;

public abstract class SchedulableThread extends Thread implements Serializable, Comparable<SchedulableThread>
{
    private static final long serialVersionUID = 1L;

	private static List<SchedulableThread> allThreads =	new LinkedList<SchedulableThread>();
	private static int threadCount = 0;		// Non-bus threads

	protected final Resource resource;
	protected final ObjectValue object;
	private final boolean periodic;
	private final boolean virtual;

	protected RunState state;
	protected Signal signal;
	private long timeslice;
	private long steps;
	private long timestep;
	private long durationEnd;
	private long alarmWakeTime;
	private long swapInBy;
	private boolean inOuterTimeStep;
	protected boolean stopCalled;
	
	public Exchanger<DebugCommand> debugExch = new Exchanger<DebugCommand>();

	public SchedulableThread(
		Resource resource, ObjectValue object, long priority,
		boolean periodic, long swapInBy)
	{
		this.resource = resource;
		this.object = object;
		this.periodic = periodic;
		this.virtual = resource.isVirtual();
		this.setSwapInBy(swapInBy);

		state = RunState.CREATED;
		signal = null;
		timeslice = 0;
		steps = 0;
		timestep = Long.MAX_VALUE;
		alarmWakeTime = Long.MAX_VALUE;
		durationEnd = 0;
		inOuterTimeStep = false;
		stopCalled = false;

		resource.register(this, priority);

		synchronized (allThreads)
		{
			allThreads.add(this);
			
			if (!(this instanceof BusThread))
			{
				threadCount++;
			}
		}
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof SchedulableThread)
		{
			SchedulableThread to = (SchedulableThread)other;
			return getId() == to.getId();
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return Long.valueOf(getId()).hashCode();
	}

	@Override
	public String toString()
	{
		return getName() + " (" + (stopCalled ? "STOPPING" : state) + ")";
	}

    @Override
	public synchronized void start()
	{
		super.start();

		while (state == RunState.CREATED)
		{
			sleep(null, null);
		}

		// Log the creation here so that it is deterministic...

		if (resource instanceof CPUResource)
		{
			CPUResource cpu = (CPUResource)resource;
			cpu.createThread(this);
		}
	}

	@Override
	public void run()
	{
		try
		{
			reschedule(null, null);
			body();
		}
		catch (VDMThreadDeath e)
		{
			// Exit cleanly
		}
		finally
		{
			setState(RunState.COMPLETE);
			resource.unregister(this);

			synchronized (allThreads)
			{
				allThreads.remove(this);

				if (!(this instanceof BusThread))
				{
					threadCount--;
				}
			}
		}
	}

	abstract protected void body();

	public void step(Context ctxt, LexLocation location)
	{
		if (Settings.dialect == Dialect.VDM_RT)
		{
			if (!virtual)
			{
				duration(getObject().getCPU().getDuration(Properties.rt_duration_default), ctxt, location);
			}
		}
		else
		{
			SystemClock.advance(Properties.rt_duration_default);
		}

		// Note that we don't reschedule if we are in an outer cycles/duration

		if (++steps >= timeslice && !inOuterTimeStep)
		{
			reschedule(ctxt, location);
			steps = 0;
		}
	}

	public synchronized RunState getRunState()
	{
		return state;
	}

	public RunState getUnsafeRunState()
	{
		return state;
	}

	public synchronized void setState(RunState newstate)
	{
		state = newstate;
		notifyAll();
	}

	public synchronized void reschedule(Context ctxt, LexLocation location)
	{
		// Yield control but remain runnable - called by thread
		waitUntilState(RunState.RUNNABLE, RunState.RUNNING, ctxt, location);
	}

	public synchronized void waiting(Context ctxt, LexLocation location)
	{
		// Enter a waiting state - called by thread
		waitUntilState(RunState.WAITING, RunState.RUNNING, ctxt, location);
	}

	public synchronized void locking(Context ctxt, LexLocation location)
	{
		// Enter a locking state - called by thread
		waitUntilState(RunState.LOCKING, RunState.RUNNING, ctxt, location);
	}

	public synchronized void alarming(long expected)
	{
		// Enter an alarming state - called by another thread, does not block.
		alarmWakeTime = expected;
		setState(RunState.ALARM);
	}

	public synchronized void runslice(long slice)
	{
		// Run one time slice - called by Scheduler
		timeslice = slice;
		waitWhileState(RunState.RUNNING, RunState.RUNNING, null, null);
	}

	public synchronized void duration(long pause, Context ctxt, LexLocation location)
	{
		// Wait until pause has passed - called by thread

		if (!inOuterTimeStep)
		{
    		setTimestep(pause);
    		durationEnd = SystemClock.getWallTime() + pause;

    		do
    		{
        		if (Properties.rt_diags_timestep)
        		{
        			RTLogger.log(String.format("-- %s Waiting to move time by %d",
        				this, timestep));
        		}

        		waitUntilState(RunState.TIMESTEP, RunState.RUNNING, ctxt, location);
    			setTimestep(durationEnd - SystemClock.getWallTime());
    		}
    		while (getTimestep() > 0);

    		setTimestep(Long.MAX_VALUE);	// Finished
		}
	}

	private synchronized void waitWhileState(
		RunState newstate, RunState whilestate, Context ctxt, LexLocation location)
	{
		setState(newstate);

		while (state == whilestate)
		{
			sleep(ctxt, location);
		}
	}

	private synchronized void waitUntilState(
		RunState newstate, RunState until, Context ctxt, LexLocation location)
	{
		setState(newstate);

		while (state != until)
		{
			sleep(ctxt, location);
		}
	}

	private  void sleep(Context ctxt, LexLocation location)
	{
		while (true)
		{
    		try
    		{
    			/**
    			 * Only synchronize on the wait, and handle any interrupted signal without
    			 * holding the thread lock. This avoids lock problems in the debugger.
    			 */
    			synchronized (this)
    			{
    				wait();

    				if (stopCalled && state == RunState.RUNNING)
    				{
    					// stopThread made us RUNNABLE, now we're running, so die
    					throw new VDMThreadDeath();
    				}

    				return;
    			}
    		}
    		catch (InterruptedException e)
    		{
   				while (signal != null)		// Can be set by handler
   				{
   					handleSignal(signal, ctxt, location);
   				}
    		}
		}
	}

	protected void handleSignal(Signal sig, Context ctxt, LexLocation location)
	{
		signal = null;

		switch (sig)
		{
			case TERMINATE:
				throw new VDMThreadDeath();

			case SUSPEND:
				DebugLink.getInstance().stopped(ctxt, location, null);
				break;
				
			case DEADLOCKED:
				DebugLink.getInstance().stopped(ctxt, location, new Exception("DEADLOCK detected"));
				throw new VDMThreadDeath();
		}
	}

	public void suspendOthers()
	{
		synchronized (allThreads)
		{
    		for (SchedulableThread th: allThreads)
    		{
    			if (!th.equals(this))
    			{
    				th.setSignal(Signal.SUSPEND);
    			}
    		}
		}
	}

	public static void terminateAll()
	{
		synchronized (allThreads)
		{
    		for (SchedulableThread th: allThreads)
    		{
   				th.setSignal(Signal.TERMINATE);
    		}
		}
	}

	public static void signalAll(Signal sig)
	{
		synchronized (allThreads)
		{
    		for (SchedulableThread th: allThreads)
    		{
   				th.setSignal(sig);
    		}
		}
	}
	
	public synchronized boolean stopThread()
	{
		if (!stopCalled)
		{
			stopCalled = true;
			timestep = Long.MAX_VALUE;			// Don't take part in time step
			
			if (Thread.currentThread() != this)
			{
				setState(RunState.RUNNABLE);	// So that thread is rescheduled
			}
			
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public static List<SchedulableThread> findThreads(ObjectValue target)
	{
		synchronized (allThreads)
		{
			List<SchedulableThread> list = new Vector<SchedulableThread>();
			
    		for (SchedulableThread th: allThreads)
    		{
   				if (th.object == target)
   				{
   					list.add(th);
   				}
    		}
    		
    		return list;
		}
	}
	
	public static MainThread getMainThread()
	{
		synchronized (allThreads)
		{
    		for (SchedulableThread th: allThreads)
    		{
   				if (th instanceof MainThread)
   				{
   					return (MainThread)th;
   				}
    		}
    		
    		return null;
		}
	}

	public static List<SchedulableThread> getAllThreads()
	{
		return allThreads;
	}
	
	public static int getThreadCount()
	{
		return threadCount;		// Number of non-bus threads
	}

	public synchronized void setSignal(Signal sig)
	{
		signal = sig;
		interrupt();
	}
	
	public Signal getSignal()
	{
		return signal;
	}

	public ObjectValue getObject()
	{
		return object;
	}

	public synchronized void setSwapInBy(long swapInBy)
	{
		this.swapInBy = swapInBy;
	}

	public synchronized long getSwapInBy()
	{
		return swapInBy;
	}

	public boolean isPeriodic()
	{
		return periodic;
	}

	public boolean isActive()
	{
		return state == RunState.TIMESTEP || state == RunState.WAITING;
	}

	public boolean isVirtualResource()
	{
		return virtual;
	}

	public synchronized void setTimestep(long step)
	{
		timestep = step;
	}

	public synchronized long getTimestep()
	{
		return timestep;
	}

	public synchronized long getDurationEnd()
	{
		return durationEnd;
	}

	public synchronized long getAlarmWakeTime()
	{
		return alarmWakeTime;
	}

	public void clearAlarm()
	{
		alarmWakeTime = Long.MAX_VALUE;
	}

	public synchronized void inOuterTimestep(boolean b)
	{
		inOuterTimeStep = b;
	}

	public synchronized boolean inOuterTimestep()
	{
		return inOuterTimeStep;
	}

	public CPUResource getCPUResource()
	{
		if (resource instanceof CPUResource)
		{
			return (CPUResource)resource;
		}
		else
		{
			throw new InternalException(66, "Thread is not running on a CPU");
		}
	}
	
	public void abort(int number, String msg, Context ctxt, LexLocation location)
	{
		ExceptionHandler.handle(new ContextException(number, msg, location, ctxt));
	}

	public void abort(ValueException ve, LexLocation location)
	{
		ExceptionHandler.handle(new ContextException(ve, location));
	}
	
	@Override
	public int compareTo(SchedulableThread other)
	{
		return getName().compareTo(other.getName());
	}
}
