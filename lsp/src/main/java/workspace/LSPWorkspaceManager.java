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
import lsp.LSPServer;
import lsp.Utils;
import lsp.textdocument.CompletionItemKind;
import lsp.textdocument.CompletionTriggerKind;
import lsp.textdocument.WatchKind;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.plugins.ASTPlugin;
import workspace.plugins.ASTPluginPR;
import workspace.plugins.ASTPluginSL;
import workspace.plugins.CTPlugin;
import workspace.plugins.INPlugin;
import workspace.plugins.INPluginPR;
import workspace.plugins.INPluginSL;
import workspace.plugins.POPlugin;
import workspace.plugins.TCPlugin;
import workspace.plugins.TCPluginPR;
import workspace.plugins.TCPluginSL;

public class LSPWorkspaceManager
{
	private static LSPWorkspaceManager INSTANCE = null;
	protected final PluginRegistry registry;
	protected final LSPMessageUtils messages;

	private JSONObject clientCapabilities;
	private File rootUri = null;
	private Map<File, StringBuilder> projectFiles = new HashMap<File, StringBuilder>();
	private Set<File> openFiles = new HashSet<File>();
	
	private LSPWorkspaceManager()
	{
		registry = PluginRegistry.getInstance();
		messages = new LSPMessageUtils();
	}

	public static synchronized LSPWorkspaceManager getInstance()
	{
		if (INSTANCE == null)
		{
			INSTANCE = new LSPWorkspaceManager();
			PluginRegistry _registry = PluginRegistry.getInstance();
			
			switch (Settings.dialect)
			{
				case VDM_SL:
					_registry.registerPlugin(new ASTPluginSL());
					_registry.registerPlugin(new TCPluginSL());
					_registry.registerPlugin(new INPluginSL());
					break;
					
				case VDM_PP:
				case VDM_RT:
					_registry.registerPlugin(new ASTPluginPR());
					_registry.registerPlugin(new TCPluginPR());
					_registry.registerPlugin(new INPluginPR());
					break;
					
				default:
					throw new RuntimeException("Unsupported dialect: " + Settings.dialect);
			}
		}

		return INSTANCE;
	}
	
	/**
	 * This is only used by unit testing.
	 */
	public static void reset()
	{
		PluginRegistry.reset();
		INSTANCE = null;
	}
	
	public File getRoot()
	{
		return rootUri;
	}

	public Map<File, StringBuilder> getProjectFiles()
	{
		return projectFiles;
	}
	
	/**
	 * LSP methods...
	 */
	
