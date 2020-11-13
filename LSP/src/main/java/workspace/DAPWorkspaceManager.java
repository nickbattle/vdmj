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

package workspace;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Interpreter;
import dap.DAPEvent;
import dap.DAPMessageList;
import dap.DAPRequest;
import dap.DAPResponse;
import dap.DAPServer;
import dap.DAPServerState;
import dap.handlers.DAPInitializeResponse;
import json.JSONArray;
import json.JSONObject;
import lsp.Utils;
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
	private DAPServerState dapServerState;
	private String launchCommand;
	private String defaultName;
	
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
	

	public void setDAPState(DAPServerState dapServerState)
	{
		this.dapServerState = dapServerState;
	}

	/**
	 * DAP methods...
	 */

	public DAPMessageList dapInitialize(DAPRequest request)
	{
		DAPMessageList responses = new DAPMessageList();
		responses.add(new DAPInitializeResponse(request));
		responses.add(new DAPEvent("initialized", null));
		return responses;
	}

	public DAPMessageList configurationDone(DAPRequest request) throws IOException
	{
		try
		{
			DAPDebugReader dbg = null;
			
			try
			{
				dbg = new DAPDebugReader();		// Allow debugging of init sequence
				dbg.start();
				
				heading();
				stdout("Initialized in ... ");
	
				long before = System.currentTimeMillis();
				getInterpreter().init();
				if (defaultName != null) getInterpreter().setDefaultName(defaultName);
				long after = System.currentTimeMillis();
	
				stdout((double)(after-before)/1000 + " secs.\n");
			}
			finally
			{
				if (dbg != null)
				{
					dbg.interrupt();
				}
			}
	
			if (launchCommand != null)
			{
				stdout("\n" + launchCommand + "\n");
				DAPMessageList eval = evaluate(request, launchCommand, "repl");
				
				JSONObject body = eval.get(0).get("body");
				Boolean success = eval.get(0).get("success");
				
				if (success && body != null)
				{
					stdout(body.get("result"));
				}
				else
				{
					stderr(eval.get(0).get("message"));
				}
	
				stdout("\nEvaluation complete.\n");
				clearInterpreter();
				dapServerState.setRunning(false);	// disconnect afterwards
			}
	
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

	public DAPMessageList launch(DAPRequest request, boolean noDebug, String defaultName, String command) throws Exception
	{
		LSPWorkspaceManager.getInstance().checkLoadedFiles();
		
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
			this.defaultName = defaultName;
			this.launchCommand = command;
			
			return new DAPMessageList(request);
		}
		catch (Exception e)
		{
			Log.error(e);
			return new DAPMessageList(request, e);
		}
	}

	public JSONObject ctRuntrace(DAPRequest request, String name, long testNumber) throws Exception
	{
		TCPlugin tc = registry.getPlugin("TC");
		
		if (!tc.getErrs().isEmpty())
		{
			throw new Exception("Type checking errors found");
		}
		
		CTPlugin ct = registry.getPlugin("CT");
		
		if (!ct.generated())
		{
			throw new Exception("Trace not generated");
		}

		if (!ct.completed())
		{
			throw new Exception("Trace still running");
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

	/**
	 * Methods to write direct to stdout/stderr, while a DAP command is being executed.
	 */
	private void heading() throws Exception
	{
		stdout("*\n" +
				"* VDMJ " + Settings.dialect + " Interpreter\n" +
				(noDebug ? "" : "* DEBUG enabled\n") +
				"*\n\nDefault " + (Settings.dialect == Dialect.VDM_SL ? "module" : "class") +
				" is " + getInterpreter().getDefaultName() + "\n");
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
		Command command = Command.parse(expression);

		if (command instanceof PrintCommand)	// ie. evaluate something
		{
			if (!canExecute())
			{
				DAPMessageList responses = new DAPMessageList(request,
						new JSONObject("result", "Cannot start interpreter: errors exist?", "variablesReference", 0));
				dapServerState.setRunning(false);
				clearInterpreter();
				return responses;
			}
			else if (hasChanged())
			{
				DAPMessageList responses = new DAPMessageList(request,
						new JSONObject("result", "Specification has changed: try restart", "variablesReference", 0));
				return responses;
			}
		}
		
		return command.run(request);
	}

	public DAPMessageList threads(DAPRequest request)
	{
		return new DAPMessageList(request, new JSONObject("threads", new JSONArray()));	// empty?
	}

	/**
	 * Termination and cleanup methods.
	 */
	public DAPMessageList disconnect(DAPRequest request, Boolean terminateDebuggee)
	{
		stdout("\nSession disconnected.\n");
		clearInterpreter();
		DAPMessageList result = new DAPMessageList(request);
		return result;
	}

	public DAPMessageList terminate(DAPRequest request, Boolean restart)
	{
		stdout("\nSession terminated.\n");
		clearInterpreter();
		DAPMessageList result = new DAPMessageList(request);
		return result;
	}
	
	private void clearInterpreter()
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
}
