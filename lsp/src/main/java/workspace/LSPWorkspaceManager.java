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
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
	private Map<File, StringBuilder> projectFiles = new LinkedHashMap<File, StringBuilder>();
	private Set<File> openFiles = new HashSet<File>();
	private boolean orderedFiles = false;
	
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
			Log.error(e);
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
			response.addAll(checkLoadedFiles("initialized"));
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
		
		// Add the rootUri so that we only notice changes in our own project.
		// We watch for all files/dirs and deal with filtering in changedWatchedFiles,
		// otherwise directory deletions are not notified.
		watchers.add(new JSONObject("globPattern", rootUri.getAbsolutePath() + "/**"));
		
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
	
	private static final String ORDERING = ".vscode/ordering";

	private void loadAllProjectFiles() throws IOException
	{
		projectFiles.clear();
		File ordering = new File(rootUri, ORDERING);
		orderedFiles = ordering.exists();
		
		if (orderedFiles)
		{
			Log.printf("Loading ordered project files from %s", ordering);
			BufferedReader br = null;
			
			try
			{
				br = new BufferedReader(new FileReader(ordering));
				String source = br.readLine();
				
				while (source != null)
				{
					// Use canonical file to allow "./folder/file"
					File file = new File(rootUri, source).getCanonicalFile();
					Log.printf("Loading %s", file);
					
					if (file.exists())
					{
						loadFile(file);
					}
					else
					{
						Log.error("Ordering file not found: " + file);
						sendMessage(1L, "Ordering file not found: " + file);
					}
					
					source = br.readLine();
				}
			}
			finally
			{
				if (br != null)	br.close();
			}
		}
		else
		{
			Log.printf("Loading all project files under %s", rootUri);
			loadProjectFiles(rootUri);
		}
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
		
		if (size > 0)	// not empty file
		{
			sb.append(data, 0, size);
		}
		
		isr.close();
		
		projectFiles.put(file, sb);
	}

	private RPCMessageList checkLoadedFiles(String reason) throws Exception
	{
		ASTPlugin ast = registry.getPlugin("AST");
		TCPlugin tc = registry.getPlugin("TC");
		INPlugin in = registry.getPlugin("IN");
		POPlugin po = registry.getPlugin("PO");
		CTPlugin ct = registry.getPlugin("CT");
		
		Log.printf("Checking loaded files (%s)...", reason);
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
			Log.printf("Opening known file: %s", file);
			openFiles.add(file);
		}
		
		StringBuilder existing = projectFiles.get(file);
		
		if (orderedFiles && existing == null)
		{
			Log.error("File not in ordering list: %s", file);
			sendMessage(1L, "Ordering file out of date? " + file);
		}
		else if (existing == null || !existing.toString().equals(text))
		{
			if (existing != null)
			{
				Log.printf("File different on didOpen?");
			}
			
			projectFiles.put(file, new StringBuilder(text));
			checkLoadedFiles("file out of sync");
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

	private boolean rebuildAfterWatch = false;
	
	public void changeWatchedFile(RPCRequest request, File file, WatchKind type) throws Exception
	{
		FilenameFilter filter = getFilenameFilter();

		switch (type)
		{
			case CREATE:
				if (file.isDirectory())
				{
					Log.printf("New directory created: %s", file);
				}
				else if (file.equals(new File(rootUri, ".vscode/ordering")))
				{
					Log.printf("Created ordering file, rebuilding");
					rebuildAfterWatch = true;
				}
				else if (!filter.accept(file.getParentFile(), file.getName()))
				{
					Log.printf("Ignoring non-project file: %s", file);
				}
				else if (!projectFiles.keySet().contains(file))	
				{
					if (orderedFiles)
					{
						Log.error("File not in ordering list: %s", file);
						sendMessage(1L, "Ordering file out of date? " + file);
					}
					else
					{
						Log.printf("Created new file: %s", file);
						loadFile(file);
					}
				}
				else
				{
					// Usually because a didOpen gets in first, on creation
					Log.printf("Created file already added: %s", file);
				}
				break;
				
			case CHANGE:
				if (file.isDirectory())
				{
					Log.printf("Directory changed: %s", file);
				}
				else if (file.equals(new File(rootUri, ".vscode/ordering")))
				{
					Log.printf("Updated ordering file, rebuilding");
					rebuildAfterWatch = true;
				}
				else if (!filter.accept(file.getParentFile(), file.getName()))
				{
					Log.printf("Ignoring non-project file change: %s", file);
				}
				else if (!projectFiles.keySet().contains(file))	
				{
					Log.error("Changed file not known: %s", file);
				}
				break;
				
			case DELETE:
				// Since the file is deleted, we don't know what it was so we have to rebuild
				Log.printf("Deleted %s (dir/file?), rebuilding", file);
				rebuildAfterWatch = true;
				break;
		}
	}

	public RPCMessageList afterChangeWatchedFiles(RPCRequest request) throws Exception
	{
		if (rebuildAfterWatch)
		{
			LSPServer server = LSPServer.getInstance();
			
			for (File source: projectFiles.keySet())
			{
				JSONObject noerrs = new JSONObject("uri", source.toURI().toString(), "diagnostics", new JSONArray());
				server.writeMessage(RPCRequest.notification("textDocument/publishDiagnostics", noerrs));
			}

			loadAllProjectFiles();
			rebuildAfterWatch = false;
		}
		
		return checkLoadedFiles("after change watched");
	}

	/**
	 * This is currently done via watched file events above. Note that this method
	 * is a notification, so cannot return errors.
	 */
	public void saveFile(RPCRequest request, File file, String text) throws Exception
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
			if (text != null)
			{
				projectFiles.put(file, new StringBuilder(text));
			}
			
			checkLoadedFiles("saved");
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
							"targetSelectionRange", Utils.lexLocationToPosition(def.location)))
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
	
	private void sendMessage(Long type, String message) throws IOException
	{
		LSPServer.getInstance().writeMessage(RPCRequest.notification("window/showMessage",
				new JSONObject("type", type, "message", message)));
	}

	public void restart()	// Called from DAP manager
	{
		try
		{
			RPCMessageList messages = checkLoadedFiles("restart");
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
