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
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.lex.BacktrackInputReader;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.runtime.SourceFile;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;

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
	private Set<File> ignores = new HashSet<File>();
	private boolean checkInProgress = false;
	private Map<File, FileTime> documentFiles = new HashMap<File, FileTime>();
	private Set<File> documentFilesToWarn = new HashSet<File>();
	
	private static final String ORDERING = ".vscode/ordering";
	private static final String VDMIGNORE = ".vscode/vdmignore";
	public static final String PROPERTIES = ".vscode/vdmj.properties";

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
		Diag.info("Reading properties from %s", PROPERTIES);
		Properties.init(PROPERTIES);
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
	
	private void loadAllProjectFiles() throws IOException
	{
		projectFiles.clear();
		clearDocumentFiles();
		documentFiles.clear();
		documentFilesToWarn.clear();
		loadVDMIgnore();
		
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
						if (documentFile(file))
						{
							loadDocFile(file);
						}
						else
						{
							loadFile(file);
						}
					}
					else
					{
						Diag.error("Ordering file not found: " + file);
						sendMessage(ERROR_MSG, "Ordering file not found: " + file);
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

	private void loadVDMIgnore() throws IOException
	{
		File ignoreFile = new File(rootUri, VDMIGNORE);
		ignores.clear();
		
		if (ignoreFile.exists())
		{
			Diag.info("Reading " + VDMIGNORE);
			BufferedReader br = null;
	
			try
			{
				br = new BufferedReader(new FileReader(ignoreFile));
				String source = br.readLine();
				
				while (source != null)
				{
					// Use canonical file to allow "./folder/file"
					File file = new File(rootUri, source).getCanonicalFile();
					ignores.add(file);
					Diag.info("Ignoring %s", file);
					source = br.readLine();
				}
			}
			catch (IOException e)
			{
				Diag.error(e);
				Diag.error("Cannot read " + VDMIGNORE);
			}
			finally
			{
				if (br != null)	br.close();
			}
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
			if (ignoredFile(file))
			{
				continue;	// ignore files in VDMIGNORE
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
				else if (documentFile(file))
				{
					loadDocFile(file);
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
		Diag.info("Loaded file %s encoding %s", file.getPath(), encoding.displayName());
	}
	
	private void loadDocFile(File file) throws IOException
	{
		SourceFile source = new SourceFile(file);
		File vdm = new File(file.getPath() + "." + Settings.dialect.getArgstring().substring(1));
		
		if (vdm.exists())
		{
			Diag.info("Not overwriting existing doc file: %s", vdm);
			documentFiles.put(vdm, FileTime.fromMillis(0));
			documentFilesToWarn.add(vdm);
		}
		else
		{
			Diag.info("Converting document file %s", file);
			PrintWriter spw = new PrintWriter(vdm, encoding.name());
			source.printSource(spw);
			spw.close();
			Diag.info("Extracted source written to " + vdm);
			
			if (vdm.length() > 0)	// eg. not an empty extraction
			{
				loadFile(vdm);
	
				BasicFileAttributes attr = Files.readAttributes(vdm.toPath(), BasicFileAttributes.class);
				documentFiles.put(vdm, attr.lastModifiedTime());
				documentFilesToWarn.add(vdm);
			}
			else
			{
				Diag.info("Removing empty extracted file: %s", vdm);
				vdm.delete();
			}
		}
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
	
	private boolean ignoredFile(File file)
	{
		if (ignores.contains(file))
		{
			return true;
		}
		else
		{
			String path = file.getAbsolutePath();
			
			for (File i: ignores)
			{
				if (i.isDirectory())
				{
					String folder = i.getAbsolutePath();
					
					if (!folder.endsWith(File.separator))
					{
						folder = folder + File.separator;
					}
					
					if (path.startsWith(folder))		// ie. within the folder
					{
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	private boolean documentFile(File file)
	{
		return BacktrackInputReader.isExternalFormat(file);
	}
	
	private void clearDocumentFiles()
	{
		Diag.info("Clearing unchanged document files");
		
		for (Entry<File, FileTime> docfile: documentFiles.entrySet())
		{
			File file = docfile.getKey();
			
			try
			{
				BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
				
				if (attr.lastModifiedTime().equals(docfile.getValue()))
				{
					Diag.info("Deleting unchanged extract file %s", file);
					file.delete();
				}
				else
				{
					Diag.info("Keeping changed extract file %s", file);
				}
			}
			catch (IOException e)
			{
				Diag.error("Problem cleaning up extract %s: %s", file, e);
			}
		}
	}
	
	public synchronized boolean checkInProgress()
	{
		return checkInProgress;
	}

	private synchronized RPCMessageList checkLoadedFiles(String reason) throws Exception
	{
		try
		{
			checkInProgress  = true;
			return checkLoadedFilesSafe(reason);
		}
		catch (Throwable th)
		{
			Diag.error(th);
			throw th;
		}
		finally
		{
			checkInProgress = false;
		}
	}
	
	private RPCMessageList checkLoadedFilesSafe(String reason) throws Exception
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
		
		boolean hasErrors = !ast.getErrs().isEmpty() || !tc.getErrs().isEmpty();
		
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
					new JSONObject("successful", !hasErrors)));
		}
		
		if (hasClientCapability("experimental.combinatorialTesting"))
		{
			ct.checkLoadedFiles(in.getIN());
		}

		result.add(RPCRequest.notification("slsp/checked", new JSONObject("successful", !hasErrors)));

		Diag.info("Checked loaded files.");
		return result;
	}

	public RPCMessageList openFile(RPCRequest request, File file, String text) throws Exception
	{
		if (onDotPath(file))
		{
			Diag.info("Ignoring %s dot path file", file);
			return null;
		}
		else if (ignoredFile(file))
		{
			Diag.info("Ignoring file %s in vdmignore", file);
			return null;			
		}
		else if (!projectFiles.keySet().contains(file))
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
			sendMessage(ERROR_MSG, "Ordering file out of date? " + file);
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
			Diag.info("Ignoring %s dot path file", file);
		}
		else if (ignoredFile(file))
		{
			Diag.info("Ignoring %s file in vdmignore", file);			
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
			Diag.info("Ignoring %s dot path file", file);
			return null;
		}
		else if (ignoredFile(file))
		{
			Diag.info("Ignoring %s file in vdmignore", file);
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
			if (documentFilesToWarn.contains(file))
			{
				sendMessage(WARNING_MSG, "WARNING: Changing generated VDM source: " + file);
				documentFilesToWarn.remove(file);
			}
			
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
				else if (file.equals(new File(rootUri, ORDERING)))
				{
					Diag.info("Created ordering file, rebuilding");
					actionCode = RELOAD_AND_CHECK;
				}
				else if (file.equals(new File(rootUri, VDMIGNORE)))
				{
					Diag.info("Created vdmignore file, rebuilding");
					actionCode = RELOAD_AND_CHECK;
				}
				else if (ignoreDotPath)
				{
					Diag.info("Ignoring file on dot path: %s", file);
					actionCode = DO_NOTHING;
				}
				else if (ignoredFile(file))
				{
					Diag.info("Ignoring %s file in vdmignore", file);
					actionCode = DO_NOTHING;			
				}
				else if (documentFile(file))
				{
					Diag.info("Created new document file, rebuilding");
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
						sendMessage(WARNING_MSG, "Ordering file out of date? " + file);
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
				else if (file.equals(new File(rootUri, ORDERING)))
				{
					Diag.info("Changed ordering file, rebuilding");
					actionCode = RELOAD_AND_CHECK;
				}
				else if (file.equals(new File(rootUri, VDMIGNORE)))
				{
					Diag.info("Changed vdmignore file, rebuilding");
					actionCode = RELOAD_AND_CHECK;
				}
				else if (ignoreDotPath)
				{
					Diag.info("Ignoring file on dot path: %s", file);
					actionCode = DO_NOTHING;
				}
				else if (ignoredFile(file))
				{
					Diag.info("Ignoring %s file in vdmignore", file);
					actionCode = DO_NOTHING;			
				}
				else if (documentFile(file))
				{
					Diag.info("Updated document file, rebuilding");
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
					Diag.info("Simple file change: %s", file);
					actionCode = RECHECK;

					// Can't distinguish quick delete/create from change here, so comment out...
					// if (documentFiles.containsKey(file))
					// {
					// 		sendMessage(WARNING_MSG, "WARNING: Overwriting generated VDM source: " + file);
					// }
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
			Diag.info("Ignoring %s dot path file", file);
		}
		else if (ignoredFile(file))
		{
			Diag.info("Ignoring file %s in vdmignore", file);
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
			if (documentFiles.containsKey(file))
			{
				sendMessage(WARNING_MSG, "WARNING: Saving generated VDM source: " + file);
			}

			if (text != null)
			{
				projectFiles.put(file, new StringBuilder(text));
			}
			
			checkLoadedFiles("saved");
		}
	}

	public RPCMessageList findDefinition(RPCRequest request, File file, int zline, int zcol)
	{
		if (onDotPath(file) || ignoredFile(file))
		{
			return new RPCMessageList(request, null);
		}
		
		TCDefinition def = findDefinition(file, zline, zcol);
		
		if (def == null)
		{
			ASTPlugin ast = registry.getPlugin("AST");
			TCPlugin tc = registry.getPlugin("TC");
			
			if (!ast.getErrs().isEmpty() || !tc.getErrs().isEmpty())
			{
				try
				{
					sendMessage(WARNING_MSG, "Specification contains errors. Cannot locate symbols.");
				}
				catch (IOException e)
				{
					// ignore
				}
			}

			Diag.info("Unable to locate symbol at %s %d:%d", file, zline, zcol);
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

	public RPCMessageList findReferences(RPCRequest request, File file, int zline, int zcol, Boolean incdec)
	{
		if (onDotPath(file) || ignoredFile(file))
		{
			return new RPCMessageList(request, null);
		}
		
		TCDefinition def = findDefinition(file, zline, zcol);
		
		if (def == null || def.name == null)
		{
			Diag.info("Unable to locate symbol at %s %d:%d", file, zline, zcol);
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
			String word = def.name.getName();
			JSONArray results = new JSONArray();
			
			JSONObject defRange = Utils.lexLocationToRange(def.location);
			
			for (File pfile: projectFiles.keySet())
			{
				StringBuilder buffer = projectFiles.get(pfile);
				
				if (buffer.indexOf(word) > 0)
				{
					JSONArray list = Utils.findWords(buffer, word);
					
					for (int i=0; i<list.size(); i++)
					{
						JSONObject range = list.index(i);
						
						if (!range.equals(defRange))	// See incdec below
						{
							TCDefinition def2 = findDefinition(pfile, range);
							
							// Check by location, so that manufactured definitions for fields
							// will match.
							if (def2 != null && def2.location.equals(def.location))
							{
								results.add(
									new JSONObject(
										"uri", pfile.toURI().toString(),
										"range", range));
							}
						}
					}
				}
			}
			
			if (incdec)
			{
				results.add(new JSONObject(
						"uri", def.location.file.toURI().toString(),
						"range", defRange));
			}
			
			return new RPCMessageList(request, results);
		}
	}

	public RPCMessageList prepareHierarchy(RPCRequest request, File file, int zline, int zcol)
	{
		// We can assume we're PP or RT
		TCDefinition def = findDefinition(file, zline, zcol);
		
		if (def == null || def.name == null)
		{
			Diag.info("Unable to locate symbol at %s %d:%d", file, zline, zcol);
			return new RPCMessageList(request, null);
		}
		else if (def.location.file.getName().equals("console") ||
				 def.location.file.getName().equals("?"))
		{
			// This happens for pseudo-symbols like CPU and BUS in RT
			return new RPCMessageList(request, null);
		}
		else if (def instanceof TCClassDefinition)
		{
			TCClassDefinition cdef = (TCClassDefinition)def;
			return new RPCMessageList(request, messages.typeHierarchyItem(cdef));
		}
		else
		{
			Diag.info("Type hierarchy request isn't for class object");
			return new RPCMessageList(request, null);
		}
	}

	public RPCMessageList getTypeHierarchy(RPCRequest request, String classname, boolean subtypes)
	{
		TCPlugin tc = registry.getPlugin("TC");
		TCClassList results = tc.getTypeHierarchy(classname, subtypes);
		return new RPCMessageList(request, messages.typeHierarchyItems(results));
	}

	public RPCMessageList completion(RPCRequest request,
			CompletionTriggerKind triggerKind, File file, int zline, int zcol)
	{
		HashMap<String, JSONObject> labels = new HashMap<String, JSONObject>();
		
		if (onDotPath(file) || ignoredFile(file))
		{
			return new RPCMessageList(request, new JSONArray());
		}
		
		TCDefinition def = findDefinition(file, zline, zcol - 2);
	
		if (def != null)
		{
			if (def.getType() instanceof TCRecordType)
			{
				TCRecordType rtype = (TCRecordType)def.getType();
				
				for (TCField field: rtype.fields)
				{
					labels.put(field.tag, new JSONObject(
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
						JSONObject comp = completionForDef(field);
						labels.put(comp.get("label"), comp);
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
							JSONObject comp = completionForDef(defn);
							labels.put(comp.get("label"), comp);
						}
					}
				}
			}
		}
		
		JSONArray result = new JSONArray();
		result.addAll(labels.values());		
		
		return new RPCMessageList(request, result);
	}
	
	private JSONObject completionForDef(TCDefinition defn)
	{
		TCType type = defn.getType();
		String label = null;
		String insertText = null;
		
		if (type instanceof TCOperationType)
		{
			TCOperationType otype = (TCOperationType)type;
			label = defn.name.toString();
			insertText = defn.name.getName() + snippetText(otype.parameters);
		}
		else if (type instanceof TCFunctionType)
		{
			TCFunctionType ftype = (TCFunctionType)type;
			label = defn.name.toString();
			insertText = defn.name.getName() + snippetText(ftype.parameters);
		}
		else
		{
			return new JSONObject(
					"label", defn.name.getName(),
					"kind", CompletionItemKind.kindOf(defn).getValue(),
					"detail", type.toString(),
					"insertText", defn.name.getName());
		}

		return new JSONObject(
			"label", label,
			"kind", CompletionItemKind.kindOf(defn).getValue(),
			"insertText", insertText,
			"insertTextFormat", 2);		// Snippet format
	}

	/**
	 * A snippet string like "(${1:nat}, ${2:seq of nat})" 
	 */
	private String snippetText(TCTypeList parameters)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		int pos = 1;
		String sep = "";
		
		for (TCType p: parameters)
		{
			sb.append(sep);
			sb.append("${");
			sb.append(pos++);
			sb.append(":");
			sb.append(p.toString());
			sb.append("}");
			sep = ", ";
		}
		
		sb.append(")");
		return sb.toString();
	}

	public RPCMessageList documentSymbols(RPCRequest request, File file)
	{
		if (onDotPath(file) || ignoredFile(file))
		{
			return new RPCMessageList(request, new JSONArray());
		}

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
		if (onDotPath(file) || ignoredFile(file))
		{
			return new RPCMessageList(request, new JSONArray());
		}

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

	private TCDefinition findDefinition(File file, JSONObject range)
	{
		TCPlugin plugin = registry.getPlugin("TC");
		JSONObject start = range.get("start");
		long zline = start.get("line");
		long zcol  = start.get("character");
		return plugin.findDefinition(file, (int)zline, (int)zcol);
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

	private static final long ERROR_MSG = 1L;
	private static final long WARNING_MSG = 2L;
	
	private void sendMessage(Long type, String message) throws IOException
	{
		LSPServer.getInstance().writeMessage(RPCRequest.notification("window/showMessage",
				new JSONObject("type", type, "message", message)));
	}
	
	public RPCMessageList shutdown(RPCRequest request)
	{
		Diag.info("Shutting down server");
		LSPServer.getInstance().setInitialized(false);
		clearDocumentFiles();
		return new RPCMessageList(request);
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
