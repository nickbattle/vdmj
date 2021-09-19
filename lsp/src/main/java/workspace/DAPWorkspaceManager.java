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

package workspace;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.messages.RTLogger;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.scheduler.SchedulableThread;

import dap.AsyncExecutor;
import dap.DAPMessageList;
import dap.DAPRequest;
import dap.DAPResponse;
import dap.DAPServer;
import dap.InitExecutor;
import dap.handlers.DAPInitializeResponse;
import json.JSONArray;
import json.JSONObject;
import lsp.LSPException;
import lsp.Utils;
import rpc.RPCErrors;
import vdmj.DAPDebugReader;
import vdmj.commands.Command;
import vdmj.commands.PrintCommand;
import workspace.plugins.ASTPlugin;
import workspace.plugins.CTPlugin;
import workspace.plugins.INPlugin;
import workspace.plugins.TCPlugin;

public class DAPWorkspaceManager
{
	private static DAPWorkspaceManager INSTANCE = null;
	private final PluginRegistry registry;
	
	private Boolean noDebug;
	private Interpreter interpreter;
	private String launchCommand;
	private String defaultName;
	private DAPDebugReader debugReader;
	
	protected DAPWorkspaceManager()
	{
		this.registry = PluginRegistry.getInstance();
	}

	public static synchronized DAPWorkspaceManager getInstance()
	{
		if (INSTANCE == null)
		{
			INSTANCE = new DAPWorkspaceManager();
		}
		
		return INSTANCE;
	}
	
	/**
	 * This is only used by unit testing.
	 */
	public static void reset()
	{
		if (INSTANCE != null)
		{
			INSTANCE = null;
		}
	}

	/**
	 * DAP methods...
	 */

	public DAPMessageList dapInitialize(DAPRequest request)
	{
		RTLogger.enable(false);
		DAPMessageList responses = new DAPMessageList();
		responses.add(new DAPInitializeResponse(request));
		responses.add(new DAPResponse("initialized", null));
		return responses;
	}

	public DAPMessageList launch(DAPRequest request, boolean noDebug, String defaultName, String command) throws Exception
	{
		if (!canExecute())
		{
			DAPMessageList responses = new DAPMessageList();
			responses.add(new DAPResponse(request, false, "Specification has errors, cannot launch", null));
			stderr("Specification has errors, cannot launch");
			clearInterpreter();
			return responses;
		}
		
		try
		{
			// These values are used in configurationDone
			this.noDebug = noDebug;
			this.launchCommand = command;
			this.defaultName = defaultName;
			
			return new DAPMessageList(request);
		}
		catch (Exception e)
		{
			Log.error(e);
			return new DAPMessageList(request, e);
		}
	}

	public DAPMessageList configurationDone(DAPRequest request) throws IOException
	{
		try
		{
			InitExecutor exec = new InitExecutor("init", request, launchCommand, defaultName);
			exec.start();
			return new DAPMessageList(request);
		}
		catch (Exception e)
		{
			Log.error(e);
			return new DAPMessageList(request, e);
		}
		finally
		{
			launchCommand = null;
		}
	}

	public JSONObject ctRuntrace(DAPRequest request, String name, long testNumber) throws LSPException
	{
		CTPlugin ct = registry.getPlugin("CT");
		
		if (ct.isRunning())
		{
			Log.error("Previous trace is still running...");
			throw new LSPException(RPCErrors.InvalidRequest, "Trace still running");
		}

		/**
		 * If the specification has been modified since we last ran (or nothing has yet run),
		 * we have to re-create the interpreter, otherwise the old interpreter (with the old tree)
		 * is used to "generate" the trace names, so changes are not picked up. Note that a
		 * new tree will have no breakpoints, so if you had any set via a launch, they will be
		 * ignored.
		 */
		refreshInterpreter();
		
		if (specHasErrors())
		{
			throw new LSPException(RPCErrors.ContentModified, "Specification has errors");
		}

		return ct.runtrace(Utils.stringToName(name), testNumber);
	}

	public Interpreter getInterpreter()
	{
		if (interpreter == null)
		{
			try
			{
				TCPlugin tc = registry.getPlugin("TC");
				INPlugin in = registry.getPlugin("IN");
				interpreter = in.getInterpreter(tc.getTC());
			}
			catch (Exception e)
			{
				Log.error(e);
				interpreter = null;
			}
		}
		
		return interpreter;
	}

	private boolean canExecute()
	{
		ASTPlugin ast = registry.getPlugin("AST");
		TCPlugin tc = registry.getPlugin("TC");
		
		return ast.getErrs().isEmpty() && tc.getErrs().isEmpty();
	}
	
	private boolean hasChanged()
	{
		INPlugin in = registry.getPlugin("IN");
		return getInterpreter() != null && getInterpreter().getIN() != in.getIN();
	}
	
	private boolean isDirty()
	{
		ASTPlugin ast = registry.getPlugin("AST");
		return ast.isDirty();
	}

	private void stdout(String message)
	{
		DAPServer.getInstance().stdout(message);
	}
	
	private void stderr(String message)
	{
		DAPServer.getInstance().stderr(message);
	}
	
