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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package vdmj;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Exchanger;

import com.fujitsu.vdmj.debug.DebugCommand;
import com.fujitsu.vdmj.debug.DebugType;
import com.fujitsu.vdmj.debug.TraceCallback;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INSeqEnumExpression;
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
import workspace.Diag;
import workspace.plugins.DAPPlugin;

/**
 * A class to listen for and interact with multiple threads that are being debugged.
 */
public class DAPDebugReader extends Thread implements TraceCallback
{
	private final DAPServer server;
	private final DAPDebugLink link;

	private Exchanger<JSONObject> exchanger = null;
	private SchedulableThread debuggedThread = null;
	
	public DAPDebugReader() throws Exception
	{
		setName("DAPDebugReader");
		DAPDebugExecutor.init();
		server = DAPServer.getInstance();
		link = DAPDebugLink.getNewInstance();
	}
	
	public boolean isListening()
	{
		return exchanger != null;
	}

	public void handle(JSONObject request)
	{
		try
		{
			exchanger.exchange(request);	// Send to debugger thread
			exchanger.exchange(null);		// Wait for it to be processed
		}
		catch (Exception e)
		{
			Diag.error(e);
		}
	}
	
	@Override
	public void run()
	{
		link.setTraceCallback(this);
		
		while (link.waitForStop())
		{
			try
			{
				exchanger = new Exchanger<JSONObject>();
				debuggedThread = link.getDebugThread();
				Diag.info("----------------- DEBUG STOP in %s", debuggedThread.getName());
				
				while (doCommand());
				
				Diag.info("----------------- RESUME");
				exchanger = null;
			}
			catch (Exception e)
			{
				Diag.error(e);
				link.killThreads();
				break;
			}
		}
		
		link.reset();
	}
	
	private boolean doCommand() throws Exception
	{
		JSONObject dapMessage = null;
		
		try
		{
			dapMessage = exchanger.exchange(null);
		}
		catch (InterruptedException e)	// eg. via QuitCommand
		{
			Diag.info("DAPDebugReader exchange interrupted");
			link.killThreads();
			return false;
		}
		
		DAPRequest dapRequest = new DAPRequest(dapMessage);
		DAPResponse dapResponse = null;
		boolean result = true;

		switch (dapRequest.getCommand())
		{
			case "threads":
				server.writeMessage(doThreads(dapRequest));
				result = true;
				break;

			case "setBreakpoints":
			{
				JSONObject arguments = dapRequest.get("arguments");
				JSONObject source = arguments.get("source");
				File file = Utils.pathToFile(source.get("path"));
				JSONArray lines = arguments.get("breakpoints");
				DAPMessageList responses = DAPPlugin.getInstance().dapSetBreakpoints(dapRequest, file, lines);

				for (JSONObject response: responses)
				{
					server.writeMessage(response);
				}

				result = true;
				break;
			}

			case "setFunctionBreakpoints":
			{
				JSONObject arguments = dapRequest.get("arguments");
				JSONArray names = arguments.get("breakpoints");
				DAPMessageList responses = DAPPlugin.getInstance().dapSetFunctionBreakpoints(dapRequest, names);

				for (JSONObject response: responses)
				{
					server.writeMessage(response);
				}

				result = true;
				break;
			}

			case "setExceptionBreakpoints":
			{
				JSONObject arguments = dapRequest.get("arguments");
				JSONArray filterOptions = arguments.get("filterOptions");
				DAPMessageList responses = DAPPlugin.getInstance().dapSetExceptionBreakpoints(dapRequest, filterOptions);

				for (JSONObject response: responses)
				{
					server.writeMessage(response);
				}

				result = true;
				break;
			}

			case "terminate":
				link.killThreads();
				dapResponse = new DAPResponse(dapRequest, true, null, null);
				server.writeMessage(dapResponse);
				server.stdout("Debug session terminated (RESTART is advised)\n");
				result = false;
				break;
				
			case "disconnect":
				link.killThreads();
				dapResponse = new DAPResponse(dapRequest, true, null, null);
				server.writeMessage(dapResponse);
				server.stdout("Debug session disconnected\n");
				result = false;
				break;
				
			default:
				DebugCommand command = parse(dapRequest);
				SchedulableThread targetThread = threadFor(dapRequest);

				if (command.getType() == null)	// Ignore - payload is DAP response
				{
					server.writeMessage((JSONObject) command.getPayload());
					result = true;
				}
				else
				{
					DebugCommand response = link.sendCommand(targetThread, command);
					dapResponse = new DAPResponse(dapRequest, true, null, response.getPayload());
	
					switch (response.getType())
					{
						case RESUME:
							link.resumeThreads();
							server.writeMessage(dapResponse);
							result = false;
							break;
	
						case PRINT:
							server.writeMessage(dapResponse);
							result = true;
							break;
	
						default:
							server.writeMessage(dapResponse);
							result = true;
							break;
					}
				}
		}
		
		exchanger.exchange(null);	// Let handle return
		return result;
	}

