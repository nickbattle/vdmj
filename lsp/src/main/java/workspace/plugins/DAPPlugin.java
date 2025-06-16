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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package workspace.plugins;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
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

import dap.DAPInitializeResponse;
import dap.DAPMessageList;
import dap.DAPRequest;
import dap.DAPResponse;
import dap.DAPServer;
import dap.InitExecutor;
import dap.RemoteControlExecutor;
import dap.UnknownHandler;
import dap.handlers.DebuggingHandler;
import dap.handlers.DisconnectHandler;
import dap.handlers.EvaluateHandler;
import dap.handlers.InitializeHandler;
import dap.handlers.LaunchHandler;
import dap.handlers.PauseHandler;
import dap.handlers.SetBreakpointsHandler;
import dap.handlers.SourceHandler;
import dap.handlers.StackTraceHandler;
import dap.handlers.TerminateHandler;
import dap.handlers.ThreadsHandler;
import json.JSONArray;
import json.JSONObject;
import lsp.CancellableThread;
import lsp.Utils;
import vdmj.DAPDebugReader;
import vdmj.commands.AnalysisCommand;
import workspace.Diag;
import workspace.EventListener;
import workspace.PluginRegistry;
import workspace.events.DAPBeforeEvaluateEvent;
import workspace.events.DAPConfigDoneEvent;
import workspace.events.DAPDisconnectEvent;
import workspace.events.DAPEvaluateEvent;
import workspace.events.DAPInitializeEvent;
import workspace.events.DAPLaunchEvent;
import workspace.events.DAPTerminateEvent;
import workspace.events.UnknownCommandEvent;

public class DAPPlugin extends AnalysisPlugin
{
	private static DAPPlugin INSTANCE = null;
	
	private JSONObject clientCapabilities;
	private Boolean noDebug;
	private Interpreter interpreter;
	private String launchCommand;
	private JSONObject launchParams;
	private String defaultName;
	private DAPDebugReader debugReader;
	private String remoteControl;
	private String logging;
	
	/**
	 * These are the only property names that can sensibly set via the DAP launch.
	 */
	private static List<String> propertyNames = Arrays.asList(
		"vdmj.scheduler.fcfs_timeslice",
		"vdmj.scheduler.virtual_timeslice",
		"vdmj.scheduler.jitter",
		"vdmj.rt.duration_default",
		"vdmj.rt.duration_transactions",
		"vdmj.rt.log_instvarchanges",
		"vdmj.rt.max_periodic_overlaps",
		"vdmj.rt.diags_guards",
		"vdmj.rt.diags_timestep",
		"vdmj.in.powerset_limit",
		"vdmj.in.typebind_limit"
	);
	
	protected DAPPlugin()
	{
		Diag.info("DAPPlugin created");
	}
	
	@Override
	public int getPriority()
	{
		return EventListener.DAP_PRIORITY;
	}

	@Override
	public String getName()
	{
		return "DAP";
	}

	public static synchronized DAPPlugin getInstance()
	{
		if (INSTANCE == null)
		{
			INSTANCE = new DAPPlugin();
			
			PluginRegistry registry = PluginRegistry.getInstance();
			registry.registerPlugin(INSTANCE);
		}
		
		return INSTANCE;
	}
	
	@Override
	public void init()
	{
		dapDispatcher.register(new InitializeHandler(), "initialize");
		dapDispatcher.register(new LaunchHandler(), "launch");
		dapDispatcher.register(new InitializeHandler(), "configurationDone");
		dapDispatcher.register(new ThreadsHandler(), "threads");
		dapDispatcher.register(new SetBreakpointsHandler(), "setBreakpoints", "setExceptionBreakpoints", "setFunctionBreakpoints");
		dapDispatcher.register(new EvaluateHandler(), "evaluate");
		dapDispatcher.register(new StackTraceHandler(), "stackTrace");
		dapDispatcher.register(new DisconnectHandler(), "disconnect");
		dapDispatcher.register(new TerminateHandler(), "terminate");
		dapDispatcher.register(new PauseHandler(), "pause");
		dapDispatcher.register(new SourceHandler(), "source");
		
		dapDispatcher.register(new DebuggingHandler(),
			"continue", "stepIn", "stepOut", "next", "scopes", "variables");

		dapDispatcher.register(new UnknownHandler());
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
		responses.addAll(eventhub.publish(new DAPInitializeEvent(request)));
		return responses;
	}