	public DAPMessageList setBreakpoints(DAPRequest request, File file, JSONArray breakpoints) throws Exception
	{
		JSONArray results = new JSONArray();
		
		Map<Integer, Breakpoint> existing = getInterpreter().getBreakpoints();
		Set<Integer> bps = new HashSet<Integer>(existing.keySet());
		
		for (Integer bpno: bps)
		{
			Breakpoint bp = existing.get(bpno);
			
			if (bp.location.file.equals(file))
			{
				interpreter.clearBreakpoint(bpno);
			}
		}
		
		for (Object object: breakpoints)
		{
			JSONObject breakpoint = (JSONObject) object;
			long line = breakpoint.get("line");
			String logMessage = breakpoint.get("logMessage");
			String condition = breakpoint.get("condition");
			
			if (condition == null || condition.isEmpty())
			{
				condition = breakpoint.get("hitCondition");
			}
			
			if (condition != null && condition.isEmpty()) condition = null;

			if (!noDebug)	// debugging allowed!
			{
				INStatement stmt = interpreter.findStatement(file, (int)line);
				
				if (stmt == null)
				{
					INExpression exp = interpreter.findExpression(file, (int)line);
		
					if (exp == null)
					{
						results.add(new JSONObject("verified", false));
					}
					else
					{
						interpreter.clearBreakpoint(exp.breakpoint.number);
						
						if (logMessage == null || logMessage.isEmpty())
						{
							interpreter.setBreakpoint(exp, condition);
						}
						else
						{
							if (condition != null) Log.error("Ignoring tracepoint condition " + condition);
							interpreter.setTracepoint(exp, logMessage);
						}
						
						results.add(new JSONObject("verified", true));
					}
				}
				else
				{
					interpreter.clearBreakpoint(stmt.breakpoint.number);
					
					if (logMessage == null || logMessage.isEmpty())
					{
						interpreter.setBreakpoint(stmt, condition);
					}
					else
					{
						if (condition != null) Log.error("Ignoring tracepoint condition " + condition);
						interpreter.setTracepoint(stmt, logMessage);
					}

					results.add(new JSONObject("verified", true));
				}
			}
			else
			{
				results.add(new JSONObject("verified", false));
			}
		}
		
		return new DAPMessageList(request, new JSONObject("breakpoints", results));
	}
	
	public DAPMessageList evaluate(DAPRequest request, String expression, String context)
	{
		CTPlugin ct = registry.getPlugin("CT");
		
		if (ct.isRunning())
		{
			DAPMessageList responses = new DAPMessageList(request,
					new JSONObject("result", "Cannot start interpreter: trace still running?", "variablesReference", 0));
			DAPServer.getInstance().setRunning(false);
			clearInterpreter();
			return responses;
		}
		
		Command command = Command.parse(expression);
		
		if (command.notWhenRunning() && AsyncExecutor.currentlyRunning() != null)
		{
			DAPMessageList responses = new DAPMessageList(request,
					new JSONObject("result", "Still running " + AsyncExecutor.currentlyRunning(), "variablesReference", 0));
			return responses;
		}

		if (command instanceof PrintCommand)	// ie. evaluate something
		{
			if (!canExecute())
			{
				DAPMessageList responses = new DAPMessageList(request,
						new JSONObject("result", "Cannot start interpreter: errors exist?", "variablesReference", 0));
				clearInterpreter();
				return responses;
			}
			else if (hasChanged())
			{
				DAPMessageList responses = new DAPMessageList(request,
						new JSONObject("result", "Specification has changed: try restart", "variablesReference", 0));
				return responses;
			}
			else if (isDirty())
			{
				stderr("WARNING: specification has unsaved changes");
			}
		}
		
		return command.run(request);
	}

	public DAPMessageList threads(DAPRequest request)
	{
		List<SchedulableThread> threads = SchedulableThread.getAllThreads();
		Collections.sort(threads);
		JSONArray list = new JSONArray();
		
		for (SchedulableThread thread: threads)
		{
			list.add(new JSONObject(
				"id",	thread.getId(),
				"name", thread.getName()));
		}
		
		return new DAPMessageList(request, new JSONObject("threads", list));
	}

	/**
	 * Termination and cleanup methods.
	 */
	public DAPMessageList disconnect(DAPRequest request, Boolean terminateDebuggee)
	{
		RTLogger.dump(true);
		stdout("\nSession disconnected.\n");
		clearInterpreter();
		DAPMessageList result = new DAPMessageList(request);
		return result;
	}

	public DAPMessageList terminate(DAPRequest request, Boolean restart)
	{
		DAPMessageList result = new DAPMessageList(request);
		RTLogger.dump(true);

		if (restart && canExecute())
		{
			stdout("\nSession restarting...\n");
			LSPWorkspaceManager lsp = LSPWorkspaceManager.getInstance();
			lsp.restart();
		}
		else
		{
			if (restart)
			{
				stdout("Cannot restart: specification has errors");
			}
			
			stdout("\nSession terminated.\n");
			result.add(new DAPResponse("terminated", null));
			result.add(new DAPResponse("exit", new JSONObject("exitCode", 0L)));
		}
		
		clearInterpreter();
		return result;
	}
	
	public void clearInterpreter()
	{
		if (interpreter != null)
		{
			// Clear the BPs since they are embedded in the tree and the next
			// launch may have noDebug set.
			
			Set<Integer> bps = new HashSet<Integer>(interpreter.getBreakpoints().keySet());
			
			for (Integer bpno: bps)
			{
				interpreter.clearBreakpoint(bpno);
			}
			
			interpreter = null;
		}
	}
	
	public boolean refreshInterpreter()
	{
		if (hasChanged())
		{
			Log.printf("Specification has changed, resetting interpreter");
			interpreter = null;
			return true;
		}
		
		return false;
	}
	
	private boolean specHasErrors()
	{
		ASTPlugin ast = registry.getPlugin("AST");
		TCPlugin tc = registry.getPlugin("TC");
		
		return !ast.getErrs().isEmpty() || !tc.getErrs().isEmpty();
	}

	public void setDebugReader(DAPDebugReader debugReader)
	{
		this.debugReader = debugReader;
	}
	
	public DAPDebugReader getDebugReader()
	{
		return debugReader;
	}
	
	public boolean getNoDebug()
	{
		return noDebug;
	}
}
