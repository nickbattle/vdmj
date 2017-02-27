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

/**
 * Link class to allow multiple stopped threads to link to a debugging interface.
 */
public class DebugLink
{
	/** An ACK string */
	private final static String ACK = "ACK";
	
	/** The singleton instance */
	private static DebugLink instance = null;
	
	/** The threads that are currently stopped */
	private List<SchedulableThread> stopped = new LinkedList<SchedulableThread>();
	
	/** The threads breakpoint, if any */
	private Map<SchedulableThread, Breakpoint> breakpoints = new HashMap<SchedulableThread, Breakpoint>();
	
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
		while (stopped.isEmpty())
		{
			try
			{
				wait();
			}
			catch (InterruptedException e)
			{
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
	 * Called by a thread which has stopped, but not at a breakpoint. For example,
	 * when an exception occurs or deadlock is detected, or when a waiting thread
	 * is pushed into the debugger with a suspendOthers call.
	 * @param ctxt 
	 */
	public void stopped(Context ctxt, LexLocation location)
	{
		SchedulableThread thread = (SchedulableThread)Thread.currentThread();
		System.out.printf("%s: stopped entered\n", thread);
		
		synchronized(stopped)
		{
			stopped.add(thread);
		}

		synchronized(this)
		{
			this.notify();	// See waitForStop()
		}
		
		Breakpoint bp = breakpoints.get(thread);
		
		if (bp == null)
		{
			bp = new Breakpoint(location);
		}
		
		DebugCommand dc = new DebugCommand(bp, ctxt);
		
		while (true)
		{
			try
			{
				String request = thread.debugExch.exchange(ACK);
				System.out.printf("%s: stopped got %s\n", thread, request);
				String response = dc.run(request);
				System.out.printf("%s: stopped returning %s\n", thread, response);
				thread.debugExch.exchange(response);
			}
			catch (InterruptedException e)
			{
				System.out.printf("%s: stopped interrupted\n", thread);
			}
		}
	}
	
	/**
	 * Called by a thread which has stopped at a breakpoint.
	 */
	public void breakpoint(Context ctxt, Breakpoint bp)
	{
		SchedulableThread thread = (SchedulableThread)Thread.currentThread();
		
		synchronized(breakpoints)
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
			System.out.printf("%s: command sending %s to %s\n", Thread.currentThread(), cmd, thread);
			thread.debugExch.exchange(cmd);
			System.out.printf("%s: command completed %s from %s\n", Thread.currentThread(), cmd, thread);
			return thread.debugExch.exchange(ACK);
		}
		catch (InterruptedException e)
		{
			System.out.printf("%s: command interrupted\n", Thread.currentThread());
			return null;
		}
	}
	
	/**
	 * Resume all threads, with the breakpoint thread last.
	 */
	public void resume()
	{
		
	}
}
