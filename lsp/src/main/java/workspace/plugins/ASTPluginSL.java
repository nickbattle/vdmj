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

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.modules.ASTModule;
import com.fujitsu.vdmj.ast.modules.ASTModuleList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.syntax.ModuleReader;

import json.JSONArray;
import lsp.textdocument.SymbolKind;
import workspace.events.CheckPrepareEvent;
import workspace.events.CheckSyntaxEvent;
import workspace.lenses.ASTCodeLens;

public class ASTPluginSL extends ASTPlugin
{
	private ASTModuleList astModuleList = null;
	private ASTModuleList dirtyModuleList = null;
	
	public ASTPluginSL()
	{
		super();
	}
	
	@Override
	protected void preCheck(CheckPrepareEvent ev)
	{
		super.preCheck(ev);
		astModuleList = new ASTModuleList();
	}
	
	@Override
	public void checkLoadedFiles(CheckSyntaxEvent event)
	{
		dirty = false;
		Map<File, StringBuilder> projectFiles = LSPPlugin.getInstance().getProjectFiles();
		LexLocation.resetLocations();
		
		for (Entry<File, StringBuilder> entry: projectFiles.entrySet())
		{
			LexTokenReader ltr = new LexTokenReader(entry.getValue().toString(), Dialect.VDM_SL, entry.getKey());
			ModuleReader mr = new ModuleReader(ltr);
			astModuleList.addAll(mr.readModules());
			
			if (mr.getErrorCount() > 0)
			{
				messagehub.addPluginMessages(this, mr.getErrors());
			}
			
			if (mr.getWarningCount() > 0)
			{
				messagehub.addPluginMessages(this, mr.getWarnings());
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T extends Mappable> T getAST()
	{
		return (T)astModuleList;
	}
	
	@Override
	protected void parseFile(File file)
	{
		dirty = true;	// Until saved.
		dirtyModuleList = null;

		Map<File, StringBuilder> projectFiles = LSPPlugin.getInstance().getProjectFiles();
		StringBuilder buffer = projectFiles.get(file);
		
		LexTokenReader ltr = new LexTokenReader(buffer.toString(), Settings.dialect, file);
		ModuleReader mr = new ModuleReader(ltr);
		dirtyModuleList = mr.readModules();
		
		if (mr.getErrorCount() > 0)
		{
			messagehub.addPluginMessages(this, mr.getErrors());
		}
		
		if (mr.getWarningCount() > 0)
		{
			messagehub.addPluginMessages(this, mr.getWarnings());
		}
	}
	
	@Override
	public JSONArray documentSymbols(File file)
	{
		JSONArray results = new JSONArray();
		
		if (!astModuleList.isEmpty())	// May be syntax errors
		{
			for (ASTModule module: astModuleList)
			{
				if (module.files.contains(file))
				{
					results.add(messages.documentSymbol(
							module.name.name,
							"",
							SymbolKind.Module,
							module.name.location,
							module.name.location,
							documentSymbols(module.defs)));
				}
			}
		}
		
		return results;
	}

	@Override
	public FilenameFilter getFilenameFilter()
	{
		return Dialect.VDM_SL.getFilter();
	}
	
	@Override
	public String[] getFilenameFilters()
	{
		return new String[] { "**/*.vdm", "**/*.vdmsl" }; 
	}

	@Override
	public JSONArray getCodeLenses(File file)
	{
		JSONArray results = new JSONArray();
		
		if (dirtyModuleList != null && !dirtyModuleList.isEmpty())
		{
			List<ASTCodeLens> lenses = getASTCodeLenses();
			
			for (ASTModule module: dirtyModuleList)
			{
				for (ASTDefinition def: module.defs)
				{
					if (def.location.file.equals(file))
					{
						for (ASTCodeLens lens: lenses)
						{
							results.addAll(lens.getDefinitionLenses(def, module));
						}
					}
				}
			}
		}
		
		return results;
	}
}
