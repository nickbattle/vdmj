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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.runtime.SourceFile;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.traces.TraceReductionType;
import com.fujitsu.vdmj.util.GetResource;

import json.JSONArray;
import json.JSONObject;
import lsp.LSPException;
import lsp.Utils;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.events.UnknownMethodEvent;
import workspace.plugins.AnalysisPlugin;
import workspace.plugins.CTPlugin;
import workspace.plugins.POPlugin;
import workspace.plugins.TCPlugin;

public class LSPXWorkspaceManager
{
	private static LSPXWorkspaceManager INSTANCE = null;
	private final PluginRegistry registry;
	private final EventHub eventhub;
	private final MessageHub messagehub;
	private final LSPWorkspaceManager wsManager;
	
	protected LSPXWorkspaceManager()
	{
		this.registry = PluginRegistry.getInstance();
		this.eventhub = EventHub.getInstance();
		this.messagehub = MessageHub.getInstance();
		this.wsManager = LSPWorkspaceManager.getInstance();
	}

	public static synchronized LSPXWorkspaceManager getInstance()
	{
		if (INSTANCE == null)
		{
			INSTANCE = new LSPXWorkspaceManager();		
			Diag.info("Created LSPXWorkspaceManager");
		}

		return INSTANCE;
	}

	/**
	 * This is called after the client capabilities have been received. If the option
	 * is enabled in the capabilities, the relevant plugin is registered.
	 * 
	 * PO and CT are built-in, but still enabled by the capabilities.
	 * 
	 * Further plugins may be loaded via the property "lspx.plugins".
	 * @throws Exception 
	 */
	public void enablePlugins() throws Exception
	{
		if (wsManager.hasClientCapability("experimental.proofObligationGeneration"))
		{
			registry.registerPlugin(POPlugin.factory(Settings.dialect));
		}
		
		if (wsManager.hasClientCapability("experimental.combinatorialTesting"))
		{
			registry.registerPlugin(CTPlugin.factory(Settings.dialect));
		}
		
		List<String> plugins = GetResource.readResource("lspx.plugins");
		
		if (!plugins.isEmpty())
		{
			for (String plugin: plugins)
			{
				try
				{
					Class<?> clazz = Class.forName(plugin);
					
					try
					{
						Method factory = clazz.getMethod("factory", Dialect.class);
						AnalysisPlugin instance = (AnalysisPlugin)factory.invoke(null, Settings.dialect);
						registry.registerPlugin(instance);
						Diag.info("Registered LSPX plugin %s", plugin);
					}
					catch (NoSuchMethodException e)		// Try default constructor
					{
						try
						{
							if (Modifier.isAbstract(clazz.getModifiers()))
							{
								Diag.severe("Plugin class is abstract: %s", clazz.getName());
								continue;
							}

							Constructor<?> ctor = clazz.getConstructor();
							AnalysisPlugin instance = (AnalysisPlugin) ctor.newInstance();
							registry.registerPlugin(instance);
							Diag.info("Registered LSPX plugin %s", plugin);
						}
						catch (Throwable th)
						{
							Diag.error(th);
							Diag.error("Cannot register LSPX plugin %s", plugin);
						}
					}
					catch (Exception e)
					{
						Diag.error(e);
						Diag.error("Plugin %s factory method failed", plugin);
					}
				}
				catch (ClassNotFoundException e)
				{
					Diag.error("Plugin class %s not found", plugin);
				}
			}
		}
		else
		{
			Diag.info("No external plugins configured in lspx.plugins");
		}
	}

	public RPCMessageList unhandledMethod(RPCRequest request)
	{
		RPCMessageList results = eventhub.publish(new UnknownMethodEvent(request));
		
		if (results.isEmpty())
		{
			Diag.error("No external plugin registered for " + request.getMethod());
			return new RPCMessageList(request, RPCErrors.MethodNotFound, request.getMethod());
		}
		else
		{
			return results;
		}
	}

	/**
	 * Reset the singleton.
	 */
	public static void reset()
	{
		if (INSTANCE != null)
		{
			INSTANCE = null;
		}
	}
	
	/**
	 * LSPX extensions...
	 */

