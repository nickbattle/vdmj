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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.lex.LexLocation;
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
import workspace.plugins.CTPlugin;
import workspace.plugins.INPlugin;
import workspace.plugins.POPlugin;
import workspace.plugins.TCPlugin;

public class LSPWorkspaceManager
{
	private static LSPWorkspaceManager INSTANCE = null;
	private final PluginRegistry registry;
	private final LSPMessageUtils messages;
	private final Charset encoding;

	private JSONObject clientInfo;
	private JSONObject clientCapabilities;
	private File rootUri = null;
	private Map<File, StringBuilder> projectFiles = new LinkedHashMap<File, StringBuilder>();
	private Set<File> openFiles = new HashSet<File>();
	private boolean orderedFiles = false;
	
	private LSPWorkspaceManager()
	{
		registry = PluginRegistry.getInstance();
		messages = new LSPMessageUtils();
		
		if (System.getProperty("lsp.encoding") == null)
		{
			encoding = Charset.defaultCharset();
			Diag.info("Workspace using default encoding: %s", encoding.name());
		}
		else
		{
			encoding = Charset.forName(System.getProperty("lsp.encoding"));
			Diag.info("Workspace encoding set to %s", encoding.displayName());
		}
	}

	public static synchronized LSPWorkspaceManager getInstance()
	{
		if (INSTANCE == null)
		{
			INSTANCE = new LSPWorkspaceManager();
			
			/**
			 * Register the built-in plugins. Others are registered in LSPXWorkspaceManager,
			 * when the client capabilities have been received.
			 */
			PluginRegistry registry = PluginRegistry.getInstance();
			registry.registerPlugin(ASTPlugin.factory(Settings.dialect));
			registry.registerPlugin(TCPlugin.factory(Settings.dialect));
			registry.registerPlugin(INPlugin.factory(Settings.dialect));
			
			Diag.info("Created LSPWorkspaceManager");
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
	
	public RPCMessageList lspInitialize(RPCRequest request, JSONObject clientInfo, File rootUri, JSONObject clientCapabilities)
		throws IOException
	{
		this.clientInfo = clientInfo;
		this.rootUri = rootUri;
		this.clientCapabilities = clientCapabilities;
		this.openFiles.clear();
		
		LSPXWorkspaceManager.getInstance().enablePlugins();
		
		System.setProperty("vdmj.parser.tabstop", "1");	// Forced, for LSP location offsets
		Properties.init();
		loadAllProjectFiles();
		
		return new RPCMessageList(request, new LSPInitializeResponse());
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
			Diag.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}

	public boolean hasClientCapability(String dotName)	// eg. "workspace.workspaceFolders"
	{
		Object cap = getClientCapability(dotName);
		
		if (cap != null)
		{
			if (cap instanceof Boolean)
			{
				return (Boolean)cap;
			}
			
			return true;	// Object exists
		}
		else
		{
			return false;
		}
	}
	
	public <T> T getClientCapability(String dotName)
	{
		T capability = clientCapabilities.getPath(dotName);
		
		if (capability != null)
		{
			Diag.fine("Client capability %s = %s", dotName, capability);
			return capability;
		}
		else
		{
			Diag.fine("Missing client capability: %s", dotName);
			return null;
		}
	}
	
	public <T> T getClientInfo(String key)
	{
		return clientInfo.get(key);
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
			Diag.info("Loading ordered project files from %s", ordering);
			BufferedReader br = null;
			
			try
			{
				br = new BufferedReader(new FileReader(ordering));
				String source = br.readLine();
				
				while (source != null)
				{
					// Use canonical file to allow "./folder/file"
					File file = new File(rootUri, source).getCanonicalFile();
					Diag.info("Loading %s", file);
					
					if (file.exists())
					{
						loadFile(file);
					}
					else
					{
						Diag.error("Ordering file not found: " + file);
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
			Diag.info("Loading all project files under %s", rootUri);
			loadProjectFiles(rootUri);
		}
	}

	private void loadProjectFiles(File root) throws IOException
	{
		FilenameFilter filter = getFilenameFilter();
		File[] files = root.listFiles();
		
		for (File file: files)
		{
			if (onDotPath(file))
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
				else
				{
					Diag.warning("Ignoring file %s", file.getPath());
				}
			}
		}
	}

	private void loadFile(File file) throws IOException
	{
		StringBuilder sb = new StringBuilder();
		InputStreamReader isr = new InputStreamReader(new FileInputStream(file), encoding);
		char[] data = new char[(int)file.length() + 1];
		int size = isr.read(data);
		
		if (size > 0)	// not empty file
		{
			sb.append(data, 0, size);
		}
		
		isr.close();
		
		projectFiles.put(file, sb);
		Diag.info("Loaded file %s", file.getPath());
	}
	
	private boolean onDotPath(File file)
	{
		// Ignore files on "dot" paths
		String[] parts = file.getAbsolutePath().split(Pattern.quote(File.separator));
		
		for (String part: parts)
		{
			if (!part.isEmpty() && part.charAt(0) == '.')
			{
				return true;
			}
		}
		
		return false;
	}

	private RPCMessageList checkLoadedFiles(String reason) throws Exception
	{
		ASTPlugin ast = registry.getPlugin("AST");
		TCPlugin tc = registry.getPlugin("TC");
		INPlugin in = registry.getPlugin("IN");
		POPlugin po = registry.getPlugin("PO");
		CTPlugin ct = registry.getPlugin("CT");
		
		Diag.info("Checking loaded files (%s)...", reason);
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
					Diag.info("Loaded files checked successfully");
				}
				else
				{
					Diag.error("Failed to create interpreter");
				}
			}
			else
			{
				Diag.error("Type checking errors found");
				DiagUtils.dump(tc.getErrs());
				DiagUtils.dump(tc.getWarns());
			}
		}
		else
		{
			Diag.error("Syntax errors found");
			DiagUtils.dump(ast.getErrs());
			DiagUtils.dump(ast.getWarns());
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

		Diag.info("Checked loaded files.");
		return result;
	}

	public RPCMessageList openFile(RPCRequest request, File file, String text) throws Exception
	{
		if (onDotPath(file))
		{
			Diag.info("Ignoring dot path file", file);
			return null;
		}
		if (!projectFiles.keySet().contains(file))
		{
			Diag.info("Opening new file: %s", file);
			openFiles.add(file);
		}
		else if (openFiles.contains(file))
		{
			Diag.warning("File already open: %s", file);
		}
		else
		{
			Diag.info("Opening known file: %s", file);
			openFiles.add(file);
		}
		
		StringBuilder existing = projectFiles.get(file);
		
		if (orderedFiles && existing == null)
		{
			Diag.error("File not in ordering list: %s", file);
			sendMessage(1L, "Ordering file out of date? " + file);
		}
		else if (existing == null || !existing.toString().equals(text))
		{
			if (existing != null)
			{
				Diag.info("File different on didOpen?");
			}
			
			projectFiles.put(file, new StringBuilder(text));
			checkLoadedFiles("file out of sync");
		}
		
		return null;
	}

	public RPCMessageList closeFile(RPCRequest request, File file) throws Exception
	{
		if (onDotPath(file))
		{
			Diag.info("Ignoring dot path file", file);
		}
		else if (!projectFiles.keySet().contains(file))
		{
			Diag.error("File not known: %s", file);
		}
		else if (!openFiles.contains(file))
		{
			Diag.error("File not open: %s", file);
		}
		else
		{
			Diag.info("Closing file: %s", file);
			openFiles.remove(file);
		}
		
		return null;
	}

	public RPCMessageList changeFile(RPCRequest request, File file, JSONObject range, String text) throws Exception
	{
		if (onDotPath(file))
		{
			Diag.info("Ignoring dot path file", file);
			return null;
		}
		else if (!projectFiles.keySet().contains(file))
		{
			Diag.error("File not known: %s", file);
			return null;
		}
		else if (!openFiles.contains(file))
		{
			Diag.error("File not open: %s", file);
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
			
			DiagUtils.dumpEdit(range, buffer);
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

	/**
	 * The action code returned indicates whether the change watched file requires the
	 * specification to be reloaded and rechecked(2), just re-checked(1), or nothing(0).
	 * Note that these are ordered by severity, so multiple file changes will select
	 * one actionCode that is the most comprehensive. See the handler.
	 */
	
	private static final int DO_NOTHING = 0;
	private static final int RECHECK = 1;
	private static final int RELOAD_AND_CHECK = 2;
	
	public int changeWatchedFile(RPCRequest request, File file, WatchKind type) throws Exception
	{
		FilenameFilter filter = getFilenameFilter();
		int actionCode = DO_NOTHING;
		boolean ignoreDotPath = onDotPath(file);
		
		switch (type)
		{
			case CREATE:
				if (file.isDirectory())
				{
					Diag.info("New directory created: %s", file);
					actionCode = DO_NOTHING;
				}
				else if (ignoreDotPath)
				{
					Diag.info("Ignoring file on dot path: %s", file);
					actionCode = DO_NOTHING;
				}
				else if (file.equals(new File(rootUri, ORDERING)))
				{
					Diag.info("Created ordering file, rebuilding");
					actionCode = RELOAD_AND_CHECK;
				}
				else if (!filter.accept(file.getParentFile(), file.getName()))
				{
					Diag.info("Ignoring non-project file: %s", file);
					actionCode = DO_NOTHING;
				}
				else if (!projectFiles.keySet().contains(file))	
				{
					if (orderedFiles)
					{
						Diag.error("File not in ordering list: %s", file);
						sendMessage(1L, "Ordering file out of date? " + file);
						actionCode = DO_NOTHING;
					}
					else
					{
						Diag.info("Created new file: %s", file);
						loadFile(file);
						actionCode = RECHECK;
					}
				}
				else
				{
					// Usually because a didOpen gets in first, on creation
					Diag.info("Created file already added: %s", file);
					actionCode = DO_NOTHING;	// Already re-built by openFile
				}
				break;
				
			case CHANGE:
				if (file.isDirectory())
				{
					Diag.info("Directory changed: %s", file);
					actionCode = DO_NOTHING;
				}
				else if (ignoreDotPath)
				{
					Diag.info("Ignoring file on dot path: %s", file);
					actionCode = DO_NOTHING;
				}
				else if (file.equals(new File(rootUri, ORDERING)))
				{
					Diag.info("Updated ordering file, rebuilding");
					actionCode = RELOAD_AND_CHECK;
				}
				else if (!filter.accept(file.getParentFile(), file.getName()))
				{
					Diag.info("Ignoring non-project file change: %s", file);
					actionCode = DO_NOTHING;
				}
				else if (!projectFiles.keySet().contains(file))	
				{
					Diag.error("Changed file not known: %s", file);
					actionCode = RELOAD_AND_CHECK;	// Try rebuilding?
				}
				else
				{
					actionCode = RECHECK;	// Simple file change/save
				}
				break;
				
			case DELETE:
				// Since the file is deleted, we don't know what it was so we have to rebuild
				Diag.info("Deleted %s (dir/file?), rebuilding", file);
				actionCode = RELOAD_AND_CHECK;
				break;
		}
		
		return actionCode;
	}

	public RPCMessageList afterChangeWatchedFiles(RPCRequest request, int actionCode) throws Exception
	{
		if (actionCode == RELOAD_AND_CHECK)
		{
			LSPServer server = LSPServer.getInstance();
			
			for (File source: projectFiles.keySet())
			{
				JSONObject noerrs = new JSONObject("uri", source.toURI().toString(), "diagnostics", new JSONArray());
				server.writeMessage(RPCRequest.notification("textDocument/publishDiagnostics", noerrs));
			}

			loadAllProjectFiles();
		}
		
		if (actionCode == RELOAD_AND_CHECK || actionCode == RECHECK)
		{
			RPCMessageList results = checkLoadedFiles("after change watched");
			
			if (hasClientCapability("workspace.codeLens.refreshSupport") ||
				hasClientCapability("experimental.codeLens.refreshSupport"))
			{
				results.add(RPCRequest.create("workspace/codeLens/refresh", null));
			}
			
			return results;
		}
		else
		{
			return null;	// The didChangeWatchedFiles is a notification, so no reply needed.
		}
	}

	/**
	 * This is currently done via watched file events above. Note that this method
	 * is a notification, so cannot return errors.
	 */
	public void saveFile(RPCRequest request, File file, String text) throws Exception
	{
		if (onDotPath(file))
		{
			Diag.info("Ignoring dot path file", file);
		}
		else if (!projectFiles.keySet().contains(file))
		{
			Diag.error("File not known: %s", file);
		}
		else if (!openFiles.contains(file))
		{
			Diag.error("File not open: %s", file);
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
			ASTPlugin ast = registry.getPlugin("AST");
			TCPlugin tc = registry.getPlugin("TC");
			
			if (!ast.getErrs().isEmpty() || !tc.getErrs().isEmpty())
			{
				try
				{
					sendMessage(1L, "Specification contains errors. Cannot locate symbols.");
				}
				catch (IOException e)
				{
					// ignore
				}
			}

			return new RPCMessageList(request, null);
		}
		else if (def.location.file.getName().equals("console") ||
				 def.location.file.getName().equals("?"))
		{
			// This happens for pseudo-symbols like CPU and BUS in RT
			return new RPCMessageList(request, null);
		}
		else
		{
			URI defuri = def.location.file.toURI();
			
			return new RPCMessageList(request,
				new JSONArray(
					new JSONObject(
						"uri", defuri.toString(),
						"range", Utils.lexLocationToRange(def.location))));
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
						result.add(completionForDef(field));
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
					Diag.info("Trying to complete '%s'", word);
					
					for (TCDefinition defn: lookupDefinition(word))
					{
						if (defn.name != null)
						{
							result.add(completionForDef(defn));
						}
					}
				}
			}
		}
		
		return new RPCMessageList(request, result);
	}
	
	private JSONObject completionForDef(TCDefinition defn)
	{
		TCType ftype = defn.getType();
		
		if (ftype instanceof TCOperationType || ftype instanceof TCFunctionType)
		{
			return new JSONObject(
				"label", defn.name.toString(),
				"kind", CompletionItemKind.kindOf(defn).getValue(),
				"insertText", defn.name.toString());	// Include arg types
		}
		else
		{
			return new JSONObject(
				"label", defn.name.getName(),
				"kind", CompletionItemKind.kindOf(defn).getValue(),
				"detail", ftype.toString(),
				"insertText", defn.name.getName());
		}
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
		
		if (!results.isEmpty())
		{
			 StringBuilder buffer = projectFiles.get(file);
			 fixRanges(results, afterLine(Utils.getEndPosition(buffer)));
		}
		
		return new RPCMessageList(request, results);
	}
	
	public RPCMessageList codeLens(RPCRequest request, File file)
	{
		ASTPlugin ast = registry.getPlugin("AST");
		JSONArray lenses = registry.applyCodeLenses(file, ast.isDirty());
		return new RPCMessageList(request, lenses);
	}

	public RPCMessageList codeLensResolve(RPCRequest request, JSONObject data)
	{
		return new RPCMessageList(request);
	}

	/**
	 * Fix the "range" fields of the DocumentSymbol array passed in, such that each
	 * range starts at the selectionRange and ends at the start of the next symbol,
	 * or the end passed (for the last one). Recurse into any children.
	 */
	private void fixRanges(JSONArray symbols, JSONObject endPosition)
	{
		for (int s = 0; s < symbols.size(); s++)
		{
			JSONObject symbol = symbols.index(s);
			JSONObject start = symbol.getPath("selectionRange.start");

			JSONObject nextstart = null;
			
			for (int n = s + 1; n <= symbols.size(); n++)
			{
				if (n == symbols.size())
				{
					nextstart = endPosition;
				}
				else
				{
					JSONObject next = symbols.index(n);
					nextstart = next.getPath("selectionRange.start");
				}
				
				if (!nextstart.get("line").equals(start.get("line")))
				{
					break;	// Guaranteed exit for endPosition
				}
			}
			
			JSONObject range = symbol.get("range");
			range.put("start", startLine(start));
			range.put("end", beforeNext(nextstart));
			
			verifyRange(symbol.get("name"), range, symbol.getPath("selectionRange"));
			
			JSONArray children = symbol.get("children");
			
			if (children != null)
			{
				fixRanges(children, nextstart);
			}
		}
	}
	
	private void verifyRange(String name, JSONObject range, JSONObject selectionRange)
	{
		File file = new File("?");
		LexLocation rloc = Utils.rangeToLexLocation(file, range);
		LexLocation sloc = Utils.rangeToLexLocation(file, selectionRange);
		
		if (!sloc.within(rloc))
		{
			Diag.error("Selection not within range at symbol %s", name);
			Diag.error("Range %s", range);
			Diag.error("Selection %s", selectionRange);
		}
	}

	private JSONObject afterLine(JSONObject position)
	{
		long line = position.get("line");
		return new JSONObject("line", line+1, "character", 0);
	}
	
	private JSONObject startLine(JSONObject position)
	{
		long line = position.get("line");
		return new JSONObject("line", line, "character", 0);
	}
	
	private JSONObject beforeNext(JSONObject next)
	{
		long line = next.get("line");
		return new JSONObject("line", line - 1, "character", 999999999);
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
			Diag.error(e);
		}
	}
}
