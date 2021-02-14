/*******************************************************************************
 *
 *	Copyright (c) 2017 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.debug;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.StateContext;
import com.fujitsu.vdmj.runtime.Tracepoint;
import com.fujitsu.vdmj.scheduler.SchedulableThread;
import com.fujitsu.vdmj.scheduler.Signal;
import com.fujitsu.vdmj.values.CPUValue;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.OperationValue;

/**
 * Link class to allow multiple stopped threads to link to a debugging interface.
 */
public class ConsoleDebugLink extends DebugLink
{
	/** True if we are attached to a debugger */
	protected static boolean debugging = false;

	/** True, if we're suspending breakpoints in an evaluation */
	protected boolean suspendBreaks = false;
	
	/** Singleton instance */
	private static DebugLink instance;
	
	/** The threads that are currently stopped */
	private List<SchedulableThread> stopped = new LinkedList<SchedulableThread>();
	
	/** The threads breakpoint, if any */
	private Map<SchedulableThread, Breakpoint> breakpoints = new HashMap<SchedulableThread, Breakpoint>();
	
	/** The threads locations, if known */
	private Map<SchedulableThread, LexLocation> locations = new HashMap<SchedulableThread, LexLocation>();
	
	/** The threads waited guard operations, if any */
	private Map<SchedulableThread, OperationValue> guardops = new HashMap<SchedulableThread, OperationValue>();
	
	/** The trace callback, if any */
	private TraceCallback callback = null;

	/**
	 * Get the singleton. 
	 */
	public static DebugLink getInstance()
	{
		if (instance == null)
		{
			instance = new ConsoleDebugLink();
		}
		
		return instance;
	}
	
	protected ConsoleDebugLink()
	{
		return;
	}
	
	@Override
	public DebugExecutor getExecutor(LexLocation location, Context ctxt)
	{
		return new ConsoleDebugExecutor(location, ctxt);
	}

	/**
	 * Wait for at least one thread to stop, so that it can be debugged.
	 */
	public synchronized boolean waitForStop()
	{
		debugging = true;
		
		while (stopped.size() < SchedulableThread.getThreadCount() ||
			   SchedulableThread.getThreadCount() == 0)
		{
			try
			{
				wait();
			}
			catch (InterruptedException e)
			{
				debugging = false;
				return false;
			}
		}
		
		return true;
	}

	/**
	 * Return the current set of stopped threads.
	 */
	public List<SchedulableThread> getThreads()
	{
		return stopped;
	}
	
	/**
	 * Return the breakpoint for a given thread, if any.
	 */
	public Breakpoint getBreakpoint(SchedulableThread thread)
	{
		return breakpoints.get(thread);
	}
	
	/**
	 * Return the location for a given thread, if known.
	 */
	public LexLocation getLocation(SchedulableThread thread)
	{
		return locations.get(thread);
	}
	
	/**
	 * Return the guarded operation for a given thread, if any.
	 */
	public OperationValue getGuardOp(SchedulableThread thread)
	{
		return guardops.get(thread);
	}
	
	/**
	 * Return the current breakpoint - should only ever be one or none.
	 */
	public Breakpoint getBreakpoint()
	{
		switch (breakpoints.size())
		{
			case 0:
				return null;
				
			case 1:
				return breakpoints.values().iterator().next();
				
			default:
				throw new RuntimeException("More than one breakpoint??");
		}
	}
	
	/**
	 * Return the thread with the current breakpoint, or the first thread otherwise.
	 */
	public SchedulableThread getDebugThread()
	{
		Breakpoint bp = getBreakpoint();
		
		if (bp == null)
		{
			return stopped.get(0);	// First stopped thread
		}
		else
		{
			return breakpoints.keySet().iterator().next();
		}
	}
	
	/**
	 * Send a command to one particular thread.
	 */
	public DebugCommand sendCommand(SchedulableThread thread, DebugCommand cmd)
	{
		try
		{
			writeCommand(thread, cmd);
			return readCommand(thread);
		}
		catch (InterruptedException e)
		{
			return DebugCommand.QUIT;
		}
	}

