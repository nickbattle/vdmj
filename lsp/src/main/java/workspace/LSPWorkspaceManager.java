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
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.lex.BacktrackInputReader;
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
import workspace.events.ChangeFileEvent;
import workspace.events.CheckCompleteEvent;
import workspace.events.CheckFailedEvent;
import workspace.events.CheckPrepareEvent;
import workspace.events.CheckSyntaxEvent;
import workspace.events.CheckTypeEvent;
import workspace.events.CloseFileEvent;
import workspace.events.CodeLensEvent;
import workspace.events.InitializeEvent;
import workspace.events.InitializedEvent;
import workspace.events.LSPEvent;
import workspace.events.OpenFileEvent;
import workspace.events.SaveFileEvent;
import workspace.events.ShutdownEvent;
import workspace.plugins.ASTPlugin;
import workspace.plugins.INPlugin;
import workspace.plugins.TCPlugin;

public class LSPWorkspaceManager
{
	private static LSPWorkspaceManager INSTANCE = null;
	private PluginRegistry registry;
	private EventHub eventhub;
	private MessageHub messagehub;
	private final LSPMessageUtils msgutils;
	private final Charset encoding;

	private JSONObject clientInfo;
	private JSONObject clientCapabilities;
	private File rootUri = null;
	private Map<File, StringBuilder> projectFiles = new LinkedHashMap<File, StringBuilder>();
	private Set<File> openFiles = new HashSet<File>();
	private boolean checkInProgress = false;

	private List<File> vdmignore = new Vector<File>();
	private List<File> ordering = new Vector<File>();
	private boolean hasOrderedFiles = false;
	private Map<File, FileTime> externalFiles = new HashMap<File, FileTime>();
	private Set<File> externalFilesWarned = new HashSet<File>();
	private List<File> externals = new Vector<File>();
	private List<File> ignoreChangesList = new Vector<File>();
	
	private static final String ORDERING = ".vscode/ordering";
	private static final String VDMIGNORE = ".vscode/vdmignore";
	private static final String EXTERNALS = ".vscode/externals";
	public static final String PROPERTIES = ".vscode/vdmj.properties";

