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
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;

import json.JSONArray;
import json.JSONObject;
import lsp.LSPInitializeResponse;
import lsp.LSPMessageUtils;
import lsp.LSPServerState;
import lsp.Utils;
import lsp.textdocument.CompletionItemKind;
import lsp.textdocument.WatchKind;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.plugins.ASTPlugin;
import workspace.plugins.INPlugin;
import workspace.plugins.POPlugin;
import workspace.plugins.TCPlugin;

public abstract class LSPWorkspaceManager
{
	private static LSPWorkspaceManager INSTANCE = null;
	protected final PluginRegistry registry;

	private JSONObject clientCapabilities;
	private List<File> roots = new Vector<File>();
	private Map<File, StringBuilder> projectFiles = new HashMap<File, StringBuilder>();
	private Set<File> openFiles = new HashSet<File>();
	
	// private LSPServerState lspServerState;
	
	protected LSPWorkspaceManager()
	{
		registry = PluginRegistry.getInstance();
	}

	public static synchronized LSPWorkspaceManager getInstance()
	{
		switch (Settings.dialect)
		{
			case VDM_SL:
				if (INSTANCE == null)
				{
					INSTANCE = new LSPWorkspaceManagerSL();
				}
				return INSTANCE;
				
			case VDM_PP:
				if (INSTANCE == null)
				{
					INSTANCE = new LSPWorkspaceManagerPP();
				}
				return INSTANCE;
				
			case VDM_RT:
				if (INSTANCE == null)
				{
					INSTANCE = new LSPWorkspaceManagerRT();
				}
				return INSTANCE;
				
			default:
				throw new RuntimeException("Unsupported dialect: " + Settings.dialect);
		}
	}
	
	/**
	 * This is only used by unit testing.
	 */
	public static void reset()
	{
		PluginRegistry.reset();
		INSTANCE = null;
	}
	
	public List<File> getRoots()
	{
		return roots;
	}

	public Map<File, StringBuilder> getProjectFiles()
	{
		return projectFiles;
	}
	
	public void setLSPState(LSPServerState lspServerState)
	{
		// this.lspServerState = lspServerState;	-- Not used?
	}

	/**
	 * LSP methods...
	 */
	
	public RPCMessageList lspInitialize(RPCRequest request)
	{
		try
		{
			JSONObject params = request.get("params");
			JSONArray folders = params.get("workspaceFolders");
			roots.clear();
			
			if (folders != null)
			{
				for (int i=0; i<folders.size(); i++)
				{
					JSONObject folder = folders.index(i);
					roots.add(Utils.uriToFile(folder.get("uri")));
					Log.printf("Adding workspace folder %s", (String)folder.get("uri"));
				}
			}
			else
			{
				roots.add(Utils.uriToFile(params.get("rootUri")));
			}
			
			clientCapabilities = params.get("capabilities");
			openFiles.clear();
			System.setProperty("parser.tabstop", "1");	// Forced, for LSP location offsets
			Properties.init();
			loadAllProjectFiles();
			
			return new RPCMessageList(request, new LSPInitializeResponse());
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

	public RPCMessageList checkLoadedFiles() throws Exception
	{
		ASTPlugin ast = registry.getPlugin("AST");
		TCPlugin tc = registry.getPlugin("TC");
		INPlugin in = registry.getPlugin("IN");
		
		ast.preCheck();
		tc.preCheck();
		in.preCheck();
		
		if (ast.checkLoadedFiles())
		{
			if (tc.checkLoadedFiles(ast.getAST()))
			{
				if (in.checkLoadedFiles(tc.getTC()))
				{
					Log.printf("Loaded files checked successfully");
				}
				else
				{
					Log.error("Failed to create interpreter");
				}
			}
			else
			{
				Log.error("Type checking errors found");
				Log.dump(tc.getErrs());
				Log.dump(tc.getWarns());
			}
		}
		else
		{
			Log.error("Syntax errors found");
			Log.dump(ast.getErrs());
			Log.dump(ast.getWarns());
		}
		
		List<VDMMessage> diags = new Vector<VDMMessage>();
		diags.addAll(ast.getErrs());
		diags.addAll(tc.getErrs());
		diags.addAll(ast.getWarns());
		diags.addAll(tc.getWarns());
		LSPMessageUtils utils = new LSPMessageUtils();
		RPCMessageList result = utils.diagnosticResponses(diags, projectFiles.keySet());
		
		if (hasClientCapability("experimental.proofObligationGeneration"))
		{
			POPlugin po = registry.getPlugin("PO");
			po.preCheck();
	
			result.add(new RPCRequest("lspx/POG/updated",
					new JSONObject("successful", tc.getErrs().isEmpty())));
		}
		
		return result;
	}

	private void unloadFile(File file)
	{
		projectFiles.remove(file);
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
			
			Log.dumpEdit(range, buffer);
			ASTPlugin ast = registry.getPlugin("AST");
			return ast.fileChanged(file);
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

	public RPCMessageList documentSymbols(RPCRequest request, File file) throws Exception
	{
		TCPlugin tc = registry.getPlugin("TC");
		RPCMessageList symbols = tc.documentSymbols(request, file);
		
		if (symbols == null)
		{
			ASTPlugin ast = registry.getPlugin("AST");
			symbols = ast.documentSymbols(request, file);
		}
		
		return symbols;
	}

	public RPCMessageList findDefinition(RPCRequest request, File file, int zline, int zcol)
	{
		TCDefinition def = findDefinition(file, zline, zcol);
		
		if (def == null)
		{
			return new RPCMessageList(request, null);
		}
		else
		{
			URI defuri = def.location.file.toURI();
			
			return new RPCMessageList(request,
				System.getProperty("lsp.lsp4e") != null ?
					new JSONArray(
						new JSONObject(
							"targetUri", defuri.toString(),
							"targetRange", Utils.lexLocationToRange(def.location),
							"targetSelectionRange", Utils.lexLocationToPoint(def.location)))
					:
					new JSONObject(
						"uri", defuri.toString(),
						"range", Utils.lexLocationToRange(def.location)));
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
	
	/**
	 * Abstract LSP methods that are implemented in language specific subclasses.
	 */
	abstract protected FilenameFilter getFilenameFilter();

	abstract protected String[] getFilenameFilters();

	abstract protected TCDefinition findDefinition(File file, int zline, int zcol);

	abstract protected TCDefinitionList lookupDefinition(String startsWith);
}