	/**
	 * Resume all threads.
	 */
	public synchronized void resumeThreads()
	{
		for (SchedulableThread thread: stopped)
		{
			try
			{
				writeCommand(thread, DebugCommand.RESUME);
			}
			catch (InterruptedException e)
			{
				// Ignore?
			}
		}
		
		stopped.clear();
		breakpoints.clear();
		locations.clear();
		guardops.clear();
	}

	/**
	 * Kill all threads.
	 */
	public void killThreads()
	{
		for (SchedulableThread thread: stopped)
		{
			try
			{
				writeCommand(thread, DebugCommand.TERMINATE);
			}
			catch (InterruptedException e)
			{
				// Ignore?
			}
		}
		
		stopped.clear();
		breakpoints.clear();
		locations.clear();
		guardops.clear();
		callback = null;
	}

	/**
	 * Set the trace callback.
	 */
	public void setTraceCallback(TraceCallback callback)
	{
		this.callback = callback;
	}

	
	/********************************************************************************
	 * Methods above here are called from the DebugReader thread; methods
	 * below are called from the ScheduledThreads that stop.
	 *******************************************************************************/
	
	/**
	 * Called by a thread when it is created.
	 */
	@Override
	public void newThread(CPUValue cpu)
	{
		return;
	}

	/**
	 * Called by a thread which has stopped, but not at a breakpoint. For example,
	 * when an exception occurs or deadlock is detected, or when a waiting thread
	 * is pushed into the debugger with a suspendOthers call.
	 */
	@Override
	public void stopped(Context ctxt, LexLocation location, Exception ex)
	{
		if (!debugging || suspendBreaks)	// Not attached to a debugger or local eval
		{
			return;
		}
		
		SchedulableThread thread = (SchedulableThread)Thread.currentThread();
		
		synchronized(stopped)
		{
			stopped.add(thread);
		}

		synchronized(locations)
		{
			locations.put(thread, location);
		}
		
		synchronized (guardops)
		{
			if (ctxt != null && ctxt.guardOp != null)
			{
				guardops.put(thread, ctxt.guardOp);
			}
		}
		
		synchronized(this)
		{
			this.notify();		// See waitForStop()
		}
		
		if (location == null)	// Stopped before it started!
		{
			// Create a location from the class type
			SchedulableThread th = (SchedulableThread) Thread.currentThread();
			ObjectValue obj = th.getObject();
			location = obj.type.location;
		}
		
		if (ctxt == null)		// Stopped before it started!
		{
			ctxt = new StateContext(location, "New thread", null, null);
			ctxt.setThreadState(CPUValue.vCPU);
		}
		
		DebugExecutor exec = getExecutor(location, ctxt);
		boolean stopped = true;
		
		while (stopped)
		{
			try
			{
				DebugCommand request = readCommand(thread);
				
				switch (request.getType())
				{
					case RESUME:
						synchronized (this) // So everyone resumes when "resumeThreads" method ends
						{
							stopped = false;
							break;
						}

					case TERMINATE:
						thread.setSignal(Signal.TERMINATE);
						stopped = false;
						break;
						
					case PRINT:
						suspendBreaks = true;
						writeCommand(thread, exec.run(request));
						suspendBreaks = false;
						break;

					default:
						writeCommand(thread, exec.run(request));
				}
			}
			catch (InterruptedException e)
			{
				stopped = false;		// Being killed?
			}
		}
		
		exec.clear();	// Cached frame information no longer needed
		return;
	}
	
	/**
	 * Called by a thread which has stopped at a breakpoint.
	 */
	@Override
	public void breakpoint(Context ctxt, Breakpoint bp)
	{
		if (debugging && !suspendBreaks)
		{
			SchedulableThread thread = (SchedulableThread)Thread.currentThread();
			breakpoints.put(thread, bp);
			stopped(ctxt, bp.location, null);
			
			if (thread.getSignal() == Signal.TERMINATE)
			{
				throw new ThreadDeath();	// Just die, as we're not continuing.
			}
		}
	}
	
	/**
	 * Called by a thread which has hit a tracepoint.
	 */
	@Override
	public void tracepoint(Context ctxt, Tracepoint tp)
	{
		if (callback != null)
		{
			callback.tracepoint(ctxt, tp);
		}
	}

	/**
	 * Called by a thread which is terminating, possibly with an exception.
	 */
	@Override
	public void complete(DebugReason reason, ContextException exception)
	{
		// Not used by console debugger
	}
}
