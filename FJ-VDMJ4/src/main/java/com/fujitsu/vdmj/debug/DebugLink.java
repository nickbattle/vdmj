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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.scheduler.SchedulableThread;
import com.fujitsu.vdmj.values.CPUValue;

/**
 * Link class to allow multiple stopped threads to link to a debugging interface.
 */
public class DebugLink
{
	/** An ACK string */
	private final static String ACK = "ACK";
	
	/** The singleton instance */
	private static DebugLink instance = null;
	
	/** True if we are attached to a debugger */
	private static boolean debugging = false;
	
	/** The threads that are currently stopped */
	private List<SchedulableThread> stopped = new LinkedList<SchedulableThread>();
	
	/** The threads breakpoint, if any */
	private Map<SchedulableThread, Breakpoint> breakpoints = new HashMap<SchedulableThread, Breakpoint>();
	
	/** The threads locations, if known */
	private Map<SchedulableThread, LexLocation> locations = new HashMap<SchedulableThread, LexLocation>();
	
	/**
	 * Get the singleton.
	 */
	public static DebugLink getInstance()
	{
		if (instance == null)
		{
			instance = new DebugLink();
		}
		
		return instance;
	}
	
	private DebugLink()
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
				System.err.println("More than one breakpoint??");
				return null;
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
	 * Called by a thread which has stopped, but not at a breakpoint. For example,
	 * when an exception occurs or deadlock is detected, or when a waiting thread
	 * is pushed into the debugger with a suspendOthers call.
	 */
	public void stopped(Context ctxt, LexLocation location)
	{
		if (!debugging)		// Not attached to a debugger
		{
			return;
		}
		
		SchedulableThread thread = (SchedulableThread)Thread.currentThread();
		
		synchronized(stopped)
		{
			stopped.add(thread);
		}

		synchronized(this)
		{
			this.notify();		// See waitForStop()
		}
		
		if (location == null)	// Stopped before it started!
		{
			location = new LexLocation();
		}
		
		synchronized(locations)
		{
			locations.put(thread, location);
		}
		
		if (ctxt == null)		// Stopped before it started!
		{
			ctxt = new Context(location, "Empty Context", ctxt);
			ctxt.setThreadState(CPUValue.vCPU);
		}
		
		Breakpoint bp = breakpoints.get(thread);
		
		if (bp == null)			// An interrupted thread, not a break
		{
			bp = new Breakpoint(location);
		}
		
		DebugCommand dc = new DebugCommand(bp, ctxt);
		
		while (true)
		{
			try
			{
				String request = thread.debugExch.exchange(ACK);
				
				if (request.equals("resume"))
				{
					synchronized(this)	// So everyone resumes when "resume" method ends
					{
						return;
					}
				}
				else
				{
					String response = dc.run(request);
					thread.debugExch.exchange(response);
				}
			}
			catch (InterruptedException e)
			{
				// Ignore?
			}
		}
	}
	
	/**
	 * Called by a thread which has stopped at a breakpoint.
	 */
	public void breakpoint(Context ctxt, Breakpoint bp)
	{
		SchedulableThread thread = (SchedulableThread)Thread.currentThread();
		
		synchronized (breakpoints)
		{
			breakpoints.put(thread, bp);
		}

		stopped(ctxt, bp.location);
	}
	
	/**
	 * Send a command to one particular thread.
	 */
	public String command(SchedulableThread thread, String cmd)
	{
		try
		{
			thread.debugExch.exchange(cmd);
			return thread.debugExch.exchange(ACK);
		}
		catch (InterruptedException e)
		{
			return null;
		}
	}
	
	/**
	 * Resume all threads, with the breakpoint thread last.
	 */
	public synchronized void resume()
	{
		for (SchedulableThread thread: stopped)
		{
			try
			{
				thread.debugExch.exchange("resume");
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
}
