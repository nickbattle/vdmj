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
import java.io.PrintStream;
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
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.Value;

import dap.DAPEvent;
import dap.DAPMessageList;
import dap.DAPRequest;
import dap.DAPResponse;
import dap.DAPServer;
import dap.handlers.DAPInitializeResponse;
import json.JSONArray;
import json.JSONObject;
import lsp.LSPInitializeResponse;
import lsp.Utils;
import lsp.textdocument.SymbolKind;
import lsp.textdocument.WatchKind;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import rpc.RPCResponse;
import vdmj.DAPDebugReader;

public abstract class WorkspaceManager
{
	private static WorkspaceManager INSTANCE = null;

	private File rootUri = null;
	protected Map<File, StringBuilder> projectFiles = new HashMap<File, StringBuilder>();
	protected Set<File> openFiles = new HashSet<File>();
	
	private Boolean noDebug;
	protected Interpreter interpreter;
	
	public static WorkspaceManager getInstance(Dialect dialect) throws IOException
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

	public RPCMessageList lspInitialize(RPCRequest request)
	{
		try
		{
			JSONObject params = request.get("params");
			rootUri = Utils.uriToFile(params.get("rootUri"));
			openFiles.clear();
			Properties.init();
			loadProjectFiles(rootUri);
			
			RPCMessageList responses = new RPCMessageList();
			responses.add(new RPCResponse(request, new LSPInitializeResponse()));
			return responses;
		}
		catch (URISyntaxException e)
		{
			return new RPCMessageList(request, "Malformed URI");
		}
		catch (Exception e)
		{
			return new RPCMessageList(request, e.getMessage());
		}
	}

	public RPCMessageList lspInitialized(RPCRequest request)
	{
		try
		{
			RPCMessageList response = new RPCMessageList();
			response.add(lspDynamicRegistrations());
			response.addAll(checkLoadedFiles());
			return response;
		}
		catch (Exception e)
		{
			return new RPCMessageList(request, e.getMessage());
		}
	}

