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
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.definitions.INDefinitionList;
import com.fujitsu.vdmj.in.definitions.INExplicitFunctionDefinition;
import com.fujitsu.vdmj.in.definitions.INExplicitOperationDefinition;
import com.fujitsu.vdmj.in.definitions.INImplicitFunctionDefinition;
import com.fujitsu.vdmj.in.definitions.INImplicitOperationDefinition;
import com.fujitsu.vdmj.in.expressions.INBinaryExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.messages.RTLogger;
import com.fujitsu.vdmj.messages.RTValidator;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Catchpoint;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.scheduler.SchedulableThread;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

import dap.AsyncExecutor;
import dap.DAPInitializeResponse;
import dap.DAPMessageList;
import dap.DAPRequest;
import dap.DAPResponse;
import dap.DAPServer;
import dap.InitExecutor;
import dap.RemoteControlExecutor;
import json.JSONArray;
import json.JSONObject;
import lsp.LSPServer;
import lsp.Utils;
import rpc.RPCRequest;
import vdmj.DAPDebugReader;
import vdmj.commands.Command;
import vdmj.commands.PrintCommand;
import vdmj.commands.ScriptCommand;
import workspace.plugins.ASTPlugin;
import workspace.plugins.CTPlugin;
import workspace.plugins.INPlugin;
import workspace.plugins.TCPlugin;

public class DAPWorkspaceManager
{
	private static DAPWorkspaceManager INSTANCE = null;
	private final PluginRegistry registry;
	
	private JSONObject clientCapabilities;
	private Boolean noDebug;
	private Interpreter interpreter;
	private String launchCommand;
	private String defaultName;
	private DAPDebugReader debugReader;
	private String remoteControl;
	private String logging;
	
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

	public DAPMessageList dapInitialize(DAPRequest request, JSONObject clientCapabilities)
	{
		this.clientCapabilities = clientCapabilities;
		DAPMessageList responses = new DAPMessageList();
		responses.add(new DAPInitializeResponse(request));
		responses.add(new DAPResponse("initialized", null));
		return responses;
	}

