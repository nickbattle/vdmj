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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;

import dap.DAPEvent;
import dap.DAPMessageList;
import dap.DAPRequest;
import dap.DAPResponse;
import dap.DAPServer;
import dap.DAPServerState;
import dap.handlers.DAPInitializeResponse;
import json.JSONArray;
import json.JSONObject;
import lsp.LSPInitializeResponse;
import lsp.LSPServerState;
import lsp.Utils;
import lsp.textdocument.CompletionItemKind;
import lsp.textdocument.SymbolKind;
import lsp.textdocument.WatchKind;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import rpc.RPCResponse;
import vdmj.DAPDebugReader;
import vdmj.commands.Command;
import vdmj.commands.PrintCommand;

public abstract class WorkspaceManager
{
	private static WorkspaceManager INSTANCE = null;

	private JSONObject clientCapabilities;
	private List<File> roots = new Vector<File>();
	protected Map<File, StringBuilder> projectFiles = new HashMap<File, StringBuilder>();
	protected Set<File> openFiles = new HashSet<File>();
	
	private Boolean noDebug;
	protected Interpreter interpreter;

	protected LSPServerState lspServerState;
	protected DAPServerState dapServerState;

	private String launchCommand;
	private String defaultName;

	
	public static WorkspaceManager createInstance(Dialect dialect) throws IOException
	{
		switch (dialect)
		{
			case VDM_SL:
				if (INSTANCE == null)
				{
					INSTANCE = new WorkspaceManagerSL();
				}
				return INSTANCE;
				
			case VDM_PP:
				if (INSTANCE == null)
				{
					INSTANCE = new WorkspaceManagerPP();
				}
				return INSTANCE;
				
			case VDM_RT:
				if (INSTANCE == null)
				{
					INSTANCE = new WorkspaceManagerRT();
				}
				return INSTANCE;
				
			default:
				throw new IOException("Unsupported dialect: " + dialect);
		}
	}
	
	public static void reset()
	{
		INSTANCE = null;
	}
	
	public static WorkspaceManager getInstance()
	{
		return INSTANCE;
	}

	public void setLSPState(LSPServerState lspServerState)
	{
		this.lspServerState = lspServerState;
	}

	public void setDAPState(DAPServerState dapServerState)
	{
		this.dapServerState = dapServerState;
	}

