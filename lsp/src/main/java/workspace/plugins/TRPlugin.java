/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package workspace.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.runtime.SourceFile;

import json.JSONObject;
import lsp.lspx.TranslateHandler;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.EventListener;
import workspace.LSPPlugin;

/**
 * The translate plugin.
 */
public class TRPlugin extends AnalysisPlugin implements EventListener
{
	private final LSPPlugin lspPlugin;
	
	public static AnalysisPlugin factory(Dialect dialect)
	{
		switch (dialect)
		{
			default:
				return new TRPlugin();
		}
	}
	
	private TRPlugin()
	{
		lspPlugin = registry.getPlugin("LSP");
	}
	
	@Override
	public int getPriority()
	{
		return EventListener.TR_PRIORITY;
	}

	@Override
	public String getName()
	{
		return "TR";
	}

	@Override
	public void init()
	{
		dispatcher.register(new TranslateHandler(), "slsp/TR/translate");
	}

	/**
	 * Create a subfolder within saveUri matching the folder part of file passed.
	 */
	private File getSubFolder(File saveUri, File file)
	{
		File root = lspPlugin.getRoot();
		
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
		Map<File, StringBuilder> filemap = lspPlugin.getProjectFiles();

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
				
				if (file.equals(lspPlugin.getRoot()))
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
		String project = lspPlugin.getRoot().getName();
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
		
        Path sourceBase = Paths.get(lspPlugin.getRoot().getAbsolutePath());
		
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
		Map<File, StringBuilder> filemap = lspPlugin.getProjectFiles();

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
		Map<File, StringBuilder> filemap = lspPlugin.getProjectFiles();

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