	public DAPMessageList dapLaunch(DAPRequest request,
			boolean noDebug, String defaultName, String command, String remoteControl, String logging, JSONObject params) throws Exception
	{
		LSPPlugin manager = LSPPlugin.getInstance();

		if (manager.checkInProgress())
		{
			stderr("Specification being checked, cannot launch");
			return new DAPMessageList(request, false, "Specification being checked, cannot launch", null);
		}
		
		if (specHasErrors())
		{
			stderr("Specification has errors, cannot launch");
			return new DAPMessageList(request, false, "Specification has errors, cannot launch", null);
		}
		
		try
		{
			// These values are used in configurationDone
			this.noDebug = noDebug;
			this.launchCommand = command;
			this.launchParams = params;
			this.defaultName = defaultName;
			this.remoteControl = remoteControl;
			this.logging = logging;
			
			clearInterpreter();
			processSettings(request);

			eventhub.publish(new DAPLaunchEvent(request));

			return new DAPMessageList(request);
		}
		catch (Exception e)
		{
			Diag.error(e);
			return new DAPMessageList(request, e);
		}
	}

	/**
	 * Pick out request arguments that are VDMJ Settings and properties.
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
				if (propertyNames.contains(key))
				{
					if (properties.get(key) == null)
					{
						System.clearProperty(key);
					}
					else
					{
						String value = properties.get(key).toString().trim();
						System.setProperty(key, value);
					}
				}
				else
				{
					Diag.warning("Ignoring property %s", key);
				}
			}
			
			// System properties above override those from any properties file
			Diag.info("Reading properties from %s", LSPPlugin.PROPERTIES);
			Properties.init(LSPPlugin.PROPERTIES);
		}
	}
	
	/**
	 * This puts the Settings and VDMJ properties back to the default for the project.
	 * It undoes changes from processSettings above.
	 */
	private void restoreSettings()
	{
		Diag.info("Resetting to default settings");
		Settings.dynamictypechecks = true;
		Settings.invchecks = true;
		Settings.prechecks = true;
		Settings.postchecks = true;
		Settings.measureChecks = true;
		Settings.exceptions = false;
		
		// Clear any System property overrides...
		for (String key: propertyNames)
		{
			System.clearProperty(key);
		}
		
		// Reset properties from the file
		Diag.info("Resetting properties from %s", LSPPlugin.PROPERTIES);
		Properties.init(LSPPlugin.PROPERTIES);
	}

