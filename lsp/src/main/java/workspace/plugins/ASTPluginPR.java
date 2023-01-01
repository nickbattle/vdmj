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
import workspace.LSPWorkspaceManager;
import workspace.events.CheckPrepareEvent;
import workspace.events.CheckSyntaxEvent;
import workspace.lenses.ASTCodeLens;

public class ASTPluginPR extends ASTPlugin
{
	private ASTClassList astClassList = null;
	private ASTClassList dirtyClassList = null;
	
	public ASTPluginPR()
	{
		super();
	}
	
	@Override
	protected void preCheck(CheckPrepareEvent ev)
	{
		super.preCheck(ev);
		astClassList = new ASTClassList();
	}
	
	@Override
	public void checkLoadedFiles(CheckSyntaxEvent event)
	{
		dirty = false;
		dirtyClassList = null;
		hasErrs = false;
		
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
				return;
			}
		}
		
		for (Entry<File, StringBuilder> entry: projectFiles.entrySet())
		{
			LexTokenReader ltr = new LexTokenReader(entry.getValue().toString(), Settings.dialect, entry.getKey());
			ClassReader mr = new ClassReader(ltr);
			astClassList.addAll(mr.readClasses());
			
			if (mr.getErrorCount() > 0)
			{
				messagehub.addPluginMessages(this, mr.getErrors());
				hasErrs = true;
			}
			
			if (mr.getWarningCount() > 0)
			{
				messagehub.addPluginMessages(this, mr.getWarnings());
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
		
		if (hasErrs) event.setErrors();
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

		List<VDMMessage> messages = new Vector<VDMMessage>();
		Map<File, StringBuilder> projectFiles = LSPWorkspaceManager.getInstance().getProjectFiles();
		StringBuilder buffer = projectFiles.get(file);
		
		LexTokenReader ltr = new LexTokenReader(buffer.toString(), Settings.dialect, file);
		ClassReader cr = new ClassReader(ltr);
		dirtyClassList = cr.readClasses();
		
		if (cr.getErrorCount() > 0)
		{
			messages.addAll(cr.getErrors());
		}
		
		if (cr.getWarningCount() > 0)
		{
			messages.addAll(cr.getWarnings());
		}

		return messages;
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
	public JSONArray getCodeLenses(File file)
	{
		JSONArray results = new JSONArray();
		ASTPlugin ast = registry.getPlugin("AST");
		
		if (dirtyClassList != null && !dirtyClassList.isEmpty())	// May be syntax errors
		{
			List<ASTCodeLens> lenses = getASTCodeLenses(ast.isDirty());
			
			for (ASTClassDefinition clazz: dirtyClassList)
			{
				if (clazz.name.location.file.equals(file))
				{
					for (ASTDefinition def: clazz.definitions)
					{
						if (def.location.file.equals(file))
						{
							for (ASTCodeLens lens: lenses)
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
