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

import com.fujitsu.vdmj.debug.ConsoleDebugLink;
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
import com.fujitsu.vdmj.values.OperationValue;

import dap.DAPRequest;
import dap.DAPResponse;
import dap.DAPServer;
import json.JSONObject;

/**
 * A class to listen for and interact with multiple threads that are being debugged.
 */
public class DAPDebugReader extends Thread implements TraceCallback
{
	private final DAPServer server;
	private final ConsoleDebugLink link;

	private SchedulableThread debuggedThread = null;
	private LexLocation lastLoc = null;
	private SchedulableThread lastThread = null;
	
	public DAPDebugReader() throws Exception
	{
		server = DAPServer.getInstance();
		link = (ConsoleDebugLink)DebugLink.getInstance();
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
			
			DAPRequest request = new DAPRequest(server.readMessage());
			DebugCommand command = parse(request);
			
			if (command.getType() == null)	// Ignore - payload is DAP response
			{
				server.writeMessage((JSONObject) command.getPayload());
				return true;
			}
			
			switch (command.getType())
			{
				case THREADS:
					doThreads();
					return true;

				case THREAD:
					doThread(command);
					return true;

				default:
				{
					DebugCommand response = link.sendCommand(debuggedThread, command);

					switch (response.getType())
					{
						case RESUME:
							link.resumeThreads();
							return false;

						case STOP:
						case QUIT:
							link.killThreads();
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
			return false;
		}
	}

	private DebugCommand parse(DAPRequest request) throws IOException
	{
		String command = request.get("command");
		
		switch (command)
		{
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
				
			case "setBreakpoints":
				return new DebugCommand(null, new DAPResponse(request, false, "Unsupported at breakpoint", null));
			
			default:
				return new DebugCommand(null, new DAPResponse(request, false, "Unsupported command", null));
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

	private void doThreads()
	{
		List<SchedulableThread> threads = link.getThreads();
		Collections.sort(threads);
		
		if (threads.isEmpty())
		{
			Console.out.println("No threads?");
		}
		else
		{
    		int maxName = 0;
    		long maxNum = 0;
    		
    		for (SchedulableThread th: threads)
    		{
    			if (th.getName().length() > maxName)
    			{
    				maxName = th.getName().length();
    			}
    			
    			if (th.getId() > maxNum)
    			{
    				maxNum = th.getId();
    			}
    		}
    		
    		int width = (int)Math.floor(Math.log10(maxNum)) + 1;
    		
    		for (SchedulableThread th: threads)
    		{
    			Breakpoint bp = link.getBreakpoint(th);
    			OperationValue guard = link.getGuardOp(th);
    			LexLocation loc = link.getLocation(th);
    			String info = "(not started)";
    			
    			if (bp != null)
    			{
    				info = bp.toString();
    			}
    			else if (guard != null)
    			{
    				info = "sync: " + guard.name.getName() + " " + loc;
    			}
    			else if (loc != null)
    			{
    				info = loc.toString();
    			}
    			
    			String format = String.format("%%%dd: %%-%ds  %%s\n", width, maxName);
    			Console.out.printf(format, th.getId(), th.getName(), info);
    		}
    		
    		Console.out.println();
		}
	}
	
	private void doThread(DebugCommand cmd)
	{
		Integer n = (Integer)cmd.getPayload();

		for (SchedulableThread th: link.getThreads())
		{
			if (th.getId() == n)
			{
				debuggedThread = th;
				return;
			}
		}

		Console.out.println("No such thread Id - try 'threads'");
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
