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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import com.fujitsu.vdmj.debug.DebugCommand;
import com.fujitsu.vdmj.debug.DebugLink;
import com.fujitsu.vdmj.debug.DebugType;
import com.fujitsu.vdmj.debug.TraceCallback;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.Tracepoint;
import com.fujitsu.vdmj.scheduler.SchedulableThread;

import dap.DAPMessageList;
import dap.DAPRequest;
import dap.DAPResponse;
import dap.DAPServer;
import json.JSONArray;
import json.JSONObject;
import lsp.Utils;
import workspace.Log;

/**
 * A class to listen for and interact with multiple threads that are being debugged.
 */
public class DAPDebugReader extends Thread implements TraceCallback
{
	private final DAPServer server;
	private final DAPDebugLink link;

	private SchedulableThread debuggedThread = null;
	
	public DAPDebugReader() throws Exception
	{
		setName("DAPDebugReader");
		DAPDebugExecutor.init();
		server = DAPServer.getInstance();
		link = (DAPDebugLink)DebugLink.getInstance();
	}
	
	@Override
	public void run()
	{
		link.setTraceCallback(this);
		
		while (link.waitForStop())
		{
			try
			{
				debuggedThread = link.getDebugThread();		// Initially bp thread
				server.writeMessage(breakpointEvent(link.getBreakpoint(debuggedThread), debuggedThread));
				server.writeMessage(text("[debug]> "));
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
			JSONObject message = server.readMessage();
			
			if (message == null)	// EOF - closed socket?
			{
				Log.printf("End of stream detected");
				link.killThreads();
				return false;
			}
			
			DAPRequest request = new DAPRequest(message);
			
			switch ((String)request.get("command"))
			{
				case "threads":
					server.writeMessage(doThreads(request));
					return true;
					
				case "setBreakpoints":
					JSONObject arguments = request.get("arguments");
					JSONObject source = arguments.get("source");
					URI uri = Utils.fileToURI(new File((String)source.get("path")));
					JSONArray lines = arguments.get("lines");
					DAPMessageList responses = server.getState().getManager().setBreakpoints(request, uri, lines);
					
					for (JSONObject response: responses)
					{
						server.writeMessage(response);
					}
					
					return true;
				
				default:
					// process via the debug link...
					break;
			}
			
			DebugCommand command = parse(request);
			SchedulableThread targetThread = threadFor(request);
			
			if (command.getType() == null)	// Ignore - payload is DAP response
			{
				server.writeMessage((JSONObject) command.getPayload());
				return true;
			}
			
			DebugCommand response = link.sendCommand(targetThread, command);
			DAPResponse dapResp = new DAPResponse(request, true, null, response.getPayload());

			switch (response.getType())
			{
				case RESUME:
					link.resumeThreads();
					server.writeMessage(text("\n"));
					server.writeMessage(dapResp);
					return false;

				case STOP:
				case QUIT:
				case TERMINATE:
					link.killThreads();
					server.writeMessage(dapResp);
					return false;
					
				case DATA:
					server.writeMessage(dapResp);
					server.writeMessage(text("[debug]> "));
					return true;

				default:
					server.writeMessage(dapResp);
					return true;
			}
		}
		catch (Exception e)
		{
			Log.error(e);
			return false;
		}
	}

	private SchedulableThread threadFor(DAPRequest request)
	{
		String command = request.get("command");
		JSONObject arguments = request.get("arguments");
		Long th = arguments.get("threadId");
		
		switch (command)
		{
			case "continue":
			case "stepIn":
			case "stepOut":
			case "next":
			case "stackTrace":
				return findThread(th);
				
			case "scopes":
			case "variables":
			default:
				return debuggedThread;
		}
	}

	private SchedulableThread findThread(Long th)
	{
		if (th != null)
		{
			for (SchedulableThread thread: link.getThreads())
			{
				if (thread.getId() == th.longValue())
				{
					return thread;
				}
			}
		}

		Log.printf("Cannot find thread %s", th);
		return debuggedThread;
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
			text(Thread.currentThread().getName() + ": " + s);
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
			text(Thread.currentThread().getName() + ": " + s);
		}
	}
	
	private DAPResponse text(String message)
	{
		return new DAPResponse("output", new JSONObject("output", message));
	}
}