	public RPCMessageList lspInitialize(RPCRequest request)
	{
		try
		{
			JSONObject params = request.get("params");
			getRoots().clear();
			getRoots().add(Utils.uriToFile(params.get("rootUri")));	// TODO workspace folders
			clientCapabilities = params.get("capabilities");
			openFiles.clear();
			System.setProperty("parser.tabstop", "1");	// Forced, for LSP location offsets
			Properties.init();
			loadAllProjectFiles();
			
			RPCMessageList responses = new RPCMessageList();
			responses.add(new RPCResponse(request, new LSPInitializeResponse()));
			return responses;
		}
		catch (URISyntaxException e)
		{
			return new RPCMessageList(request, RPCErrors.InvalidRequest, "Malformed URI");
		}
		catch (Exception e)
		{
			Log.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}

	public RPCMessageList lspInitialized(RPCRequest request)
	{
		try
		{
			RPCMessageList response = new RPCMessageList();
			response.add(lspDynamicRegistrations());
			
			if (hasClientCapability("workspace.workspaceFolders"))
			{
				response.add(lspWorkspaceFolders());
			}
			
			response.addAll(checkLoadedFiles());
			return response;
		}
		catch (Exception e)
		{
			Log.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}

	private RPCRequest lspWorkspaceFolders()
	{
		return new RPCRequest(0L, "workspace/workspaceFolders", new JSONObject());
	}

	private RPCRequest lspDynamicRegistrations()
	{
		JSONArray watchers = new JSONArray();
		
		for (String glob: getFilenameFilters())
		{
			watchers.add(new JSONObject("globPattern", glob));
		}
		
		return new RPCRequest(0L, "client/registerCapability",
			new JSONObject(
				"registrations",
					new JSONArray(
						new JSONObject(
							"id", "12345",
							"method", "workspace/didChangeWatchedFiles",
							"registerOptions",
								new JSONObject("watchers", watchers)
			))));
	}

	public DAPMessageList dapInitialize(DAPRequest request)
	{
		DAPMessageList responses = new DAPMessageList();
		responses.add(new DAPInitializeResponse(request));
		responses.add(new DAPEvent("initialized", null));
		return responses;
	}

	public DAPMessageList launch(DAPRequest request, boolean noDebug, String defaultName, String command) throws Exception
	{
		checkLoadedFiles();
		
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
			DAPMessageList responses = new DAPMessageList(request, e);
			return responses;
		}
	}

	public List<File> getRoots()
	{
		return roots;
	}
	
	public boolean hasClientCapability(String dotName)	// eg. "workspace.workspaceFolders"
	{
		Boolean cap = getClientCapability(dotName);
		return cap != null && cap;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getClientCapability(String dotName)
	{
		T capability = clientCapabilities.getPath(dotName);
		
		if (capability != null)
		{
			Log.printf("Client capability %s = %s", dotName, capability);
			return capability;
		}
		else
		{
			Log.printf("Missing client capability: %s", dotName);
			return null;
		}
	}

	/** True if we have an interpreter that we can use. */
	protected abstract boolean canExecute();
	
	/** True if the spec has been updated since the interpreter was created. */
	protected abstract boolean hasChanged();

	private void heading() throws Exception
	{
		stdout("*\n" +
				"* VDMJ " + Settings.dialect + " Interpreter\n" +
				(noDebug ? "" : "* DEBUG enabled\n") +
				"*\n\nDefault " + (Settings.dialect == Dialect.VDM_SL ? "module" : "class") +
				" is " + getInterpreter().getDefaultName() + "\n");
	}
	
	protected void stdout(String message)
	{
		DAPServer.getInstance().stdout(message);
	}
	
	protected void stderr(String message)
	{
		DAPServer.getInstance().stderr(message);
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
			return new DAPMessageList(request, e);
		}
		finally
		{
			launchCommand = null;
		}
	}

	private void loadAllProjectFiles() throws IOException
	{
		for (File root: getRoots())
		{
			loadProjectFiles(root);
		}
	}

	private void loadProjectFiles(File root) throws IOException
	{
		FilenameFilter filter = getFilenameFilter();
		File[] files = root.listFiles();
		
		for (File file: files)
		{
			if (file.isDirectory())
			{
				loadProjectFiles(file);
			}
			else
			{
				if (filter.accept(root, file.getName()))
				{
					loadFile(file);
				}
			}
		}
	}

	private void loadFile(File file) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		String line = br.readLine();
	
		while (line != null)
		{
			sb.append(line);
			sb.append('\n');
			line = br.readLine();
		}
	
		br.close();
		projectFiles.put(file, sb);
	}
	
	private void unloadFile(File file)
	{
		projectFiles.remove(file);
	}

	protected RPCMessageList diagnosticResponses(List<? extends VDMMessage> list, File oneFile) throws IOException
	{
		Map<File, List<VDMMessage>> map = new HashMap<File, List<VDMMessage>>();
		
		for (VDMMessage message: list)
		{
			File file = message.location.file.getAbsoluteFile();
			List<VDMMessage> set = map.get(file);
			
			if (set == null)
			{
				set = new Vector<VDMMessage>();
				set.add(message);
				map.put(file, set);
			}
			else
			{
				set.add(message);
			}
		}
		
		RPCMessageList responses = new RPCMessageList();
		
		// Only publish diags for a subset of the files - usually the one being edited, only.
		// Defaults to all project files, if none specified.
		Set<File> filesToReport = new HashSet<File>();
		
		if (oneFile == null)
		{
			filesToReport.addAll(projectFiles.keySet());
		}
		else
		{
			filesToReport.add(oneFile);
		}
		
		for (File file: filesToReport)
		{
			JSONArray messages = new JSONArray();
			
			if (map.containsKey(file))
			{
				for (VDMMessage message: map.get(file))
				{
					messages.add(
						new JSONObject(
							"range",	Utils.lexLocationToRange(message.location),
							"severity", (message instanceof VDMError ? 1 : 2),
							"code", 	message.number,
							"message",	message.toProblemString().replaceAll("\n", ", ")));
					
				}
			}
			
			JSONObject params = new JSONObject("uri", file.toURI().toString(), "diagnostics", messages);
			responses.add(new RPCRequest("textDocument/publishDiagnostics", params));
		}
		
		return responses;
	}
	