	private SchedulableThread threadFor(DAPRequest request)
	{
		JSONObject arguments = request.get("arguments");
		Long th = arguments.get("threadId");
		
		if (th != null)		// Command has a threadId target
		{
			for (SchedulableThread thread: link.getThreads())
			{
				if (thread.getId() == th.longValue())
				{
					return thread;
				}
			}

			Diag.error("Cannot find threadId %s", th);
		}

		return debuggedThread;
	}

	private DebugCommand parse(DAPRequest request) throws IOException
	{
		String command = request.getCommand();
		JSONObject arguments = request.get("arguments");
		String context = arguments.get("context");
		
		switch (command)
		{
			case "evaluate":
				if ("variables".equals(context))
				{
					// In the variables context, the expression sent to evaluate is already
					// the value of the variable, so just send it back as the result.

					return new DebugCommand(null,
						new DAPResponse(request, true, null,
							new JSONObject("result", arguments.get("expression"), "variablesReference", 0)));
				}
				else if ("watch".equals(context))
				{
					return new DebugCommand(DebugType.PRINT, arguments);
				}
				else if ("repl".equals(context))
				{
					String expression = arguments.get("expression");
					
					if (expression != null && expression.startsWith("p "))
					{
						expression = expression.substring(2);
					}
					else if (expression != null && expression.startsWith("print "))
					{
						expression = expression.substring(6);
					}
					else
					{
						return new DebugCommand(null,
								new DAPResponse(request, false,
									"Can only use \"[p]rint <expression>\" at breakpoint", null));
					}
					
					arguments.put("expression", expression);
					return new DebugCommand(DebugType.PRINT, arguments);
				}
				else	// Unknown context
				{
					Diag.info("Ignoring command %s, context %s", command, context);
					return new DebugCommand(null,
						new DAPResponse(request, false, "(Stop debugger first)", null));
				}
				
			case "continue":
				return DebugCommand.CONTINUE;
				
			case "stepIn":
				return DebugCommand.STEP;
				
			case "stepOut":
				return DebugCommand.OUT;
				
			case "next":
				return DebugCommand.NEXT;
				
			case "stackTrace":
				return new DebugCommand(DebugType.STACK, arguments);
			
			case "scopes":
				return new DebugCommand(DebugType.SCOPES, arguments);
				
			case "variables":
				return new DebugCommand(DebugType.VARIABLES, arguments);
				
			default:
				return new DebugCommand(null,
					new DAPResponse(request, false, "Unsupported command: " + command, null));
		}
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
			String s = "Reached trace point " + tp.location + "\n";
			server.stdout(Thread.currentThread().getName() + ": " + s);
		}
		else
		{
			String message = null;
			
			try
			{
				// A trace like "Weight = {x} kilos" is [ "Weight = ", x, " kilos" ]
				
				if (tp.condition instanceof INSeqEnumExpression)
				{
					INSeqEnumExpression seq = (INSeqEnumExpression)tp.condition;
					StringBuilder sb = new StringBuilder();
					
					for (INExpression exp: seq.members)
					{
						String v = exp.eval(ctxt).toString();
						
						if (v.startsWith("\""))
						{
							sb.append(v.substring(1, v.length()-1));
						}
						else if (!exp.toString().equals("\"\""))
						{
							sb.append(v);
						}
					}
					
					message = sb.toString();
				}
				else
				{
					message = tp.trace + " = " + tp.condition.eval(ctxt).toString();
				}
			}
			catch (Exception e)
			{
				message = e.getMessage();
			}
			
			server.stdout(Thread.currentThread().getName() + ": " + message + "\n");
		}
	}
}
