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
import java.util.Collections;
import java.util.List;

import com.fujitsu.vdmj.debug.DebugCommand;
import com.fujitsu.vdmj.debug.DebugLink;
import com.fujitsu.vdmj.debug.DebugType;
import com.fujitsu.vdmj.debug.TraceCallback;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.Tracepoint;
import com.fujitsu.vdmj.scheduler.SchedulableThread;

import dap.DAPRequest;
import dap.DAPResponse;
import dap.DAPServer;
import json.JSONArray;
import json.JSONObject;
import workspace.Log;

/**
 * A class to listen for and interact with multiple threads that are being debugged.
 */
public class DAPDebugReader extends Thread implements TraceCallback
{
	private final DAPServer server;
	private final DAPDebugLink link;

	private SchedulableThread debuggedThread = null;
	private LexLocation lastLoc = null;
	private SchedulableThread lastThread = null;
	
	public DAPDebugReader() throws Exception
	{
		server = DAPServer.getInstance();
		link = (DAPDebugLink)DebugLink.getInstance();
		link.setExecutor(new DAPDebugExecutor());
	}
	
	@Override
	public void run()
	{
		setName("DAPDebugReader");
		link.setTraceCallback(this);
		
		while (link.waitForStop())
		{
			try
			{
				lastThread = debuggedThread;
				debuggedThread = link.getDebugThread();		// Initially bp thread
				server.writeMessage(breakpointEvent(link.getBreakpoint(debuggedThread), debuggedThread));
				while (doCommand());
			}
			catch (IOException e)
			{
				Log.error(e);
			}
		}
	}

	private boolean doCommand()
	{
		try
		{
			Breakpoint bp   = link.getBreakpoint(debuggedThread);
			LexLocation loc = link.getLocation(debuggedThread);
			
			if (bp != null && bp.number != 0)	// Zero is used for next/step breakpoints.
			{
				lastLoc = bp.location;
			}
			
			if (!debuggedThread.equals(lastThread) || !loc.equals(lastLoc))
			{
				lastLoc = loc;
			}
			
			JSONObject message = server.readMessage();
			
			if (message == null)	// EOF
			{
				Log.printf("End of stream detected");
				return false;
			}
			
			DAPRequest request = new DAPRequest(message);
			DebugCommand command = parse(request);
			
			if (command.getType() == null)	// Ignore - payload is DAP response
			{
				server.writeMessage((JSONObject) command.getPayload());
				return true;
			}
			
			switch (command.getType())
			{
				case THREADS:
					server.writeMessage(doThreads(request));
					return true;

				default:
				{
					DebugCommand response = link.sendCommand(debuggedThread, command);

					switch (response.getType())
					{
						case RESUME:
							link.resumeThreads();
							server.writeMessage(new DAPResponse(request, true, null, response.getPayload()));
							return false;

						case STOP:
						case QUIT:
						case TERMINATE:
							link.killThreads();
							server.writeMessage(new DAPResponse(request, true, null, response.getPayload()));
							return false;

						default:
							server.writeMessage(new DAPResponse(request, true, null, response.getPayload()));
							return true;
					}
				}
			}
		}
		catch (IOException e)
		{
			Log.error(e);
			return false;
		}
	}

	private DebugCommand parse(DAPRequest request) throws IOException
	{
		String command = request.get("command");
		
		switch (command)
		{
			case "terminate":
				return DebugCommand.TERMINATE;
				
			case "evaluate":
				return new DebugCommand(DebugType.PRINT, request.get("arguments"));
				
			case "continue":
				return DebugCommand.CONTINUE;
				
			case "stepIn":
				return DebugCommand.STEP;
				
			case "stepOut":
				return DebugCommand.OUT;
				
			case "next":
				return DebugCommand.NEXT;
				
			case "stackTrace":
				return new DebugCommand(DebugType.STACK, request.get("arguments"));
			
			case "scopes":
				return new DebugCommand(DebugType.DATA, request.get("arguments"));
				
			case "variables":
				return new DebugCommand(DebugType.DATA, request.get("arguments"));
				
			case "threads":
				return DebugCommand.THREADS;
				
			case "setBreakpoints":
				return new DebugCommand(null, new DAPResponse(request, false, "Unsupported at breakpoint", null));
			
			default:
				return new DebugCommand(null, new DAPResponse(request, false, "Unsupported command: " + command, null));
		}
	}

	private DAPResponse breakpointEvent(Breakpoint bp, SchedulableThread stoppedThread)
	{
		return new DAPResponse("stopped",
			new JSONObject(
				"reason", "breakpoint",
				"threadId", stoppedThread.getId(),
				"allThreadsStopped", true));
	}

	private DAPResponse doThreads(DAPRequest request)
	{
		List<SchedulableThread> threads = link.getThreads();
		Collections.sort(threads);
		JSONArray list = new JSONArray();
		
		for (SchedulableThread thread: threads)
		{
			list.add(new JSONObject(
				"id",	thread.getId(),
				"name", thread.getName()));
		}
		
		return new DAPResponse(request, true, null, new JSONObject("threads", list));
	}
	
	@Override
	public String toString()
	{
		return getName();
	}
	
	@Override
	public void tracepoint(Context ctxt, Tracepoint tp)
	{
		if (tp.condition == null)
		{
			String s = "Reached trace point [" + tp.number + "]";
			Console.out.println(Thread.currentThread().getName() + ": " + s);
		}
		else
		{
			String result = null;
			
			try
			{
				result = tp.condition.eval(ctxt).toString();
			}
			catch (Exception e)
			{
				result = e.getMessage();
			}
			
			String s = tp.trace + " = " + result + " at trace point [" + tp.number + "]";
			Console.out.println(Thread.currentThread().getName() + ": " + s);
		}
	}
}
