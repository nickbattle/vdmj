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
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

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
import workspace.LSPWorkspaceManager;
import workspace.Log;
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
				Log.error(e);
				return false;
			}
		}
		
		for (Entry<File, StringBuilder> entry: projectFiles.entrySet())
		{
			LexTokenReader ltr = new LexTokenReader(entry.getValue().toString(),
					Settings.dialect, entry.getKey(), Charset.defaultCharset().displayName());
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
		
		LexTokenReader ltr = new LexTokenReader(buffer.toString(),
				Settings.dialect, file, Charset.defaultCharset().displayName());
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

		Log.dump(errs);
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
	public JSONArray documentLenses(File file)
	{
		JSONArray results = new JSONArray();
		
		if (dirtyClassList != null && !dirtyClassList.isEmpty())	// May be syntax errors
		{
			for (ASTClassDefinition clazz: dirtyClassList)
			{
				if (clazz.name.location.file.equals(file))
				{
					for (ASTDefinition def: clazz.definitions)
					{
						if (def.location.file.equals(file))
						{
							for (CodeLens lens: CodeLens.getLenses())
							{
								results.addAll(lens.codeLenses(def, file));
							}
						}
					}
				}
			}
		}
		
		return results;
	}
}