	public RPCMessageList pogGenerate(RPCRequest request, File file)
	{
		try
		{
			if (messagehub.hasErrors())	// No clean tree
			{
				return new RPCMessageList(request, RPCErrors.InvalidRequest, "Specification errors found");
			}
			
			POPlugin po = registry.getPlugin("PO");
			return po.getJSONObligations(request, file);
		}
		catch (Exception e)
		{
			Diag.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}

	public RPCMessageList ctTraces(RPCRequest request, File project)
	{
		try
		{
			if (specHasErrors())
			{
				return new RPCMessageList(request, RPCErrors.ParseError, "Specification has errors");
			}
			
			DAPWorkspaceManager.getInstance().refreshInterpreter();
			CTPlugin ct = registry.getPlugin("CT");
			Map<String, TCNameList> nameMap = ct.getTraceNames();
			JSONArray results = new JSONArray();
			
			for (String module: nameMap.keySet())
			{
				JSONArray array = new JSONArray();
				
				for (TCNameToken name: nameMap.get(module))
				{
					array.add(new JSONObject(
						"name",		name.getExplicit(true).toString(),
						"location",	Utils.lexLocationToLocation(name.getLocation())));
				}
				
				results.add(new JSONObject("name", module, "traces", array));
			}
			
			return new RPCMessageList(request, results);
		}
		catch (Exception e)
		{
			Diag.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}

	public RPCMessageList ctGenerate(RPCRequest request, String name)
	{
		try
		{
			if (specHasErrors())
			{
				return new RPCMessageList(request, RPCErrors.ParseError, "Specification has errors");
			}
			
			CTPlugin ct = registry.getPlugin("CT");
			
			if (ct.isRunning())
			{
				return new RPCMessageList(request, RPCErrors.InvalidRequest, "Trace still running");
			}
	
			DAPWorkspaceManager.getInstance().refreshInterpreter();
			TCNameToken tracename = Utils.stringToName(name);
			int count = ct.generate(tracename);
			return new RPCMessageList(request, new JSONObject("numberOfTests", count));
		}
		catch (LSPException e)
		{
			Diag.error(e);
			return new RPCMessageList(request, e.getError(), e.getMessage());
		}
		catch (Exception e)
		{
			Diag.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}

	public RPCMessageList ctExecute(RPCRequest request, String name,
			Object progressToken, Object workDoneToken,
			TraceReductionType rType, float subset, long seed, Long start, Long end)
	{
		try
		{
			if (specHasErrors())
			{
				return new RPCMessageList(request, RPCErrors.ParseError, "Specification has errors");
			}

			TCNameToken tracename = Utils.stringToName(name);
			CTPlugin ct = registry.getPlugin("CT");

			if (ct.isRunning())
			{
				return new RPCMessageList(request, RPCErrors.InvalidRequest, "Trace still running");
			}

			if (DAPWorkspaceManager.getInstance().refreshInterpreter())
			{
				Diag.error("The spec has changed since generate, so re-generating");
				ct.generate(tracename);
			}
			
			JSONArray batch = ct.runTraceRange(request, tracename, progressToken, workDoneToken,
					rType, subset, seed, start, end);
			
			if (batch == null)	// Running in background
			{
				return null;
			}
			else
			{
				return new RPCMessageList(request, batch);
			}
		}
		catch (LSPException e)
		{
			Diag.error(e);
			return new RPCMessageList(request, e.getError(), e.getMessage());
		}
		catch (Exception e)
		{
			Diag.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}
	
	private boolean specHasErrors()
	{
		return messagehub.hasErrors();
	}

	/**
	 * Create a subfolder within saveUri matching the folder part of file passed.
	 */
	private File getSubFolder(File saveUri, File file)
	{
		File root = wsManager.getRoot();
		
		if (file.getParent().startsWith(root.getPath()))
		{
			int r = root.getPath().length();
			return new File(saveUri.getPath() + File.separator + file.getParent().substring(r));
		}
		else
		{
			return saveUri;		// Should never happen?
		}
	}

	public RPCMessageList translateLaTeX(RPCRequest request, File file, File saveUri, JSONObject options)
	{
		File responseFile = null;
		Map<File, StringBuilder> filemap = wsManager.getProjectFiles();

		try
		{
			if (file == null)	// translate whole project
			{
				for (File pfile: filemap.keySet())
				{
					fileToLaTeX(saveUri, pfile, options);
				}
				
				createLaTeXDocument(saveUri, filemap.keySet());

				responseFile = saveUri;		// ??
			}
			else if (file.isDirectory())
			{
				String subfolder = file.getPath();
				
				for (File pfile: filemap.keySet())
				{
					if (pfile.getPath().startsWith(subfolder))
					{
						fileToLaTeX(saveUri, pfile, options);
					}
				}
				
				if (file.equals(wsManager.getRoot()))
				{
					createLaTeXDocument(saveUri, filemap.keySet());
				}

				responseFile = file;		// ??
			}
			else if (filemap.containsKey(file))
			{
				responseFile = fileToLaTeX(saveUri, file, options);
			}
			else
			{
				return new RPCMessageList(request, RPCErrors.InvalidParams, "No such file in project");
			}

			return new RPCMessageList(request, new JSONObject("uri", responseFile.toURI().toString()));
		}
		catch (IOException e)
		{
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}
	
	private void createLaTeXDocument(File saveUri, Set<File> sources) throws IOException
	{
		String project = wsManager.getRoot().getName();
		File document = new File(saveUri, project + ".tex");
		PrintWriter out = new PrintWriter(new FileOutputStream(document));
		
		out.println("\\documentclass{article}");
		out.println("\\usepackage{fullpage}"); 
		out.println("\\usepackage[color]{vdmlisting}"); 
		out.println("\\usepackage[hidelinks]{hyperref} "); 
		out.println("\\usepackage{longtable}"); 
		out.println("\\begin{document}"); 
		out.println("\\title{" + project + "}"); 
		out.println("\\author{}"); 
		out.println("\\maketitle"); 
		out.println("\\tableofcontents");
		out.println();
		
        Path sourceBase = Paths.get(wsManager.getRoot().getAbsolutePath());
		
		for (File pfile: sources)
		{
	        Path pathSource = Paths.get(pfile.getAbsolutePath());
	        File relativeParent = sourceBase.relativize(pathSource).toFile().getParentFile();

	        String section = pfile.getName().replaceAll("\\.vdm..$", "");
			String texname = section + ".tex";
			File outfile = relativeParent == null ? new File(texname) : new File(relativeParent, texname);
			
			out.println("\\section{" + section + "}"); 
			out.println("\\input{" + outfile + "}");
			out.println();
		}
		
		out.println("\\end{document}");
		out.close();
	}

	private File fileToLaTeX(File saveUri, File file, JSONObject options) throws IOException
	{
		boolean modelOnly = false;
		boolean markCoverage = false;
		boolean insertCoverageTables = false;

		if (options != null)
		{
			if (options.containsKey("modelOnly"))
			{
				modelOnly = options.get("modelOnly");
			}
			
			if (options.containsKey("markCoverage"))
			{
				markCoverage = options.get("markCoverage");
			}
			
			if (options.containsKey("insertCoverageTables"))
			{
				insertCoverageTables = options.get("insertCoverageTables");
			}
		}

		SourceFile source = new SourceFile(file);
		String texname = file.getName().replaceAll("\\.vdm..$", ".tex");
		File subfolder = getSubFolder(saveUri, file);
		subfolder.mkdirs();
		File outfile = new File(subfolder, texname);
		
		PrintWriter out = new PrintWriter(outfile);
		source.printLatexCoverage(out, true, modelOnly, markCoverage, insertCoverageTables);
		out.close();

		return outfile;
	}

	public RPCMessageList translateWord(RPCRequest request, File file, File saveUri, JSONObject options)
	{
		File responseFile = null;
		Map<File, StringBuilder> filemap = wsManager.getProjectFiles();

		try
		{
			if (file == null)	// translate whole project
			{
				for (File pfile: filemap.keySet())
				{
					fileToWord(saveUri, pfile);
				}

				responseFile = saveUri;		// ??
			}
			else if (file.isDirectory())
			{
				String subfolder = file.getPath();
				
				for (File pfile: filemap.keySet())
				{
					if (pfile.getPath().startsWith(subfolder))
					{
						fileToWord(saveUri, pfile);
					}
				}

				responseFile = file;		// ??
			}
			else if (filemap.containsKey(file))
			{
				responseFile = fileToWord(saveUri, file);
			}
			else
			{
				return new RPCMessageList(request, RPCErrors.InvalidParams, "No such file in project");
			}

			return new RPCMessageList(request, new JSONObject("uri", responseFile.toURI().toString()));
		}
		catch (IOException e)
		{
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}

	private File fileToWord(File saveUri, File file) throws IOException
	{
		SourceFile source = new SourceFile(file);
		String wordname = file.getName().replaceAll("\\.vdm..$", ".doc");
		File subfolder = getSubFolder(saveUri, file);
		subfolder.mkdirs();
		File outfile = new File(subfolder, wordname);
		
		PrintWriter out = new PrintWriter(outfile);
		source.printWordCoverage(out, true, false);
		out.close();
		
		return  outfile;
	}
	
	public RPCMessageList translateCoverage(RPCRequest request, File file, File saveUri, JSONObject options)
	{
		File responseFile = null;
		Map<File, StringBuilder> filemap = wsManager.getProjectFiles();

		try
		{
			if (file == null)	// translate whole project
			{
				for (File pfile: filemap.keySet())
				{
					fileToCoverage(saveUri, pfile);
				}

				responseFile = saveUri;		// ??
			}
			else if (file.isDirectory())
			{
				String subfolder = file.getPath();
				
				for (File pfile: filemap.keySet())
				{
					if (pfile.getPath().startsWith(subfolder))
					{
						fileToCoverage(saveUri, pfile);
					}
				}

				responseFile = file;		// ??
			}
			else if (filemap.containsKey(file))
			{
				responseFile = fileToCoverage(saveUri, file);
			}
			else
			{
				return new RPCMessageList(request, RPCErrors.InvalidParams, "No such file in project");
			}

			return new RPCMessageList(request, new JSONObject("uri", responseFile.toURI().toString()));
		}
		catch (IOException e)
		{
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}

	private File fileToCoverage(File saveUri, File file) throws IOException
	{
		SourceFile source = new SourceFile(file);
		String covname = file.getName() + ".covtbl";
		File subfolder = getSubFolder(saveUri, file);
		subfolder.mkdirs();
		File outfile = new File(subfolder, covname);
		
		PrintWriter out = new PrintWriter(outfile);
		source.writeCoverage(out, true);
		out.close();

		return outfile;
	}
	
	public RPCMessageList translateGraphviz(RPCRequest request, File file, File saveUri, JSONObject options)
	{
		try
		{
			TCPlugin tc = registry.getPlugin("TC");
			File result = new File(saveUri, "dependencies.dot");
			tc.saveDependencies(result);
			return new RPCMessageList(request, new JSONObject("uri", result.toURI().toString()));
		}
		catch (IOException e)
		{
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}
}
