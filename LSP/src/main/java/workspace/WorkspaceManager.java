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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.Value;

import dap.DAPMessageList;
import dap.DAPRequest;
import dap.DAPResponse;
import dap.handlers.InitializeResponse;
import json.JSONArray;
import json.JSONObject;
import lsp.Utils;
import lsp.textdocument.SymbolKind;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import vdmj.DAPDebugReader;

public abstract class WorkspaceManager
{
	private static WorkspaceManager INSTANCE = null;
	private URI rootUri = null;
	protected Map<URI, StringBuilder> projectFiles = new HashMap<URI, StringBuilder>();
	protected Set<URI> openFiles = new HashSet<URI>();
	
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
				
			default:
				throw new IOException("Unsupported dialect: " + dialect);
		}
	}

	public RPCMessageList initialize(RPCRequest request)
	{
		try
		{
			JSONObject params = request.get("params");
			rootUri = new URI(params.get("rootUri"));
			openFiles.clear();
			Properties.init();
			loadProjectFiles(new File(rootUri));
			return new RPCMessageList();
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

	public RPCMessageList initialized(RPCRequest request)
	{
		try
		{
			return checkLoadedFiles();
		}
		catch (Exception e)
		{
			return new RPCMessageList(request, e.getMessage());
		}
	}

	public DAPMessageList initialize(DAPRequest request)
	{
		return new DAPMessageList(new InitializeResponse(request));
	}

	public DAPMessageList launch(DAPRequest request, boolean noDebug, String defaultName)
	{
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
			responses.add(prompt());
			return responses;
		}
		catch (Exception e)
		{
			DAPMessageList responses = new DAPMessageList(request, e);
			responses.add(prompt());
			return responses;
		}
	}
	
	private DAPResponse heading()
	{
		return text("*\n" +
				"* VDMJ " + Settings.dialect + " Interpreter\n" +
				"*\n\n");
	}
	
	protected DAPResponse prompt()
	{
		return text(interpreter.getDefaultName() + "> ");
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

	public abstract Interpreter getInterpreter() throws Exception;

	
	
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

	protected abstract FilenameFilter getFilenameFilter();

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
		projectFiles.put(Utils.fileToURI(file), sb);
	}
	
	protected abstract List<VDMMessage> parseURI(URI uri);

	protected abstract RPCMessageList checkLoadedFiles() throws Exception;

	protected RPCMessageList diagnosticResponses(List<? extends VDMMessage> list, URI oneURI)
	{
		Map<URI, List<VDMMessage>> map = new HashMap<URI, List<VDMMessage>>();
		
		for (VDMMessage message: list)
		{
			URI uri = Utils.fileToURI(message.location.file);
			List<VDMMessage> set = map.get(uri);
			
			if (set == null)
			{
				set = new Vector<VDMMessage>();
				set.add(message);
				map.put(uri, set);
			}
			else
			{
				set.add(message);
			}
		}
		
		RPCMessageList responses = new RPCMessageList();
		
		// Only publish diags for a subset of the URIs - usually the one being edited, only.
		// Defaults to all project files, if none specified.
		Set<URI> urisToReport = new HashSet<URI>();
		
		if (oneURI == null)
		{
			urisToReport.addAll(projectFiles.keySet());
		}
		else
		{
			urisToReport.add(oneURI);
		}
		
		for (URI uri: urisToReport)
		{
			JSONArray messages = new JSONArray();
			
			if (map.containsKey(uri))
			{
				for (VDMMessage message: map.get(uri))
				{
					messages.add(
						new JSONObject(
							"range",	Utils.lexLocationToRange(message.location),
							"severity", (message instanceof VDMError ? 1 : 2),
							"code", 	message.number,
							"message",	message.toProblemString().replaceAll("\n", ", ")));
					
				}
			}
			
			JSONObject params = new JSONObject("uri", uri.toString(), "diagnostics", messages);
			responses.add(new RPCRequest("textDocument/publishDiagnostics", params));
		}
		
		return responses;
	}
	
	public RPCMessageList openFile(RPCRequest request, URI uri, String text)
	{
		if (!projectFiles.keySet().contains(uri))
		{
			return new RPCMessageList(request, "File not known");
		}
		else if (openFiles.contains(uri))
		{
			return new RPCMessageList(request, "File already open");
		}
		else
		{
			openFiles.add(uri);
			return null;
		}
	}
	
	public RPCMessageList closeFile(RPCRequest request, URI uri)
	{
		if (!projectFiles.keySet().contains(uri))
		{
			return new RPCMessageList(request, "File not known");
		}
		else if (!openFiles.contains(uri))
		{
			return new RPCMessageList(request, "File not open");
		}
		else
		{
			openFiles.remove(uri);
			return null;
		}
	}

	public RPCMessageList changeFile(RPCRequest request, URI uri, JSONObject range, String text) throws Exception
	{
		if (!projectFiles.keySet().contains(uri))
		{
			return new RPCMessageList(request, "File not known");
		}
		else if (!openFiles.contains(uri))
		{
			return new RPCMessageList(request, "File not open");
		}
		else
		{
			StringBuilder buffer = projectFiles.get(uri);
			int start = Utils.findPosition(buffer, range.get("start"));
			int end   = Utils.findPosition(buffer, range.get("end"));
			buffer.replace(start, end, text);
			return diagnosticResponses(parseURI(uri), uri);
		}
	}

	public RPCMessageList saveFile(RPCRequest request, URI uri, String text) throws Exception
	{
		if (!projectFiles.keySet().contains(uri))
		{
			return new RPCMessageList(request, "File not known");
		}
		else if (!openFiles.contains(uri))
		{
			return new RPCMessageList(request, "File not open");
		}
		else
		{
			StringBuilder buffer = projectFiles.get(uri);
			
			if (!text.trim().equals(buffer.toString().trim()))
			{
				Log.error("Files different on save?");
				projectFiles.put(uri, new StringBuilder(text));
			}
			
			return checkLoadedFiles();		// typecheck on save
		}
	}

	abstract public RPCMessageList findDefinition(RPCRequest request, URI uri, int line, int col) throws IOException;

	abstract public RPCMessageList documentSymbols(RPCRequest request, URI uri);

	private JSONObject symbolInformation(String name, LexLocation location, SymbolKind kind, String container)
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

	protected JSONObject symbolInformation(TCIdentifierToken name, SymbolKind kind, String container)
	{
		return symbolInformation(name.getName(), name.getLocation(), kind, container);
	}
	
	protected JSONObject symbolInformation(TCNameToken name, TCType type, SymbolKind kind, String container)
	{
		return symbolInformation(name.getName() + ":" + type, name.getLocation(), kind, container);
	}

	public DAPMessageList setBreakpoints(DAPRequest request, URI uri, JSONArray lines) throws Exception
	{
		File file = new File(uri);
		JSONArray results = new JSONArray();
		
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

	public abstract DAPMessageList threads(DAPRequest request);

	public DAPMessageList disconnect(DAPRequest request, boolean terminateDebuggee)
	{
		return new DAPMessageList(request);
	}

	public DAPMessageList evaluate(DAPRequest request, String expression, String context)
	{
		DAPDebugReader dbg = null;
		
		try
		{
			dbg = new DAPDebugReader();
			dbg.start();
			
			long before = System.currentTimeMillis();
			Value result = getInterpreter().execute(expression);
			long after = System.currentTimeMillis();
			
			String answer = "= " + result;
			DAPMessageList responses = new DAPMessageList(request, new JSONObject("result", answer, "variablesReference", 0));
			responses.add(text("Executed in " + (double)(after-before)/1000 + " secs.\n"));
			responses.add(prompt());
			return responses;
		}
		catch (Exception e)
		{
			DAPMessageList responses = new DAPMessageList(request, e);
			responses.add(prompt());
			return responses;
		}
		finally
		{
			if (dbg != null)
			{
				dbg.interrupt();	// Stop the debugger reader.
			}
		}
	}

	public DAPMessageList terminate(DAPRequest request, Boolean restart)
	{
		return new DAPMessageList(request);
	}
}
