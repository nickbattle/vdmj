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

package workspace.plugins;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import com.fujitsu.vdmj.RemoteSimulation;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.definitions.ASTBUSClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTCPUClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.syntax.ClassReader;

import json.JSONArray;
import lsp.textdocument.SymbolKind;
import workspace.Diag;
import workspace.DiagUtils;
import workspace.LSPWorkspaceManager;
import workspace.lenses.CodeLens;

public class ASTPluginPR extends ASTPlugin
{
	private ASTClassList astClassList = null;
	private ASTClassList dirtyClassList = null;
	
	public ASTPluginPR()
	{
		super();
	}
	
	@Override
	public void preCheck()
	{
		super.preCheck();
		astClassList = new ASTClassList();
	}
	
	@Override
	public boolean checkLoadedFiles()
	{
		dirty = false;
		dirtyClassList = null;
		
		Map<File, StringBuilder> projectFiles = LSPWorkspaceManager.getInstance().getProjectFiles();
		LexLocation.resetLocations();
		
		if (Settings.dialect == Dialect.VDM_RT)
		{
			try
			{
				// Add CPU and BUS up front, to be overwritten if they are explicitly defined
				astClassList.add(new ASTCPUClassDefinition());
				astClassList.add(new ASTBUSClassDefinition());
			}
			catch (Exception e)
			{
				Diag.error(e);
				return false;
			}
		}
		
		for (Entry<File, StringBuilder> entry: projectFiles.entrySet())
		{
			LexTokenReader ltr = new LexTokenReader(entry.getValue().toString(), Settings.dialect, entry.getKey(), "UTF-8");
			ClassReader mr = new ClassReader(ltr);
			astClassList.addAll(mr.readClasses());
			
			if (mr.getErrorCount() > 0)
			{
				errs.addAll(mr.getErrors());
			}
			
			if (mr.getWarningCount() > 0)
			{
				warns.addAll(mr.getWarnings());
			}
		}
		
		String remoteSimulation = System.getProperty("lsp.remoteSimulation");
		
		if (remoteSimulation != null)
		{
			RemoteSimulation simulation = RemoteSimulation.getInstance();
			
			if (simulation == null)
			{
				try
				{
					@SuppressWarnings("unchecked")
					Class<RemoteSimulation> clazz = (Class<RemoteSimulation>) Class.forName(remoteSimulation);
					simulation = clazz.getDeclaredConstructor().newInstance();
				}
				catch (Exception e)
				{
					Diag.error(e);
					Diag.error("Error while creating %s", remoteSimulation);
				}
			}
			
			Diag.info("Calling remoteSimulation setup %s", remoteSimulation);
			simulation.setup(astClassList);
		}
		
		return errs.isEmpty();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Mappable> T getAST()
	{
		return (T)astClassList;
	}
	
	@Override
	protected List<VDMMessage> parseFile(File file)
	{
		dirty = true;	// Until saved.

		List<VDMMessage> errs = new Vector<VDMMessage>();
		Map<File, StringBuilder> projectFiles = LSPWorkspaceManager.getInstance().getProjectFiles();
		StringBuilder buffer = projectFiles.get(file);
		
		LexTokenReader ltr = new LexTokenReader(buffer.toString(), Settings.dialect, file, "UTF-8");
		ClassReader cr = new ClassReader(ltr);
		dirtyClassList = cr.readClasses();
		
		if (cr.getErrorCount() > 0)
		{
			errs.addAll(cr.getErrors());
		}
		
		if (cr.getWarningCount() > 0)
		{
			errs.addAll(cr.getWarnings());
		}

		DiagUtils.dump(errs);
		return errs;
	}

	@Override
	public JSONArray documentSymbols(File file)
	{
		JSONArray results = new JSONArray();

		if (!astClassList.isEmpty())	// May be syntax errors
		{
			for (ASTClassDefinition clazz: astClassList)
			{
				if (clazz.name.location.file.equals(file))
				{
					results.add(messages.documentSymbol(
							clazz.name.getName(),
							"",
							SymbolKind.Class,
							clazz.name.location,
							clazz.name.location,
							documentSymbols(clazz.definitions)));
				}
			}
		}
		
		return results;
	}
	
	@Override
	public FilenameFilter getFilenameFilter()
	{
		return Settings.dialect.getFilter();
	}
	
	@Override
	public String[] getFilenameFilters()
	{
		if (Settings.dialect == Dialect.VDM_RT)
		{
			return new String[] { "**/*.vpp", "**/*.vdmrt" };
		}
		else
		{
			return new String[] { "**/*.vpp", "**/*.vdmpp" };
		}
	}

	@Override
	public JSONArray applyCodeLenses(File file, boolean dirty)
	{
		JSONArray results = new JSONArray();
		
		if (dirtyClassList != null && !dirtyClassList.isEmpty())	// May be syntax errors
		{
			List<CodeLens> lenses = getCodeLenses(dirty);
			
			for (ASTClassDefinition clazz: dirtyClassList)
			{
				if (clazz.name.location.file.equals(file))
				{
					for (ASTDefinition def: clazz.definitions)
					{
						if (def.location.file.equals(file))
						{
							for (CodeLens lens: lenses)
							{
								results.addAll(lens.getDefinitionLenses(def, clazz));
							}
						}
					}
				}
			}
		}
		
		return results;
	}
}