	public RPCMessageList openFile(RPCRequest request, File file, String text) throws Exception
	{
		if (!projectFiles.keySet().contains(file))
		{
			Log.printf("Opening new file: %s", file);
			openFiles.add(file);
		}
		else if (openFiles.contains(file))
		{
			Log.error("File already open: %s", file);
		}
		else
		{
			Log.printf("Opening new file: %s", file);
			openFiles.add(file);
		}
		
		return null;
	}
	
	public RPCMessageList closeFile(RPCRequest request, File file) throws Exception
	{
		if (!projectFiles.keySet().contains(file))
		{
			Log.error("File not known: %s", file);
		}
		else if (!openFiles.contains(file))
		{
			Log.error("File not open: %s", file);
		}
		else
		{
			Log.printf("Closing file: %s", file);
			openFiles.remove(file);
		}
		
		return null;
	}

	public RPCMessageList changeFile(RPCRequest request, File file, JSONObject range, String text) throws Exception
	{
		if (!projectFiles.keySet().contains(file))
		{
			Log.error("File not known: %s", file);
			return null;
		}
		else if (!openFiles.contains(file))
		{
			Log.error("File not open: %s", file);
			return null;
		}
		else
		{
			StringBuilder buffer = projectFiles.get(file);
			int start = Utils.findPosition(buffer, range.get("start"));
			int end   = Utils.findPosition(buffer, range.get("end"));
			
			if (start >= 0 && end >= 0)
			{
				buffer.replace(start, end, text);
			}
			
			if (Log.logging())	// dump edited line
			{
				JSONObject position = range.get("start");
				long line = position.get("line");
				long count = 0;
				start = 0;
				
				while (count < line)
				{
					if (buffer.charAt(start++) == '\n')
					{
						count++;
					}
				}
				
				end = start;
				while (end < buffer.length() && buffer.charAt(end) != '\n') end++;
				Log.printf("EDITED %d: [%s]", line+1, buffer.substring(start, end));
			}
			
			return diagnosticResponses(parseFile(file), file);
		}
	}

	public void changeWatchedFile(RPCRequest request, File file, WatchKind type) throws Exception
	{
		switch (type)
		{
			case CREATE:
				if (!projectFiles.keySet().contains(file))	
				{
					Log.printf("Created new file: %s", file);
					loadFile(file);
				}
				else
				{
					Log.error("Created file already exists: %s", file);
				}
				break;
				
			case CHANGE:
				if (!projectFiles.keySet().contains(file))	
				{
					Log.error("Changed file not loaded: %s", file);
				}
				break;
				
			case DELETE:
				if (projectFiles.keySet().contains(file))	
				{
					Log.printf("Deleted file: %s", file);
					unloadFile(file);
				}
				else
				{
					Log.error("Deleted file not known: %s", file);
				}
				break;
		}
	}

