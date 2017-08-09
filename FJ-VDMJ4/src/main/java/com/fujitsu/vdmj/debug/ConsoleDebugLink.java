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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.debug;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fujitsu.vdmj.dbgp.DBGPReason;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.Tracepoint;
import com.fujitsu.vdmj.scheduler.SchedulableThread;
import com.fujitsu.vdmj.scheduler.Signal;
import com.fujitsu.vdmj.values.CPUValue;
import com.fujitsu.vdmj.values.OperationValue;

/**
 * Link class to allow multiple stopped threads to link to a debugging interface.
 */
public class ConsoleDebugLink extends DebugLink
{
	/** True if we are attached to a debugger */
	private static boolean debugging = false;

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

	/** True, if we're suspending breakpoints in an evaluation */
	private boolean suspendBreaks = false;
	
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
	
	public ConsoleDebugLink()
	{
		// Private constructor for singleton.
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

	/**
	 * Called by a thread which has stopped, but not at a breakpoint. For example,
	 * when an exception occurs or deadlock is detected, or when a waiting thread
	 * is pushed into the debugger with a suspendOthers call.
	 */
	@Override
	public void stopped(Context ctxt, LexLocation location)
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
		
		if (ctxt != null)
		{
			synchronized (guardops)
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
			location = new LexLocation();
		}
		
		if (ctxt == null)		// Stopped before it started!
		{
			ctxt = new Context(location, "Empty Context", null);
			ctxt.setThreadState(CPUValue.vCPU);
		}
		
		DebugExecutor dc = new DebugExecutor(location, ctxt);
		
		while (true)
		{
			try
			{
				DebugCommand request = readCommand(thread);
				DebugCommand response = null;
				
				switch (request.getType())
				{
					case RESUME:
						synchronized (this) // So everyone resumes when "resumeThreads" method ends
						{
							return;
						}

					case TERMINATE:
						thread.setSignal(Signal.TERMINATE);
						return;
						
					case PRINT:
						suspendBreaks = true;
						response = dc.run(request);
						writeCommand(thread, response);
						suspendBreaks = false;
						break;

					default:
						response = dc.run(request);
						writeCommand(thread, response);
				}
			}
			catch (InterruptedException e)
			{
				return;		// Being killed?
			}
		}
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
			stopped(ctxt, bp.location);
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

	@Override
	public void complete(DBGPReason reason, ContextException exception)
	{
		// Not used by console debugger
	}
}