	private LSPWorkspaceManager()
	{
		registry = PluginRegistry.getInstance();
		eventhub = EventHub.getInstance();
		messagehub = MessageHub.getInstance();
		msgutils = new LSPMessageUtils();
		
		if (System.getProperty("lsp.encoding") == null)
		{
			encoding = Charset.defaultCharset();
			Diag.info("Workspace created, using default encoding: %s", encoding.name());
		}
		else
		{
			encoding = Charset.forName(System.getProperty("lsp.encoding"));
			Diag.info("Workspace created, encoding set to %s", encoding.displayName());
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
	
	public static void reset()
	{
		Diag.config("Resetting WorkspaceManagers, PluginRegistry, EventHub and MessageHub");
		
		LSPXWorkspaceManager.reset();
		DAPWorkspaceManager.reset();
		DAPXWorkspaceManager.reset();
		PluginRegistry.reset();
		EventHub.reset();
		MessageHub.reset();
		
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
		throws Exception
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
		
		RPCMessageList responses = new RPCMessageList(request, new LSPInitializeResponse());
		responses.addAll(eventhub.publish(new InitializeEvent(request)));
		
		return responses;
	}

	public RPCMessageList lspInitialized(RPCRequest request)
	{
		try
		{
			RPCMessageList response = new RPCMessageList();
			response.addAll(checkLoadedFiles("initialized"));
			response.addAll(eventhub.publish(new InitializedEvent(request)));
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

	private void loadAllProjectFiles() throws IOException
	{
		projectFiles.clear();
		externalFilesWarned.clear();	// Re-warn after reloads
		messagehub.clear();
		
		removeExtractedFiles();
		externalFiles.clear();
		
		vdmignore = readFileList(VDMIGNORE, true);
		externals = readFileList(EXTERNALS, true);
		ordering  = readFileList(ORDERING, false);	// Don't glob this one!
		
		hasOrderedFiles = !ordering.isEmpty();
		
		if (hasOrderedFiles)
		{
			Diag.info("Loading ordered project files from %s", ORDERING);
			loadOrderedFiles();
		}
		else
		{
			Diag.info("Loading all project files under %s", rootUri);
			loadProjectFiles(rootUri);
		}
	}
	
	private void loadOrderedFiles() throws IOException
	{
		for (File file: ordering)
		{
			Diag.info("Loading ordered item %s", file);
			
			if (file.exists())
			{
				if (onDotPath(file))
				{
					Diag.info("Ignoring ordering item on dot path: %s", file);
					sendMessage(WARNING_MSG, "Ignoring ordering item on dot path: " + file);
				}
				else if (ignoredFile(file))
				{
					Diag.info("Ignoring ordering item in vdmignore: %s", file);
					sendMessage(WARNING_MSG, "Ignoring ordering item in vdmignore: " + file);
				}
				else if (isExternalFile(file))
				{
					loadExternalFile(file);
				}
				else
				{
					loadFile(file);
				}
			}
			else
			{
				Diag.error("Ordering item not found: " + file);
				sendMessage(ERROR_MSG, "Ordering item not found: " + file);
			}
		}
	}

	private List<File> readFileList(String filename, boolean globbing) throws IOException
	{
		List<File> contents = new Vector<File>();
		File fileList = new File(rootUri, filename);
		
		if (fileList.exists())
		{
			Diag.info("Reading " + filename);
			BufferedReader br = null;
	
			try
			{
				br = new BufferedReader(new FileReader(fileList));
				boolean hasText = false;
				
				for (String source = br.readLine(); source != null; source = br.readLine())
				{
					source = source.trim();
					
					if (!source.isEmpty())
					{
						hasText = true;	// has some text!
						Diag.info("Read %s from %s", source, filename);

						if (globbing)
						{
							try
							{
								GlobFinder finder = new GlobFinder(source);
								Files.walkFileTree(Paths.get(""), finder);
								List<File> found = finder.getMatches();
								
								for (File file: found)
								{
									Diag.fine("Glob: %s", file);
								}
								
								contents.addAll(found);
							}
							catch (PatternSyntaxException e)
							{
								Diag.error(e);
								sendMessage(WARNING_MSG, filename + ": " + source + ": " + e.getDescription());
							}
						}
						else
						{
							// Use canonical file to allow "./folder/file"
							File item = new File(rootUri, source).getCanonicalFile();
							contents.add(item);
						}
					}
				}
				
				if (contents.isEmpty() && hasText)
				{
					if (globbing)
					{
						Diag.warning("Config file has no matches: %s", filename);
						sendMessage(WARNING_MSG, filename + " matches no files?");
					}
					else
					{
						Diag.warning("Config file has no entries: %s", filename);
					}
				}
			}
			catch (IOException e)
			{
				Diag.error(e);
				Diag.error("Cannot read " + filename);
			}
			finally
			{
				if (br != null)	br.close();
			}
		}
		
		return contents;
	}

	private void loadProjectFiles(File root) throws IOException
	{
		FilenameFilter filter = getFilenameFilter();
		File[] files = root.listFiles();
		List<File> ignored = new Vector<File>();
		
		for (File file: files)
		{
			if (onDotPath(file))
			{
				continue;	// ignore .generated, .vscode etc
			}
			else if (ignoredFile(file))
			{
				continue;	// ignore files in VDMIGNORE
			}
			else if (file.isDirectory())
			{
				loadProjectFiles(file);		// Recurse into subdir
			}
			else if (filter.accept(root, file.getName()))
			{
				loadFile(file);
			}
			else if (isExternalFile(file))
			{
				if (externals.isEmpty() || externals.contains(file))
				{
					loadExternalFile(file);
				}
				else
				{
					File extract = getExtractedName(file);
					
					if (extract.exists())
					{
						Diag.info("Removing ignored VDM extract: %s", extract);
						extract.delete();
						externalFiles.remove(extract);
					}
					else
					{
						Diag.fine("Ignoring external file %s", file);
					}
				}
			}
			else
			{
				Diag.warning("Ignoring file %s", file.getPath());
				ignored.add(file);
			}
		}
		
		if (!ignored.isEmpty())
		{
			Collections.sort(ignored);
			StringBuilder sb = new StringBuilder();
			sb.append("These files can be added to vdmignore:\n");
			String project = rootUri.getPath();
			
			for (File ignore: ignored)
			{
				if (ignore.getPath().startsWith(project))	// It should!
				{
					sb.append(ignore.getPath().substring(project.length() + 1));
				}
				else
				{
					sb.append(ignore);
				}
				
				sb.append("\n");
			}
			
			sendMessage(WARNING_MSG, sb.toString());
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
		messagehub.addFile(file);
		Diag.info("Loaded file %s encoding %s", file.getPath(), encoding.displayName());
	}
	
	private File getExtractedName(File file)
	{
		return new File(file.getPath() + "." + Settings.dialect.getArgstring().substring(1));
	}
	
	private void loadExternalFile(File file) throws IOException
	{
		File extract = getExtractedName(file);
		
		if (extract.exists())
		{
			Diag.info("Not overwriting existing extract file: %s", extract);
			externalFiles.put(extract, FileTime.fromMillis(0));
			loadFile(extract);
		}
		else
		{
			SourceFile source = new SourceFile(file);
	
			if (source.hasContent())	// ie. not an empty extraction
			{
				Diag.info("Processing external file %s", file);
				PrintWriter spw = new PrintWriter(extract, encoding.name());
				source.printSource(spw);
				spw.close();
				Diag.info("Extracted source written to " + extract);
				
				loadFile(extract);
	
				BasicFileAttributes attr = Files.readAttributes(extract.toPath(), BasicFileAttributes.class);
				externalFiles.put(extract, attr.lastModifiedTime());
			}
			else
			{
				Diag.info("External file contains no VDM source: %s", file);
			}
		}
	}

	private boolean onDotPath(File file)
	{
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
		if (vdmignore.contains(file))
		{
			return true;
		}
		else
		{
			String path = file.getAbsolutePath();
			
			for (File i: vdmignore)
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
	
	private boolean isExternalFile(File file)
	{
		return BacktrackInputReader.isExternalFormat(file);
	}
	
	private boolean isExtractedFile(File file)
	{
		// Check whether the file, less the dialect extension, is external.
		String suffix = "." + Settings.dialect.getArgstring().substring(1);	// eg. ".vdmsl"
		String path = file.getPath();
		
		if (path.endsWith(suffix))
		{
			File prefix = new File(path.substring(0, path.lastIndexOf(suffix)));
			return isExternalFile(prefix);	// prefix of file is external => file extracted
		}
		else
		{
			return false;
		}
	}
	
	private void removeExtractedFiles()
	{
		Diag.info("Clearing unchanged extracted files");
		ignoreChangesList.clear();
		
		for (Entry<File, FileTime> extfile: externalFiles.entrySet())
		{
			File extract = extfile.getKey();
			
			if (extract.exists())
			{
				try
				{
					BasicFileAttributes attr = Files.readAttributes(extract.toPath(), BasicFileAttributes.class);
					
					if (attr.lastModifiedTime().equals(extfile.getValue()))
					{
						Diag.info("Deleting unchanged extracted file %s", extract);
						extract.delete();
						ignoreChangesList.add(extract);
					}
					else
					{
						Diag.info("Keeping changed extracted file %s", extract);
					}
				}
				catch (IOException e)
				{
					Diag.error("Problem cleaning up %s: %s", extract, e);
				}
			}
		}
	}
	
	/**
	 * This will wait for up to 5 seconds before returning the checkInProgress
	 * flag, to give a chance for a type check to complete.
	 */
	public synchronized boolean checkInProgress()
	{
		for (int retry = 50; retry > 0 && checkInProgress; retry--)
		{
			Diag.fine("Waiting for check to complete, %d", retry);
			pause(100);
		}
		
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
		Diag.info("Checking loaded files (%s)...", reason);
		RPCMessageList results = new RPCMessageList();

		LSPEvent event = new CheckPrepareEvent();
		results.addAll(eventhub.publish(event));

		if (!messagehub.hasErrors())
		{
			event = new CheckSyntaxEvent();
			results.addAll(eventhub.publish(event));
			
			if (!messagehub.hasErrors())
			{
				event = new CheckTypeEvent();
				results.addAll(eventhub.publish(event));
	
				if (!messagehub.hasErrors())
				{
					event = new CheckCompleteEvent();
					results.addAll(eventhub.publish(event));
	
					if (!messagehub.hasErrors())
					{
						Diag.info("Loaded files checked successfully");
					}
					else
					{
						Diag.error("Failed to initialize interpreter");
						results.addAll(eventhub.publish(new CheckFailedEvent(event)));
					}
				}
				else
				{
					Diag.error("Type checking errors found");
					results.addAll(eventhub.publish(new CheckFailedEvent(event)));
				}
			}
			else
			{
				Diag.error("Syntax errors found");
				results.addAll(eventhub.publish(new CheckFailedEvent(event)));
			}
		}
		else
		{
			Diag.error("Preparation errors found");
			results.addAll(eventhub.publish(new CheckFailedEvent(event)));
		}

		results.addAll(messagehub.getDiagnosticResponses(projectFiles.keySet()));
		results.add(RPCRequest.notification("slsp/checked", new JSONObject("successful", !messagehub.hasErrors())));

		Diag.info("Checked loaded files.");
		return results;
	}

	public RPCMessageList lspDidOpen(RPCRequest request, File file, String text) throws Exception
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
		else if (!projectFiles.containsKey(file))
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
		
		eventhub.publish(new OpenFileEvent(request, file));
		
		StringBuilder existing = projectFiles.get(file);
		
		if (hasOrderedFiles && !isExtractedFile(file) && existing == null)
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
			messagehub.addFile(file);
			return checkLoadedFiles("file out of sync");
		}
		
		return null;
	}

	public RPCMessageList lspDidClose(RPCRequest request, File file) throws Exception
	{
		if (onDotPath(file))
		{
			Diag.info("Ignoring %s dot path file", file);
		}
		else if (ignoredFile(file))
		{
			Diag.info("Ignoring %s file in vdmignore", file);			
		}
		else if (!projectFiles.containsKey(file))
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
			eventhub.publish(new CloseFileEvent(request, file));
		}
		
		return null;
	}

	public RPCMessageList lspDidChange(RPCRequest request, File file, JSONObject range, String text) throws Exception
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
		else if (!projectFiles.containsKey(file))
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
			if (externalFiles.containsKey(file) && !externalFilesWarned.contains(file))
			{
				sendMessage(WARNING_MSG, "WARNING: Changing extracted VDM source: " + file);
				externalFilesWarned.add(file);
			}
			
			StringBuilder buffer = projectFiles.get(file);
			
			if (range != null)
			{
				int start = Utils.findPosition(buffer, range.get("start"));
				int end   = Utils.findPosition(buffer, range.get("end"));
				
				if (start >= 0 && end >= 0)
				{
					buffer.replace(start, end, text);
				}
				
				DiagUtils.dumpEdit(range, buffer);
			}
			else
			{
				Diag.fine("Replacing entire content of %s", file);
				buffer.setLength(0);
				buffer.append(text);
			}
			
			return eventhub.publish(new ChangeFileEvent(request, file));
		}
	}

	/**
	 * This is currently done via watched file events above in VSCode. Note that this method
	 * is a notification, but we do return error notifications.
	 */
	public RPCMessageList lspDidSave(RPCRequest request, File file, String text) throws Exception
	{
		RPCMessageList messages = null;
		
		if (onDotPath(file))
		{
			Diag.info("Ignoring %s dot path file", file);
		}
		else if (ignoredFile(file))
		{
			Diag.info("Ignoring file %s in vdmignore", file);
		}
		else if (!projectFiles.containsKey(file))
		{
			Diag.error("File not known: %s", file);
		}
		else if (!openFiles.contains(file))
		{
			Diag.error("File not open: %s", file);
		}
		else
		{
			if (externalFiles.containsKey(file))
			{
				sendMessage(WARNING_MSG, "WARNING: Saving extracted VDM source: " + file);
			}
	
			if (text != null)
			{
				projectFiles.put(file, new StringBuilder(text));
			}
			
			eventhub.publish(new SaveFileEvent(request, file));
			messages = checkLoadedFiles("saved");
		}
		
		return messages;
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
	
	public int lspDidChangeWatchedFile(RPCRequest request, File file, WatchKind type) throws Exception
	{
		FilenameFilter filter = getFilenameFilter();
		int actionCode = DO_NOTHING;
		
		/**
		 * This is a kludge to avoid loops caused by the build clearing the extracted files
		 * generating changes that cause more builds. The list is set in removeExtractedFiles,
		 * and cleared here once an event has been received.
		 */
		if (ignoreChangesList.contains(file))
		{
			Diag.info("Suppressing %s event for %s", type, file);
			ignoreChangesList.remove(file);
			return DO_NOTHING;
		}
		
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
				else if (file.equals(new File(rootUri, EXTERNALS)))
				{
					Diag.info("Created externals file, rebuilding");
					actionCode = RELOAD_AND_CHECK;
				}
				else if (file.equals(new File(rootUri, VDMIGNORE)))
				{
					Diag.info("Created vdmignore file, rebuilding");
					actionCode = RELOAD_AND_CHECK;
				}
				else if (onDotPath(file))
				{
					Diag.info("Ignoring file on dot path: %s", file);
					actionCode = DO_NOTHING;
				}
				else if (ignoredFile(file))
				{
					Diag.info("Ignoring %s file in vdmignore", file);
					actionCode = DO_NOTHING;			
				}
				else if (isExternalFile(file))
				{
					Diag.info("Created new external file: %s", file);
					actionCode = RELOAD_AND_CHECK;
				}
				else if (isExtractedFile(file))
				{
					Diag.info("Created new extracted file: %s", file);
					actionCode = DO_NOTHING;	// Created by a build anyway
				}
				else if (!filter.accept(file.getParentFile(), file.getName()))
				{
					Diag.info("Ignoring non-project file: %s", file);
					actionCode = DO_NOTHING;
				}
				else if (!projectFiles.containsKey(file))	
				{
					if (hasOrderedFiles)
					{
						Diag.error("File not in ordering list: %s", file);
						sendMessage(WARNING_MSG, "Ordering file out of date? " + file);
						actionCode = DO_NOTHING;
					}
					else
					{
						Diag.info("Created new file: %s", file);
						actionCode = RELOAD_AND_CHECK;
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
				else if (file.equals(new File(rootUri, EXTERNALS)))
				{
					Diag.info("Changed externals file, rebuilding");
					actionCode = RELOAD_AND_CHECK;
				}
				else if (file.equals(new File(rootUri, VDMIGNORE)))
				{
					Diag.info("Changed vdmignore file, rebuilding");
					actionCode = RELOAD_AND_CHECK;
				}
				else if (onDotPath(file))
				{
					Diag.info("Ignoring changed file on dot path: %s", file);
					actionCode = DO_NOTHING;
				}
				else if (ignoredFile(file))
				{
					Diag.info("Ignoring changed %s file in vdmignore", file);
					actionCode = DO_NOTHING;			
				}
				else if (isExternalFile(file))
				{
					Diag.info("Updated external file: %s", file);
					actionCode = RELOAD_AND_CHECK;
				}
				else if (isExtractedFile(file))
				{
					Diag.info("Updated extracted file: %s", file);
					actionCode = RECHECK;
				}
				else if (!filter.accept(file.getParentFile(), file.getName()))
				{
					Diag.info("Ignoring non-project file change: %s", file);
					actionCode = DO_NOTHING;
				}
				else if (!projectFiles.containsKey(file))	
				{
					Diag.error("Changed file not known: %s", file);
					actionCode = RELOAD_AND_CHECK;	// Try rebuilding?
				}
				else
				{
					Diag.info("Simple file change: %s", file);
					actionCode = RECHECK;
				}
				break;
				
			case DELETE:
				// Since the file is deleted, we can't access any file attributes, so we
				// just check for whether it is ignored/dotpath.
				if (onDotPath(file))
				{
					Diag.info("Ignoring deleted file on dot path: %s", file);
					actionCode = DO_NOTHING;
				}
				else if (ignoredFile(file))
				{
					Diag.info("Ignoring deleted file in vdmignore: %s", file);
					actionCode = DO_NOTHING;			
				}
				else if (isExtractedFile(file))
				{
					Diag.info("Deleted extracted file: %s", file);
					actionCode = RELOAD_AND_CHECK;	// Could be a user delete!
				}
				else
				{
					Diag.info("Deleted %s (dir/file?), rebuilding", file);
					actionCode = RELOAD_AND_CHECK;
				}
				break;
		}
		
		return actionCode;
	}

	public RPCMessageList afterChangeWatchedFiles(RPCRequest request, int actionCode, List<File> deleted) throws Exception
	{
		if (actionCode == RELOAD_AND_CHECK)
		{
			loadAllProjectFiles();
		}
		
		RPCMessageList results = null;
		
		if (actionCode == RELOAD_AND_CHECK || actionCode == RECHECK)
		{
			results = checkLoadedFiles("after change watched");
			
			if (hasClientCapability("workspace.codeLens.refreshSupport") ||
				hasClientCapability("experimental.codeLens.refreshSupport"))
			{
				results.add(RPCRequest.create("workspace/codeLens/refresh", null));
			}
			
			// Send publishDiagnostics for any deleted files, to clear messages
			
			if (deleted != null)
			{
				for (File file: deleted)
				{
					if (isExternalFile(file))
					{
						file = getExtractedName(file);	// Messages reported against extract
					}
					
					JSONObject params = new JSONObject("uri", file.toURI().toString(), "diagnostics", new JSONArray());
					results.add(RPCRequest.notification("textDocument/publishDiagnostics", params));
				}
			}
		}
		
		return results;		// Can be null, if DO_NOTHING
	}

	public RPCMessageList lspDefinition(RPCRequest request, File file, long zline, long zcol)
	{
		if (onDotPath(file) || ignoredFile(file))
		{
			return new RPCMessageList(request, null);
		}
		
		TCDefinition def = findDefinition(file, zline, zcol);
		
		if (def == null)
		{
			if (messagehub.hasErrors())
			{
				sendMessage(WARNING_MSG, "Specification contains errors. Cannot locate symbols.");
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

	public RPCMessageList lspReferences(RPCRequest request, File file, long zline, long zcol, Boolean incdec)
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
							JSONObject start = range.get("start");
							long zline2 = start.get("line");
							long zcol2  = start.get("character");

							TCDefinition def2 = findDefinition(pfile, zline2, zcol2);
							
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

	public RPCMessageList lspPrepareTypeHierarchy(RPCRequest request, File file, long zline, long zcol)
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
			return new RPCMessageList(request, msgutils.typeHierarchyItem(cdef));
		}
		else
		{
			Diag.info("Type hierarchy request isn't for class object");
			return new RPCMessageList(request, null);
		}
	}

	public RPCMessageList lspSupertypes(RPCRequest request, String classname)
	{
		TCPlugin tc = registry.getPlugin("TC");
		TCClassList results = tc.getTypeHierarchy(classname, false);
		return new RPCMessageList(request, msgutils.typeHierarchyItems(results));
	}

	public RPCMessageList lspSubtypes(RPCRequest request, String classname)
	{
		TCPlugin tc = registry.getPlugin("TC");
		TCClassList results = tc.getTypeHierarchy(classname, true);
		return new RPCMessageList(request, msgutils.typeHierarchyItems(results));
	}

	public RPCMessageList lspCompletion(RPCRequest request,
			CompletionTriggerKind triggerKind, File file, long zline, long zcol)
	{
		if (onDotPath(file) || ignoredFile(file))
		{
			return new RPCMessageList(request, new JSONArray());
		}
		
		Map<String, JSONObject> labels = new HashMap<String, JSONObject>();
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
					TCPlugin plugin = registry.getPlugin("TC");
					TCDefinitionList startingWith = plugin.lookupDefinition(word);
					
					for (TCDefinition defn: startingWith)
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

	public RPCMessageList lspDocumentSymbols(RPCRequest request, File file)
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
			 Utils.fixRanges(results, Utils.afterLine(Utils.getEndPosition(buffer)));
		}
		
		return new RPCMessageList(request, results);
	}
	
	public RPCMessageList lspCodeLens(RPCRequest request, File file)
	{
		if (onDotPath(file) || ignoredFile(file))
		{
			return new RPCMessageList(request, new JSONArray());
		}
		
		RPCMessageList responses = eventhub.publish(new CodeLensEvent(request, file));
		
		// We have to combine all the plugin lens responses into one.
		JSONArray lenses = new JSONArray();
		
		for (JSONObject lens: responses)
		{
			lenses.addAll(lens.get("result"));
		}
		
		return new RPCMessageList(request, lenses);
	}

	public RPCMessageList lspCodeLensResolve(RPCRequest request, JSONObject data)
	{
		return new RPCMessageList(request);
	}
	
	private TCDefinition findDefinition(File file, long zline, long zcol)
	{
		TCPlugin plugin = registry.getPlugin("TC");
		return plugin.findDefinition(file, (int)zline, (int)zcol);
	}

	private FilenameFilter getFilenameFilter()
	{
		ASTPlugin ast = registry.getPlugin("AST");
		return ast.getFilenameFilter();
	}

	private static final long ERROR_MSG = 1L;
	private static final long WARNING_MSG = 2L;
	
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
	
	public RPCMessageList lspShutdown(RPCRequest request)
	{
		Diag.info("Shutting down server");
		eventhub.publish(new ShutdownEvent(request));
		LSPServer.getInstance().setInitialized(false);
		removeExtractedFiles();
		reset();	// Clear registry, eventhub and messagehub
		
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
