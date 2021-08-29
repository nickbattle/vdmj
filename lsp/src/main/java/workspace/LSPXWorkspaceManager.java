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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.runtime.SourceFile;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.traces.TraceReductionType;

import json.JSONArray;
import json.JSONObject;
import lsp.LSPException;
import lsp.Utils;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.plugins.ASTPlugin;
import workspace.plugins.CTPlugin;
import workspace.plugins.CTPluginPR;
import workspace.plugins.CTPluginSL;
import workspace.plugins.POPlugin;
import workspace.plugins.POPluginPR;
import workspace.plugins.POPluginSL;
import workspace.plugins.TCPlugin;

public class LSPXWorkspaceManager
{
	private static LSPXWorkspaceManager INSTANCE = null;
	protected final PluginRegistry registry;
	
	protected LSPXWorkspaceManager()
	{
		this.registry = PluginRegistry.getInstance();
	}

	public static synchronized LSPXWorkspaceManager getInstance()
	{
		if (INSTANCE == null)
		{
			PluginRegistry _registry = PluginRegistry.getInstance();
			INSTANCE = new LSPXWorkspaceManager();
			
			switch (Settings.dialect)
			{
				case VDM_SL:
					_registry.registerPlugin(new POPluginSL());
					_registry.registerPlugin(new CTPluginSL());
					break;
					
				case VDM_PP:
				case VDM_RT:
					_registry.registerPlugin(new POPluginPR());
					_registry.registerPlugin(new CTPluginPR());
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
			TCPlugin tc = registry.getPlugin("TC");
			
			if (!tc.getErrs().isEmpty())	// No type clean tree
			{
				return new RPCMessageList(request, RPCErrors.InvalidRequest, "Type checking errors found");
			}
			
			POPlugin po = registry.getPlugin("PO");
			JSONArray results = po.getObligations(file);
			return new RPCMessageList(request, results);
		}
		catch (Exception e)
		{
			Log.error(e);
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
			Log.error(e);
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
			Log.error(e);
			return new RPCMessageList(request, e.getError(), e.getMessage());
		}
		catch (Exception e)
		{
			Log.error(e);
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
				Log.error("The spec has changed since generate, so re-generating");
				ct.generate(tracename);
			}
			
			JSONArray batch = ct.execute(request, tracename, progressToken, workDoneToken,
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
			Log.error(e);
			return new RPCMessageList(request, e.getError(), e.getMessage());
		}
		catch (Exception e)
		{
			Log.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}
	
	private boolean specHasErrors()
	{
		ASTPlugin ast = registry.getPlugin("AST");
		TCPlugin tc = registry.getPlugin("TC");
		
		return !ast.getErrs().isEmpty() || !tc.getErrs().isEmpty();
	}

	public RPCMessageList translateLaTeX(RPCRequest request, File file, File saveUri)
	{
		File responseFile = null;
		LSPWorkspaceManager manager = LSPWorkspaceManager.getInstance();
		Map<File, StringBuilder> filemap = manager.getProjectFiles();

		try
		{
			if (file == null)	// translate whole project
			{
				for (File pfile: filemap.keySet())
				{
					fileToLaTeX(saveUri, pfile);
				}

				responseFile = saveUri;		// ??
			}
			else
			{
				if (filemap.containsKey(file))
				{
					responseFile = fileToLaTeX(saveUri, file);
				}
				else
				{
					return new RPCMessageList(request, RPCErrors.InvalidParams, "No such file in project");
				}
			}

			return new RPCMessageList(request, new JSONObject("uri", responseFile.toURI().toString()));
		}
		catch (IOException e)
		{
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}
	
	private File fileToLaTeX(File saveUri, File file) throws IOException
	{
		SourceFile source = new SourceFile(file);
		String texname = file.getName().replaceAll("\\.vdm..$", ".tex");
		File outfile = new File(saveUri, texname);
		
		PrintWriter out = new PrintWriter(outfile);
		source.printLatexCoverage(out, true, true, false);
		out.close();

		return outfile;
	}

	public RPCMessageList translateWord(RPCRequest request, File file, File saveUri)
	{
		File responseFile = null;
		LSPWorkspaceManager manager = LSPWorkspaceManager.getInstance();
		Map<File, StringBuilder> filemap = manager.getProjectFiles();

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
			else
			{
				if (filemap.containsKey(file))
				{
					responseFile = fileToWord(saveUri, file);
				}
				else
				{
					return new RPCMessageList(request, RPCErrors.InvalidParams, "No such file in project");
				}
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
		String texname = file.getName().replaceAll("\\.vdm..$", ".doc");
		File outfile = new File(saveUri, texname);
		
		PrintWriter out = new PrintWriter(outfile);
		source.printWordCoverage(out, true, false);
		out.close();
		
		return  outfile;
	}
	
	public RPCMessageList translateCoverage(RPCRequest request, File file, File saveUri)
	{
		File responseFile = null;
		LSPWorkspaceManager manager = LSPWorkspaceManager.getInstance();
		Map<File, StringBuilder> filemap = manager.getProjectFiles();

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
			else
			{
				if (filemap.containsKey(file))
				{
					responseFile = fileToCoverage(saveUri, file);
				}
				else
				{
					return new RPCMessageList(request, RPCErrors.InvalidParams, "No such file in project");
				}
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
		String texname = file.getName() + ".covtbl";
		File outfile = new File(saveUri, texname);
		
		PrintWriter out = new PrintWriter(outfile);
		source.writeCoverage(out);
		out.close();

		return outfile;
	}
	
	public RPCMessageList translateDependencies(RPCRequest request, File file, File saveUri)
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