	public RPCMessageList changeFolders(RPCRequest request, List<File> newRoots)
	{
		try
		{
			roots.clear();
			projectFiles.clear();
			loadAllProjectFiles();
			return checkLoadedFiles();
		}
		catch (Exception e)
		{
			Log.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}

	public RPCMessageList afterChangeWatchedFiles(RPCRequest request) throws Exception
	{
		return checkLoadedFiles();
	}

	/**
	 * This is currently done via watched file events above.
	 */
	public RPCMessageList saveFile(RPCRequest request, File file, String text) throws Exception
	{
		if (!projectFiles.keySet().contains(file))
		{
			return new RPCMessageList(request, "File not known");
		}
		else if (!openFiles.contains(file))
		{
			return new RPCMessageList(request, "File not open");
		}
		else
		{
			String buffer = projectFiles.get(file).toString();
			
			if (!text.trim().equals(buffer.trim()))		// Trim for trailing newline
			{
				Utils.diff("File different on didSave at %d", text, buffer);
				projectFiles.put(file, new StringBuilder(text));
			}
			
			return checkLoadedFiles();		// typecheck on save
		}
	}

	public RPCMessageList completion(RPCRequest request, File file, int zline, int zcol)
	{
		JSONArray result = new JSONArray();
		TCDefinition def = findDefinition(file, zline, zcol - 2);

		if (def != null)
		{
			if (def.getType() instanceof TCRecordType)
			{
				TCRecordType rtype = (TCRecordType)def.getType();
				
				for (TCField field: rtype.fields)
				{
					result.add(new JSONObject(
						"label", field.tag,
						"kind", CompletionItemKind.kindOf(def).getValue(),
						"detail", field.type.toString()));
				}
			}
			else if (def.getType() instanceof TCClassType)
			{
				TCClassType ctype = (TCClassType)def.getType();
				
				for (TCDefinition field: ctype.classdef.definitions)
				{
					if (field.name != null)
					{
						TCType ftype = field.getType();
						
						if (ftype instanceof TCOperationType || ftype instanceof TCFunctionType)
						{
							result.add(new JSONObject(
								"label", field.name.toString(),		// Include types
								"kind", CompletionItemKind.kindOf(field).getValue(),
								"detail", ftype.toString()));
						}
						else
						{
							result.add(new JSONObject(
								"label", field.name.getName(),
								"kind", CompletionItemKind.kindOf(field).getValue(),
								"detail", ftype.toString()));
						}
					}
				}
			}
		}
		else
		{
			StringBuilder buffer = projectFiles.get(file);
			int position = Utils.findPosition(buffer, zline, zcol);
			
			if (position >= 0)
			{
				int start = position - 1;
				
				while (start >= 0 && Character.isJavaIdentifierPart(buffer.charAt(start)))
				{
					start--;
				}
				
				String word = buffer.subSequence(start + 1, position).toString();
				
				if (!word.isEmpty())
				{
					Log.printf("Trying to complete '%s'", word);
					
					for (TCDefinition defn: lookupDefinition(word))
					{
						TCType ftype = defn.getType();
						String insert = defn.name.getName();
						
						if (defn.isFunctionOrOperation())
						{
							insert = insert + ftype.toString().replaceAll("( ->| \\+>| ==>).*", ")");
						}
						
						result.add(new JSONObject(
								"label", defn.name.getName(),
								"kind", CompletionItemKind.kindOf(defn).getValue(),
								"detail", ftype.toString(),
								"insertText", insert));
					}
				}
			}
		}
		
		return new RPCMessageList(request, result);
	}

	protected JSONObject symbolInformation(String name, LexLocation location, SymbolKind kind, String container)
	{
		JSONObject sym = new JSONObject(
			"name", name,
			"kind", kind.getValue(),
			"location", Utils.lexLocationToLocation(location));
		
		if (container != null)
		{
			sym.put("container", container);
		}
		
		return sym;
	}

	protected JSONObject symbolInformation(LexIdentifierToken name, SymbolKind kind, String container)
	{
		return symbolInformation(name.name, name.location, kind, container);
	}
	
	protected JSONObject symbolInformation(LexIdentifierToken name, TCType type, SymbolKind kind, String container)
	{
		return symbolInformation(name.name + ":" + type, name.location, kind, container);
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

	/**
	 * Abstract methods that are implemented in language specific subclasses.
	 */
	abstract protected FilenameFilter getFilenameFilter();

	abstract protected String[] getFilenameFilters();

	abstract protected List<VDMMessage> parseFile(File file);

	abstract protected RPCMessageList checkLoadedFiles() throws Exception;

	abstract protected TCNode findLocation(File file, int zline, int zcol);

	abstract protected TCDefinition findDefinition(File file, int zline, int zcol);

	abstract public RPCMessageList findDefinition(RPCRequest request, File file, int zline, int zcol) throws IOException;

	protected abstract TCDefinitionList lookupDefinition(String startsWith);

	abstract public RPCMessageList documentSymbols(RPCRequest request, File file);

	abstract public DAPMessageList threads(DAPRequest request);

	abstract public Interpreter getInterpreter();

	abstract public RPCMessageList pogGenerate(RPCRequest request, File file, JSONObject range);

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
	
	protected void clearInterpreter()
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
