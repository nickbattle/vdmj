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
 *
 ******************************************************************************/

package workspace;

import java.io.File;
import java.io.FilenameFilter;

import com.fujitsu.vdmj.ast.definitions.ASTClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;

import json.JSONArray;
import lsp.textdocument.SymbolKind;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import vdmj.LSPDefinitionFinder;
import workspace.plugins.ASTPlugin;
import workspace.plugins.ASTPluginPR;
import workspace.plugins.INPluginPR;
import workspace.plugins.TCPlugin;
import workspace.plugins.TCPluginPR;

public class LSPWorkspaceManagerPP extends LSPWorkspaceManager
{
	public LSPWorkspaceManagerPP()
	{
		registry.registerPlugin(new ASTPluginPR(this));
		registry.registerPlugin(new TCPluginPR(this));
		registry.registerPlugin(new INPluginPR(this));
	}
	
	@Override
	public RPCMessageList documentSymbols(RPCRequest request, File file)
	{
		TCPlugin tc = registry.getPlugin("TC");
		TCClassList tcClassList = tc.getTC();
		JSONArray results = new JSONArray();
		
		if (tcClassList != null)	// May be syntax errors
		{
			for (TCClassDefinition clazz: tcClassList)
			{
				if (clazz.name.getLocation().file.equals(file))
				{
					results.add(messages.symbolInformation(clazz.name.toString(),
							clazz.name.getLocation(), SymbolKind.Class, null));

					for (TCDefinition def: clazz.definitions)
					{
						for (TCDefinition indef: def.getDefinitions())
						{
							results.add(messages.symbolInformation(indef.name.getName() + ":" + indef.getType(),
									indef.location, SymbolKind.kindOf(indef), indef.location.module));
						}
					}
				}
			}
		}
		else
		{
			ASTPlugin ast = registry.getPlugin("AST");
			ASTClassList astClassList = ast.getAST();

			if (astClassList != null)	// May be syntax errors
			{
				for (ASTClassDefinition clazz: astClassList)
				{
					if (clazz.name.location.file.equals(file))
					{
						results.add(messages.symbolInformation(clazz.name.toString(), clazz.location, SymbolKind.Class, null));

						for (ASTDefinition def: clazz.definitions)
						{
							if (def.name != null)
							{
								results.add(messages.symbolInformation(def.name.name, def.name.location,
										SymbolKind.kindOf(def), def.location.module));
							}
						}
					}
				}
			}
		}
		
		return new RPCMessageList(request, results);
	}

	@Override
	protected TCDefinition findDefinition(File file, int zline, int zcol)
	{
		TCPlugin plugin = registry.getPlugin("TC");
		TCClassList tcClassList = plugin.getTC();
		
		if (tcClassList != null && !tcClassList.isEmpty())
		{
			LSPDefinitionFinder finder = new LSPDefinitionFinder();
			return finder.findDefinition(tcClassList, file, zline + 1, zcol + 1);		// Convert from zero-relative
		}
		else
		{
			return null;
		}
	}

	@Override
	protected TCDefinitionList lookupDefinition(String startsWith)
	{
		TCPlugin plugin = registry.getPlugin("TC");
		TCClassList tcClassList = plugin.getTC();
		TCDefinitionList results = new TCDefinitionList();
		
		for (TCClassDefinition cdef: tcClassList)
		{
			if (cdef.name.getName().startsWith(startsWith))
			{
				results.add(cdef);	// Add classes as well
			}
			
			for (TCDefinition def: cdef.definitions)
			{
				if (def.name != null && def.name.getName().startsWith(startsWith))
				{
					results.add(def);
				}
			}
		}
		
		return results;
	}

	@Override
	protected FilenameFilter getFilenameFilter()
	{
		return Dialect.VDM_PP.getFilter();
	}
	
	@Override
	protected String[] getFilenameFilters()
	{
		return new String[] { "**/*.vpp", "**/*.vdmpp" }; 
	}
}