	private RPCRequest lspDynamicRegistrations()
	{
		JSONArray watchers = new JSONArray();
		
		for (String glob: getFilenameFilters())
		{
			watchers.add(new JSONObject("globPattern", glob));
		}
		
		return new RPCRequest(-1L, "client/registerCapability",
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

	public DAPMessageList launch(DAPRequest request, boolean noDebug, String defaultName)
	{
		if (!canExecute())
		{
			DAPMessageList responses = new DAPMessageList(request);
			responses.add(text("Cannot start interpreter: errors exist?"));
			return responses;
		}
		
		try
		{
			this.noDebug = noDebug;
			
			long before = System.currentTimeMillis();
			getInterpreter().init();
			if (defaultName != null) getInterpreter().setDefaultName(defaultName);
			long after = System.currentTimeMillis();
			
			DAPMessageList responses = new DAPMessageList(request);
			responses.add(heading());
			responses.add(text("Initialized in " + (double)(after-before)/1000 + " secs.\n"));
			prompt(responses);
			return responses;
		}
		catch (Exception e)
		{
			DAPMessageList responses = new DAPMessageList(request, e);
			prompt(responses);
			return responses;
		}
	}
	
	protected abstract boolean canExecute();

	protected DAPResponse heading() throws Exception
	{
		return text("*\n" +
				"* VDMJ " + Settings.dialect + " Interpreter\n" +
				(noDebug ? "" : "* DEBUG enabled\n") +
				"*\n\nDefault " + (Settings.dialect == Dialect.VDM_SL ? "module" : "class") +
				" is " + getInterpreter().getDefaultName() + "\n");
	}
	
	protected void prompt(DAPMessageList list)
	{
		if (System.getProperty("lsp.prompts") != null)
		{
			list.add(text(interpreter.getDefaultName() + "> "));
		}
	}
	
	protected DAPResponse text(String message)
	{
		return new DAPResponse("output", new JSONObject("output", message));
	}
	
	public DAPMessageList configurationDone(DAPRequest request) throws IOException
	{
		try
		{
			return new DAPMessageList(request);
		}
		catch (Exception e)
		{
			return new DAPMessageList(request, e);
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
			File file = message.location.file.getCanonicalFile();
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
			buffer.replace(start, end, text);
			
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
				// System.out.printf("EDITED %d: [%s]\n", line+1, buffer.substring(start, end));
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

	public DAPMessageList setBreakpoints(DAPRequest request, File file, JSONArray lines) throws Exception
	{
		JSONArray results = new JSONArray();
		
		Map<Integer, Breakpoint> breakpoints = interpreter.getBreakpoints();
		
		for (Integer bpno: breakpoints.keySet())
		{
			Breakpoint bp = breakpoints.get(bpno);
			
			if (bp.location.file.equals(file))
			{
				interpreter.clearBreakpoint(bpno);
			}
		}
		
		for (Object object: lines)
		{
			int line = ((Long)object).intValue();

			if (!noDebug)	// debugging allowed!
			{
				INStatement stmt = interpreter.findStatement(file, line);
				
				if (stmt == null)
				{
					INExpression exp = interpreter.findExpression(file, line);
		
					if (exp == null)
					{
						results.add(new JSONObject("verified", false));
					}
					else
					{
						interpreter.clearBreakpoint(exp.breakpoint.number);
						interpreter.setBreakpoint(exp, null);
						results.add(new JSONObject("verified", true));
					}
				}
				else
				{
					interpreter.clearBreakpoint(stmt.breakpoint.number);
					interpreter.setBreakpoint(stmt, null);
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
	
	/**
	 * Abstract methods that are implemented in language specific subclasses.
	 */
	abstract protected FilenameFilter getFilenameFilter();

	abstract protected String[] getFilenameFilters();

	abstract protected List<VDMMessage> parseFile(File file);

	abstract protected RPCMessageList checkLoadedFiles() throws Exception;

	abstract public RPCMessageList findDefinition(RPCRequest request, File file, int line, int col) throws IOException;

	abstract public RPCMessageList documentSymbols(RPCRequest request, File file);

	abstract public DAPMessageList threads(DAPRequest request);

	abstract public Interpreter getInterpreter() throws Exception;

	public DAPMessageList disconnect(DAPRequest request, boolean terminateDebuggee)
	{
		return new DAPMessageList(request);
	}

	public DAPMessageList evaluate(DAPRequest request, String expression, String context)
	{
		DAPDebugReader dbg = null;
		PrintStream stdout = System.out;
		PrintStream stderr = System.err;
		
		try
		{
			dbg = new DAPDebugReader();
			dbg.start();
			
			long before = System.currentTimeMillis();
			System.setOut(DAPServer.getInstance().getOutPrintStream());
			System.setErr(DAPServer.getInstance().getErrPrintStream());
			Value result = getInterpreter().execute(expression);
			long after = System.currentTimeMillis();
			
			String answer = "= " + result;
			DAPMessageList responses = new DAPMessageList(request,
					new JSONObject("result", answer, "variablesReference", 0));
			responses.add(text("Executed in " + (double)(after-before)/1000 + " secs.\n"));
			prompt(responses);
			return responses;
		}
		catch (Exception e)
		{
			DAPMessageList responses = new DAPMessageList(request, e);
			prompt(responses);
			return responses;
		}
		finally
		{
			System.setOut(stdout);
			System.setErr(stderr);
			
			if (dbg != null)
			{
				dbg.interrupt();	// Stop the debugger reader.
			}
		}
	}

	public DAPMessageList terminate(DAPRequest request, Boolean restart)
	{
		if (interpreter != null)
		{
			// Clear the BPs since they are embedded in the tree and the next
			// launch may have noDebug set.
			
			Set<Integer> bps = new HashSet<Integer>();
			bps.addAll(interpreter.getBreakpoints().keySet());
			
			for (Integer bpno: bps)
			{
				interpreter.clearBreakpoint(bpno);
			}
			
			interpreter = null;
		}

		DAPMessageList result = new DAPMessageList(request);
		result.add(text("\nSession terminated.\n"));
		return result;
	}
}