	public RPCMessageList lspInitialize(RPCRequest request)
	{
		try
		{
			JSONObject params = request.get("params");
			rootUri = Utils.uriToFile(params.get("rootUri"));
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
				// response.add(lspWorkspaceFolders());
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

	private RPCRequest lspDynamicRegistrations()
	{
		JSONArray watchers = new JSONArray();
		
		for (String glob: getFilenameFilters())
		{
			// Add the rootUri so that we only notice changes in our own project
			watchers.add(new JSONObject("globPattern", rootUri.getAbsolutePath() + "/" + glob));
		}
		
		return RPCRequest.create("client/registerCapability",
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
		loadProjectFiles(rootUri);
	}

	private void loadProjectFiles(File root) throws IOException
	{
		FilenameFilter filter = getFilenameFilter();
		File[] files = root.listFiles();
		
		for (File file: files)
		{
			if (file.getName().startsWith("."))
			{
				continue;	// ignore .generated, .vscode etc
			}
			else if (file.isDirectory())
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
		InputStreamReader isr = new InputStreamReader(new FileInputStream(file));
		char[] data = new char[(int)file.length() + 1];
		int size = isr.read(data);
		sb.append(data, 0, size);
		isr.close();
		
		projectFiles.put(file, sb);
	}

	private RPCMessageList checkLoadedFiles() throws Exception
	{
		ASTPlugin ast = registry.getPlugin("AST");
		TCPlugin tc = registry.getPlugin("TC");
		INPlugin in = registry.getPlugin("IN");
		POPlugin po = registry.getPlugin("PO");
		CTPlugin ct = registry.getPlugin("CT");
		
		Log.printf("Checking loaded files...");
		ast.preCheck();
		tc.preCheck();
		in.preCheck();
		
		if (hasClientCapability("experimental.proofObligationGeneration"))
		{
			po.preCheck();
		}
		
		if (hasClientCapability("experimental.combinatorialTesting"))
		{
			ct.preCheck();
		}
		
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
		RPCMessageList result = messages.diagnosticResponses(diags, projectFiles.keySet());
		
		if (hasClientCapability("experimental.proofObligationGeneration"))
		{
			po.checkLoadedFiles(tc.getTC());

			result.add(RPCRequest.notification("slsp/POG/updated",
					new JSONObject("successful", tc.getErrs().isEmpty())));
		}
		
		if (hasClientCapability("experimental.combinatorialTesting"))
		{
			ct.checkLoadedFiles(in.getIN());
		}

		Log.printf("Checked loaded files.");
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
			// Should be covered by changeWatchedFile below, but to be safe...
			Log.printf("Opening new file: %s", file);
			openFiles.add(file);
		}
		else if (openFiles.contains(file))
		{
			Log.error("File already open: %s", file);
		}
		else
		{
			Log.printf("Opening known file: %s", file);
			openFiles.add(file);
		}
		
		StringBuilder existing = projectFiles.get(file);
		
		if (existing == null || !existing.toString().equals(text))
		{
			if (existing != null) Utils.diff("File different on didOpen at %d", text, existing.toString());
			projectFiles.put(file, new StringBuilder(text));
			checkLoadedFiles();
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
			List<VDMMessage> errors = ast.fileChanged(file);
			
			// Add TC errors as these need to be seen until the next save
			TCPlugin tc = registry.getPlugin("TC");
			errors.addAll(tc.getErrs());
			errors.addAll(tc.getWarns());
			
			// We report on this file, plus the files with tc errors (if any).
			Set<File> files = messages.filesOfMessages(errors);
			files.add(file);
			return messages.diagnosticResponses(errors, files);
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
			projectFiles.put(file, new StringBuilder(text));
			return checkLoadedFiles();		// typecheck on save
		}
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

	public RPCMessageList completion(RPCRequest request,
			CompletionTriggerKind triggerKind, File file, int zline, int zcol)
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
				int start = 0;
				int end = 0;
				
				switch (triggerKind)
				{
					case INVOKED:
						start = position - 1;	// eg. the "t" in "root"
						end = position;
						break;
						
					case TRIGGERCHARACTER:
					case INCOMPLETE:
						start = position - 2;	// eg. the "t" in "root."
						end = position - 1;
						break;
				}
				
				while (start >= 0 && Character.isJavaIdentifierPart(buffer.charAt(start)))
				{
					start--;
				}
				
				String word = buffer.subSequence(start + 1, end).toString();
				
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

	public RPCMessageList documentSymbols(RPCRequest request, File file)
	{
		TCPlugin tc = registry.getPlugin("TC");
		JSONArray results = tc.documentSymbols(file);

		if (results.isEmpty())
		{
			ASTPlugin ast = registry.getPlugin("AST");
			results = ast.documentSymbols(file);
		}
		
		return new RPCMessageList(request, results);
	}
	
	private TCDefinition findDefinition(File file, int zline, int zcol)
	{
		TCPlugin plugin = registry.getPlugin("TC");
		return plugin.findDefinition(file, zline, zcol);
	}

	private TCDefinitionList lookupDefinition(String startsWith)
	{
		TCPlugin plugin = registry.getPlugin("TC");
		return plugin.lookupDefinition(startsWith);
	}

	private FilenameFilter getFilenameFilter()
	{
		ASTPlugin ast = registry.getPlugin("AST");
		return ast.getFilenameFilter();
	}

	private String[] getFilenameFilters()
	{
		ASTPlugin ast = registry.getPlugin("AST");
		return ast.getFilenameFilters();
	}

	public void restart()	// Called from DAP manager
	{
		try
		{
			RPCMessageList messages = checkLoadedFiles();
			LSPServer server = LSPServer.getInstance();
			
			for (JSONObject response: messages)
			{
				server.writeMessage(response);
			}
		}
		catch (Exception e)
		{
			Log.error(e);
		}
	}
}
