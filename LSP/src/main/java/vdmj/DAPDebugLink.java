/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package vdmj;

import java.io.IOException;

import com.fujitsu.vdmj.debug.ConsoleDebugLink;
import com.fujitsu.vdmj.debug.DebugExecutor;
import com.fujitsu.vdmj.debug.DebugLink;
import com.fujitsu.vdmj.debug.DebugReason;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.Stoppoint;
import com.fujitsu.vdmj.scheduler.MainThread;
import com.fujitsu.vdmj.scheduler.SchedulableThread;
import com.fujitsu.vdmj.values.CPUValue;

import dap.DAPResponse;
import dap.DAPServer;
import json.JSONObject;
import workspace.Log;

public class DAPDebugLink extends ConsoleDebugLink
{
	/** Singleton instance */
	private static DebugLink instance;

	private final DAPServer server;

	/**
	 * Get the singleton. 
	 */
	public static DebugLink getInstance()
	{
		if (instance == null)
		{
			instance = new DAPDebugLink();
		}
		
		return instance;
	}
	
	private DAPDebugLink()
	{
		server = DAPServer.getInstance();
	}
	
	@Override
	public DebugExecutor getExecutor(LexLocation location, Context ctxt)
	{
		return new DAPDebugExecutor(location, ctxt);
	}
	
	@Override
	public void newThread(CPUValue cpu)
	{
		try
		{
			Log.printf("New thread %s(%d)", Thread.currentThread().getName(), Thread.currentThread().getId());
			server.writeMessage(new DAPResponse("thread",
				new JSONObject("reason", "started", "threadId", Thread.currentThread().getId())));
		}
		catch (IOException e)
		{
			Log.error(e);
		}
	}
	
	@Override
	public void stopped(Context ctxt, LexLocation location, Exception ex)
	{
		if (!debugging || suspendBreaks)	// Not attached to a debugger or local eval
		{
			return;
		}
		
		SchedulableThread thread = (SchedulableThread) Thread.currentThread();
		Breakpoint bp = getBreakpoint(thread);
		String reason = null;
		
		if (ex != null)
		{
			server.stderr(ex.getMessage() + "\n");
			reason = "exception";
		}
		else if (bp != null)
		{
			if (bp instanceof Stoppoint)
			{
				reason = "breakpoint";
			}
			else
			{
				reason = "step";	// Next, step in or step out
			}
		}
		else
		{
			reason = null;	// No reason displayed
		}
		
		try
		{
			server.writeMessage(new DAPResponse("stopped",
					new JSONObject(
							"reason", reason,
							"threadId", Thread.currentThread().getId(),
							"allThreadsStopped", SchedulableThread.getThreadCount() == 1)));
		}
		catch (IOException e)
		{
			Log.error(e);
		}

		super.stopped(ctxt, location, ex);

		if (ex == null && Thread.currentThread() instanceof MainThread)
		{
			try
			{
				server.writeMessage(new DAPResponse("continued",
						new JSONObject(
							"threadId", Thread.currentThread().getId(),
							"allThreadsContinued", true)));
			}
			catch (IOException e)
			{
				Log.error(e);
			}
		}
	}
	
	@Override
	public void breakpoint(Context ctxt, Breakpoint bp)
	{
		if (debugging && !suspendBreaks)
		{
			// Calls stopped with a null exception, which sends events
			super.breakpoint(ctxt, bp);
		}
	}
	
	@Override
	public void complete(DebugReason reason, ContextException exception)
	{
		try
		{
			Log.printf("End thread %s(%d)", Thread.currentThread().getName(), Thread.currentThread().getId());
			server.writeMessage(new DAPResponse("thread",
				new JSONObject("reason", "exited", "threadId", Thread.currentThread().getId())));
		}
		catch (IOException e)
		{
			Log.error(e);
		}
	}
}
