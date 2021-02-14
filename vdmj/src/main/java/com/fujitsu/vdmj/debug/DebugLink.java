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

import java.lang.reflect.Method;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.Tracepoint;
import com.fujitsu.vdmj.scheduler.SchedulableThread;
import com.fujitsu.vdmj.values.CPUValue;

/**
 * Base class of all link objects that connect the runtime to a debugger. These
 * are typically specific to the debugger type - eg. command line or remote.
 */
abstract public class DebugLink
{
	/**
	 * Get the singleton. Delegates to static methods in the concrete classes.
	 */
	public static DebugLink getInstance()
	{
		String linkClass = System.getProperty("vdmj.debug.link");
		
		if (linkClass != null)
		{
			try
			{
				// Call static getInstance from class identified
				Method getter = Class.forName(linkClass).getDeclaredMethod("getInstance");
				return (DebugLink)getter.invoke(null, (Object[])null);
			}
			catch (Exception e)
			{
				throw new RuntimeException("Failed to load debugger link class", e);
			}
		}
		else
		{
			return ConsoleDebugLink.getInstance();
		}
	}
	
	protected DebugLink()
	{
		// Protected constructor for singleton.
	}
	
	/**
	 * Get an executor to handle commands.
	 */
	abstract public DebugExecutor getExecutor(LexLocation location, Context ctxt);

	/**
	 * Called by a thread when it is created.
	 */
	abstract public void newThread(CPUValue cpu);

	/**
	 * Called by a thread which has stopped, but not at a breakpoint. For example,
	 * when an exception occurs or deadlock is detected, or when a waiting thread
	 * is pushed into the debugger with a suspendOthers call.
	 */
	abstract public void stopped(Context ctxt, LexLocation location, Exception ex);
	
	/**
	 * Called by a thread which has stopped at a breakpoint.
	 */
	abstract public void breakpoint(Context ctxt, Breakpoint bp);
	
	/**
	 * Called by a thread which has hit a tracepoint.
	 */
	abstract public void tracepoint(Context ctxt, Tracepoint tp);
	
	/**
	 * Called by a thread which is terminating, possibly with an exception.
	 */
	abstract public void complete(DebugReason reason, ContextException exception);
	
	
	/**
	 * Read and return a value from the thread's Exchange, responding with an ACK.
	 */
	protected DebugCommand readCommand(SchedulableThread thread) throws InterruptedException
	{
		return thread.debugExch.exchange(DebugCommand.ACK);		
	}
	
	/**
	 * Write a value to the thread's Exchange, and check for an ACK.
	 */
	protected void writeCommand(SchedulableThread thread, DebugCommand response) throws InterruptedException
	{
		if (!thread.debugExch.exchange(response).equals(DebugCommand.ACK))
		{
			throw new RuntimeException("Unexpected ACK from debugger");
		}
	}
}