	public DAPMessageList dapConfigurationDone(DAPRequest request)
	{
		try
		{
			if (Settings.dialect == Dialect.VDM_RT && logging != null)
			{
				File file = new File(logging);
				RTLogger.setLogfileName(file);
				Properties.rt_log_instvarchanges = true;
				Diag.info("RT events now logged to %s", file.getAbsolutePath());
			}
			
			eventhub.publish(new DAPConfigDoneEvent(request));
			
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

	public DAPMessageList unhandledCommand(DAPRequest request)
	{
		DAPMessageList responses = eventhub.publish(new UnknownCommandEvent(request));
		
		if (responses.isEmpty())
		{
			Diag.error("No external plugin registered for unknownMethodEvent (%s)", request.getCommand());
			return new DAPMessageList(request, false, "Unknown DAP command: " + request.getCommand(), null);
		}
		else
		{
			return responses;
		}
	}

	/**
	 * The interpreter has changed if there is an interpreter, and the IN tree
	 * within that interpreter is not the same as the IN plugin's tree.
	 */
	private boolean hasChanged()
	{
		INPlugin in = registry.getPlugin("IN");
		return interpreter != null && interpreter.getIN() != in.getIN();
	}
	
	/**
	 * The AST is dirty if an edit has been made that has not been saved and
	 * type checked.
	 */
	private boolean isDirty()
	{
		ASTPlugin ast = registry.getPlugin("AST");
		return ast.isDirty();
	}

	/**
	 * Write messages directly to the console, on stdout or stderr.
	 */
	private void stdout(String message)
	{
		DAPServer.getInstance().stdout(message);
	}
	
	private void stderr(String message)
	{
		DAPServer.getInstance().stderr(message);
	}
	
	public DAPMessageList dapSetBreakpoints(DAPRequest request, File file, JSONArray breakpoints) throws Exception
	{
		JSONArray results = new JSONArray();
		getInterpreter();
		
		Map<Integer, Breakpoint> existing = interpreter.getBreakpoints();
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
	
	public DAPMessageList dapSetFunctionBreakpoints(DAPRequest request, JSONArray breakpoints) throws Exception
	{
		JSONArray results = new JSONArray();
		getInterpreter();
		
		Map<Integer, Breakpoint> existing = interpreter.getBreakpoints();
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

	public DAPMessageList dapSetExceptionBreakpoints(DAPRequest request, JSONArray filterOptions)
	{
		getInterpreter();
		
		for (Catchpoint cp: interpreter.getCatchpoints())
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
					String condition = "";
					
					try
					{
						condition = filterOption.get("condition");
						interpreter.setCatchpoint(condition);
						results.add(new JSONObject("verified", true));
					}
					catch (Exception e)
					{
						String error = "Illegal exception condition '" + condition + "': "+ e.getMessage(); 
						Diag.error(error);
						results.add(new JSONObject("verified", false, "message", error));
						stderr(error);
					}
				}
				else
				{
					String error = "Unknown filterOption Id " + filterOption.get("filterId");
					Diag.error(error);
					results.add(new JSONObject("verified", false, "message", error));
					stderr(error);
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
	
	public DAPMessageList dapEvaluate(DAPRequest request, String expression, String context)
	{
		// This happens when watches are set, but there is no execution session open.
		
		if ("watch".equals(context))	// watch received outside execution
		{
			Diag.info("Ignoring watch request for %s", expression);
			return new DAPMessageList(request,
					new JSONObject("result", "not available", "variablesReference", 0));
		}

		// An event here allows plugins to return failure DAPResponse(s) with a "message"
		// reason not to evaluate. For example, CTPlugin sends this if the trace is still
		// running.
		
		DAPMessageList prechecks = eventhub.publish(new DAPBeforeEvaluateEvent(request));
		
		for (JSONObject response: prechecks)
		{
			if (response instanceof DAPResponse)
			{
				DAPResponse dap = (DAPResponse)response;
				boolean success = dap.get("success");
				
				if (!success)	// First failure stops the execution
				{
					String reason = dap.get("message");
					DAPMessageList responses = new DAPMessageList(request, false, "Cannot evaluate expression: " + reason, null);
					DAPServer.getInstance().setRunning(false);
					clearInterpreter();
					return responses;
				}
			}
		}
		
		AnalysisCommand command = AnalysisCommand.parse(expression);
	
		if (command.notWhenRunning() && CancellableThread.currentlyRunning() != null)
		{
			return new DAPMessageList(request, false, "Still running " + CancellableThread.currentlyRunning(), null);
		}

		// If we are about to evaluate something, check that we can execute.
		
		if (command.notWhenDirty())
		{
			if (specHasErrors())
			{
				clearInterpreter();
				
				return new DAPMessageList(request, false, "Cannot start interpreter: specification has errors?", null);
			}
			else if (hasChanged())
			{
				return new DAPMessageList(request, false, "Specification has changed: try restart", null);
			}
			else if (isDirty())
			{
				stderr("WARNING: specification has unsaved changes");
			}
		}
		
		eventhub.publish(new DAPEvaluateEvent(request));

		return command.run(request);
	}

	public DAPMessageList dapThreads(DAPRequest request)
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
	public DAPMessageList dapDisconnect(DAPRequest request, Boolean terminateDebuggee)
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
		eventhub.publish(new DAPDisconnectEvent(request));

		DAPMessageList result = new DAPMessageList(request);
		return result;
	}

	public DAPMessageList dapTerminate(DAPRequest request, Boolean restart)
	{
		DAPMessageList result = new DAPMessageList(request);
		RTLogger.dump(true);

		if (restart && !specHasErrors())
		{
			stdout("\nSession restarting...\n");
			LSPPlugin lsp = LSPPlugin.getInstance();
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
		
		SchedulableThread.terminateAll();
		clearInterpreter();
		restoreSettings();
		eventhub.publish(new DAPTerminateEvent(request));

		return result;
	}
	
	/**
	 * Create a new (dialect) Interpreter from the IN tree, or return the
	 * current interpreter.
	 */
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

	/**
	 * Clear the interpreter value and remove all breakpoints from the IN tree.
	 * A new Interpreter will be made on the next call to getInterpreter().
	 */
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
	
	/**
	 * If the IN plugin has a new tree, clear the interpreter so that a new one
	 * can be created (above).
	 */
	public boolean refreshInterpreter()
	{
		if (hasChanged())
		{
			Diag.info("Specification has changed, clearing interpreter");
			clearInterpreter();
			return true;
		}
		
		return false;
	}
	
	/**
	 * Check whether there are syntax or type checking errors in the spec.
	 */
	public boolean specHasErrors()
	{
		return messagehub.hasErrors();
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
	
	public JSONObject getLaunchParams()
	{
		return launchParams;
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