	public DAPMessageList launch(DAPRequest request,
			boolean noDebug, String defaultName, String command, String remoteControl, String logging) throws Exception
	{
		LSPWorkspaceManager manager = LSPWorkspaceManager.getInstance();
		int retry = 50;		// 5s worth of 100ms
		
		while (retry > 0 && manager.checkInProgress())
		{
			Diag.fine("Waiting for check to complete, %d", retry);
			pause(100);
			retry--;
		}
		
		if (manager.checkInProgress())
		{
			DAPMessageList responses = new DAPMessageList();
			responses.add(new DAPResponse(request, false, "Specification being checked, cannot launch", null));
			stderr("Specification being checked, cannot launch");
			clearInterpreter();
			return responses;
		}
		
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
			this.remoteControl = remoteControl;
			this.logging = logging;
			
			clearInterpreter();
			processSettings(request);
			
			return new DAPMessageList(request);
		}
		catch (Exception e)
		{
			Diag.error(e);
			return new DAPMessageList(request, e);
		}
	}

	/**
	 * Pick out request arguments that are VDMJ Settings.
	 */
	private void processSettings(DAPRequest request)
	{
		JSONObject args = request.get("arguments");
		JSONObject settings = args.get("settings");
		
		if (settings != null)
		{
			Diag.info("Updating settings: %s", settings);
			
			for (String key: settings.keySet())
			{
				switch (key)
				{
					case "dynamicTypeChecks":
						Settings.dynamictypechecks = settings.get(key);
						break;
						
					case "invariantsChecks":
						Settings.invchecks = settings.get(key);
						break;
						
					case "preConditionChecks":
						Settings.prechecks = settings.get(key);
						break;
						
					case "postConditionChecks":
						Settings.postchecks = settings.get(key);
						break;
						
					case "measureChecks":
						Settings.measureChecks = settings.get(key);
						break;
					
					case "exceptions":
						Settings.exceptions = settings.get(key);
						break;
					
					default:
						Diag.warning("Ignoring setting %s", key);
						break;
				}
			}
		}
		
		JSONObject properties = args.get("properties");
		
		if (properties != null)
		{
			Diag.info("Updating properties: %s", properties);

			for (String key: properties.keySet())
			{
				switch (key)
				{
					case "vdmj.annotations.packages":
					case "vdmj.annotations.debug":
					case "vdmj.mapping.search_path":
					case "vdmj.tc.skip_recursive_check":
					case "vdmj.tc.skip_cyclic_check":
					case "vdmj.tc.max_errors":
					case "vdmj.scheduler.fcfs_timeslice":
					case "vdmj.scheduler.virtual_timeslice":
					case "vdmj.scheduler.jitter":
					case "vdmj.rt.duration_default":
					case "vdmj.rt.duration_transactions":
					case "vdmj.rt.log_instvarchanges":
					case "vdmj.rt.max_periodic_overlaps":
					case "vdmj.rt.diags_guards":
					case "vdmj.rt.diags_timestep":
					case "vdmj.in.powerset_limit":
						if (properties.get(key) == null)
						{
							System.clearProperty(key);
						}
						else
						{
							String value = properties.get(key).toString().trim();
							System.setProperty(key, value);
						}
						break;

					default:
						Diag.warning("Ignoring property %s", key);
						break;
				}
			}
			
			// System properties override those from the properties file
			Diag.info("Reading properties from %s", LSPWorkspaceManager.PROPERTIES);
			Properties.init(LSPWorkspaceManager.PROPERTIES);
		}
	}
	
	/**
	 * This puts the Settings and VDMJ properties back to the default for the project.
	 * It undoes changes from processSettings above.
	 */
	private void restoreSettings()
	{
		Diag.info("Resetting settings");
		Settings.dynamictypechecks = true;
		Settings.invchecks = true;
		Settings.prechecks = true;
		Settings.postchecks = true;
		Settings.measureChecks = true;
		Settings.exceptions = false;
		
		// Clear any System property overrides...
		System.clearProperty("vdmj.annotations.packages");
		System.clearProperty("vdmj.annotations.debug");
		System.clearProperty("vdmj.mapping.search_path");
		System.clearProperty("vdmj.tc.skip_recursive_check");
		System.clearProperty("vdmj.tc.skip_cyclic_check");
		System.clearProperty("vdmj.tc.max_errors");
		System.clearProperty("vdmj.scheduler.fcfs_timeslice");
		System.clearProperty("vdmj.scheduler.virtual_timeslice");
		System.clearProperty("vdmj.scheduler.jitter");
		System.clearProperty("vdmj.rt.duration_default");
		System.clearProperty("vdmj.rt.duration_transactions");
		System.clearProperty("vdmj.rt.log_instvarchanges");
		System.clearProperty("vdmj.rt.max_periodic_overlaps");
		System.clearProperty("vdmj.rt.diags_guards");
		System.clearProperty("vdmj.rt.diags_timestep");
		System.clearProperty("vdmj.in.powerset_limit");
		
		// Reset properties from the file
		Diag.info("Resetting properties from %s", LSPWorkspaceManager.PROPERTIES);
		Properties.init(LSPWorkspaceManager.PROPERTIES);
	}

	public DAPMessageList configurationDone(DAPRequest request)
	{
		try
		{
			// Interpreter may already have been created by setBreakpoint calls during configuration.
			
			if (Settings.dialect == Dialect.VDM_RT && logging != null)
			{
				File file = new File(logging);
				RTLogger.setLogfileName(file);
				Properties.rt_log_instvarchanges = true;
				Diag.info("RT events now logged to %s", file.getAbsolutePath());
			}
			
			if (remoteControl != null)
			{
				RemoteControlExecutor exec = new RemoteControlExecutor("remote", request, remoteControl, defaultName);
				exec.start();
			}
			else
			{
				InitExecutor exec = new InitExecutor("init", request, launchCommand, defaultName);
				exec.start();
			}
			
			return new DAPMessageList(request);
		}
		catch (Exception e)
		{
			Diag.error(e);
			return new DAPMessageList(request, e);
		}
		finally
		{
			launchCommand = null;
			remoteControl = null;
			logging = null;
		}
	}

	public boolean hasClientCapability(String dotName)
	{
		Boolean cap = getClientCapability(dotName);
		return cap != null && cap;
	}
	
	public <T> T getClientCapability(String dotName)
	{
		T capability = clientCapabilities.getPath(dotName);
		
		if (capability != null)
		{
			Diag.info("Client capability %s = %s", dotName, capability);
			return capability;
		}
		else
		{
			Diag.info("Missing client capability: %s", dotName);
			return null;
		}
	}

	public Interpreter getInterpreter()
	{
		if (interpreter == null)
		{
			try
			{
				INPlugin in = registry.getPlugin("IN");
				interpreter = in.getInterpreter();
			}
			catch (Exception e)
			{
				Diag.error(e);
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
	
	private void sendMessage(Long type, String message)
	{
		try
		{
			LSPServer.getInstance().writeMessage(RPCRequest.notification("window/showMessage",
					new JSONObject("type", type, "message", message)));
		}
		catch (IOException e)
		{
			Diag.error("Failed sending message: ", message);
		}
	}
	
	public DAPMessageList setBreakpoints(DAPRequest request, File file, JSONArray breakpoints) throws Exception
	{
		JSONArray results = new JSONArray();
		
		Map<Integer, Breakpoint> existing = getInterpreter().getBreakpoints();
		Set<Integer> bps = new HashSet<Integer>(existing.keySet());
		
		for (Integer bpno: bps)
		{
			Breakpoint bp = existing.get(bpno);
			
			if (bp.location.file.equals(file) && !bp.isFunction())
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
						results.add(new JSONObject("verified", false, "message", "No statement or expression here"));
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
							if (condition != null)
							{
								Diag.error("Ignoring tracepoint condition " + condition);
							}
							
							interpreter.setTracepoint(exp, expressionList(logMessage));
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
						if (condition != null)
						{
							Diag.error("Ignoring tracepoint condition " + condition);
						}
						
						interpreter.setTracepoint(stmt, expressionList(logMessage));
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
	
	public DAPMessageList setFunctionBreakpoints(DAPRequest request, JSONArray breakpoints) throws Exception
	{
		JSONArray results = new JSONArray();
		
		Map<Integer, Breakpoint> existing = getInterpreter().getBreakpoints();
		Set<Integer> bps = new HashSet<Integer>(existing.keySet());
		
		for (Integer bpno: bps)
		{
			Breakpoint bp = existing.get(bpno);
			
			if (bp.isFunction())
			{
				interpreter.clearBreakpoint(bpno);
			}
		}
		
		for (Object object: breakpoints)
		{
			JSONObject breakpoint = (JSONObject) object;
			String name = breakpoint.get("name");
			String condition = breakpoint.get("condition");
			
			if (condition == null || condition.isEmpty())
			{
				condition = breakpoint.get("hitCondition");
			}
			
			if (condition != null && condition.isEmpty()) condition = null;

			if (!noDebug)	// debugging allowed!
			{
				LexTokenReader ltr = new LexTokenReader(name, Dialect.VDM_SL);
				LexToken token = ltr.nextToken();
				ltr.close();

				INPlugin in = registry.getPlugin("IN");
				INDefinitionList list = null;

				if (token.is(Token.IDENTIFIER))
				{
					LexIdentifierToken id = (LexIdentifierToken)token;
					TCNameToken tok = new TCNameToken(id.location, interpreter.getDefaultName(), id.name);
					list = in.findDefinition(tok);
				}
				else if (token.is(Token.NAME))
				{
					list = in.findDefinition(new TCNameToken((LexNameToken)token));
				}
				
				INNode node = null;
				
				if (!list.isEmpty())
				{
					INDefinition d = list.elementAt(0);
					
					if (d instanceof INExplicitFunctionDefinition)
					{
						INExplicitFunctionDefinition efd = (INExplicitFunctionDefinition)d;
						node = efd.body;
					}
					else if (d instanceof INImplicitFunctionDefinition)
					{
						INImplicitFunctionDefinition ifd = (INImplicitFunctionDefinition)d;
						node = ifd.body;
					}
					else if (d instanceof INExplicitOperationDefinition)
					{
						INExplicitOperationDefinition eod = (INExplicitOperationDefinition)d;
						node = eod.body;
					}
					else if (d instanceof INImplicitOperationDefinition)
					{
						INImplicitOperationDefinition iod = (INImplicitOperationDefinition)d;
						node = iod.body;
					}
				}

				if (node instanceof INExpression)
				{
					INExpression exp = (INExpression) node;
					
					while (exp instanceof INBinaryExpression)
					{
						// None of the binary expressions check their BP, to avoid excessive stepping
						// when going through (say) a chain of "and" clauses. So if we've picked a
						// binary expression here, we move the BP to the left hand.
						INBinaryExpression bexp = (INBinaryExpression)exp;
						exp = bexp.left;
					}
					
					interpreter.clearBreakpoint(exp.breakpoint.number);
					Breakpoint bp = interpreter.setBreakpoint(exp, condition);
					bp.setFunction();
					results.add(new JSONObject(
							"verified", true,
							"source", Utils.lexLocationToSource(exp.location),
							"line", exp.location.startLine));
				}
				else if (node instanceof INStatement)
				{
					INStatement stmt = (INStatement) node;
					interpreter.clearBreakpoint(stmt.breakpoint.number);
					
					Breakpoint bp = interpreter.setBreakpoint(stmt, condition);
					bp.setFunction();
					results.add(new JSONObject(
							"verified", true,
							"source", Utils.lexLocationToSource(stmt.location),
							"line", stmt.location.startLine));
				}
				else if (list.isEmpty())
				{
					Diag.error("Function breakpoint name %s not found", name);
					results.add(new JSONObject("verified", false, "message", name + " is not visible or not found"));
				}
				else
				{
					Diag.error("Function breakpoint %s is not function or operation", name);
					results.add(new JSONObject("verified", false, "message", " is not a function or operation"));
				}
			}
			else
			{
				results.add(new JSONObject("verified", false));
			}
		}
		
		return new DAPMessageList(request, new JSONObject("breakpoints", results));
	}

	public DAPMessageList setExceptionBreakpoints(DAPRequest request, JSONArray filterOptions)
	{
		for (Catchpoint cp: getInterpreter().getCatchpoints())
		{
			interpreter.clearBreakpoint(cp.number);
		}
		
		JSONArray results = new JSONArray();
		
		if (filterOptions == null)
		{
			String error = "No filterOptions";
			Diag.error(error);
			results.add(new JSONObject("verified", false, "message", error));
		}
		else
		{
			for (int i=0; i<filterOptions.size(); i++)
			{
				JSONObject filterOption = filterOptions.index(i);
				
				if (filterOption.get("filterId").equals("VDM_Exceptions"))
				{
					try
					{
						String condition = filterOption.get("condition");
						interpreter.setCatchpoint(condition);
						results.add(new JSONObject("verified", true));
					}
					catch (Exception e)
					{
						String error = "Illegal condition: " + e.getMessage(); 
						Diag.error(error);
						results.add(new JSONObject("verified", false, "message", error));
						sendMessage(1L, error);
					}
				}
				else
				{
					String error = "Unknown filterOption Id " + filterOption.get("filterId");
					Diag.error(error);
					results.add(new JSONObject("verified", false, "message", error));
					sendMessage(1L, error);
				}
			}
		}

		return new DAPMessageList(request, new JSONObject("breakpoints", results));
	}

	private String expressionList(String trace)
	{
		// Turn a string like "Weight = {x} kilos" into [ "Weight = ", x, " kilos" ]
		
		Pattern p = Pattern.compile("\\{([^{]*)\\}");
		Matcher m = p.matcher(trace);
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		String sep = "";
		
		while(m.find())
		{
			sb.append(sep);
			sb.append(" \"");
		    m.appendReplacement(sb, "\", " + m.group(1));
		    sep = ",";
		}
		
		sb.append(sep);
		sb.append(" \"");
		m.appendTail(sb);
		sb.append("\" ]");
		
		return sb.toString();
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
		
		if ("watch".equals(context))	// watch received outside execution
		{
			Diag.info("Ignoring watch request for %s", expression);
			return new DAPMessageList(request,
					new JSONObject("result", "not available", "variablesReference", 0));
		}

		Command command = Command.parse(expression);
	
		if (command.notWhenRunning() && AsyncExecutor.currentlyRunning() != null)
		{
			DAPMessageList responses = new DAPMessageList(request,
					new JSONObject("result", "Still running " + AsyncExecutor.currentlyRunning(), "variablesReference", 0));
			return responses;
		}

		if (command instanceof PrintCommand || command instanceof ScriptCommand)	// ie. evaluate something
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
		List<SchedulableThread> threads = new Vector<SchedulableThread>(SchedulableThread.getAllThreads());
		Collections.sort(threads);
		JSONArray list = new JSONArray();
		
		for (SchedulableThread thread: threads)
		{
			if (!thread.getName().startsWith("BusThread-"))		// Don't include busses
			{
				list.add(new JSONObject(
					"id",	thread.getId(),
					"name", thread.getName()));
			}
		}
		
		return new DAPMessageList(request, new JSONObject("threads", list));
	}

	/**
	 * Termination and cleanup methods.
	 */
	public DAPMessageList disconnect(DAPRequest request, Boolean terminateDebuggee)
	{
		try
		{
			RTLogger.dump(true);
			
			if (RTLogger.isEnabled())
			{
				Diag.info("Validating RT logs");
				int errs = RTValidator.validate(RTLogger.getLogfileName());
				
				if (errs == 0)
				{
					stdout("No conjecture validation errors found");
				}
				else
				{
					stderr("Found " + errs + " conjecture failures");
				}
				
				RTLogger.enable(false);
			}
		}
		catch (IOException e)
		{
			Diag.error("Problem saving/validating RT logs");
			Diag.error(e);
		}
		
		stdout("\nSession disconnected.\n");
		SchedulableThread.terminateAll();
		clearInterpreter();
		restoreSettings();
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
		restoreSettings();
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
			
			Diag.info("Cleared interpreter");
			interpreter = null;
		}
	}
	
	public boolean refreshInterpreter()
	{
		if (hasChanged())
		{
			Diag.info("Specification has changed, resetting interpreter");
			interpreter = null;
			return true;
		}
		
		return false;
	}
	
	public boolean specHasErrors()
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
	
	public void setNoDebug(boolean noDebug)
	{
		this.noDebug = noDebug;
	}
	
	public void stopDebugReader()
	{
		/**
		 * The debugReader field can be cleared at any time, when the debugger ends.
		 * So we take the initial value here.
		 */
		DAPDebugReader reader = debugReader;
		
		if (reader != null)
		{
			int retries = 5;
			
			while (retries-- > 0 && !reader.isListening())
			{
				pause(200);		// Wait for reader to stop & listen
			}
			
			if (retries > 0)
			{
				reader.interrupt();	// Cause exchange to trip & kill threads
				retries = 5;
				
				while (retries-- > 0 && getDebugReader() != null)
				{
					pause(200);
				}
				
				if (retries == 0)
				{
					Diag.error("DAPDebugReader interrupt did not work?");
				}
			}
			else
			{
				Diag.error("DAPDebugReader is not listening?");
			}
		}
	}
	
	private void pause(long ms)
	{
		try
		{
			Thread.sleep(ms);
		}
		catch (InterruptedException e)
		{
			// ignore
		}
	}
}
