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
import java.net.URI;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.in.modules.INModuleList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.runtime.ModuleInterpreter;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONArray;
import json.JSONObject;
import lsp.Utils;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import vdmj.LSPDefinitionFinder;
import vdmj.LSPDefinitionFinder.Found;
import workspace.plugins.ASTPluginSL;
import workspace.plugins.INPlugin;
import workspace.plugins.INPluginSL;
import workspace.plugins.POPluginSL;
import workspace.plugins.TCPlugin;
import workspace.plugins.TCPluginSL;

public class WorkspaceManagerSL extends WorkspaceManager
{
	public WorkspaceManagerSL()
	{
		Settings.dialect = Dialect.VDM_SL;
		registerPlugin(new ASTPluginSL(this));
		registerPlugin(new TCPluginSL(this));
		registerPlugin(new INPluginSL(this));
		registerPlugin(new POPluginSL(this));
	}

	@Override
	protected TCNode findLocation(File file, int zline, int zcol)
	{
		TCPlugin plugin = getPlugin("TC");
		TCModuleList tcModuleList = plugin.getTC();
		
		if (tcModuleList != null && !tcModuleList.isEmpty())
		{
			LSPDefinitionFinder finder = new LSPDefinitionFinder();
			Found found = finder.findLocation(tcModuleList, file, zline + 1, zcol + 1);
			
			if (found != null)
			{
				return found.node;
			}
		}

		return null;
	}

	@Override
	protected TCDefinition findDefinition(File file, int zline, int zcol)
	{
		TCPlugin plugin = getPlugin("TC");
		TCModuleList tcModuleList = plugin.getTC();
		
		if (tcModuleList != null && !tcModuleList.isEmpty())
		{
			LSPDefinitionFinder finder = new LSPDefinitionFinder();
			return finder.findDefinition(tcModuleList, file, zline + 1, zcol + 1);
		}
		else
		{
			return null;
		}
	}

	@Override
	public RPCMessageList findDefinition(RPCRequest request, File file, int zline, int zcol)
	{
		TCDefinition def = findDefinition(file, zline, zcol);
		
		if (def == null)
		{
			return new RPCMessageList(request, null);
		}
		else
		{
			URI defuri = def.location.file.toURI();
			
			return new RPCMessageList(request,
				System.getProperty("lsp.lsp4e") != null ?
					new JSONArray(
						new JSONObject(
							"targetUri", defuri.toString(),
							"targetRange", Utils.lexLocationToRange(def.location),
							"targetSelectionRange", Utils.lexLocationToPoint(def.location)))
					:
					new JSONObject(
						"uri", defuri.toString(),
						"range", Utils.lexLocationToRange(def.location)));
		}
	}

	@Override
	protected TCDefinitionList lookupDefinition(String startsWith)
	{
		TCPlugin plugin = getPlugin("TC");
		TCModuleList tcModuleList = plugin.getTC();
		TCDefinitionList results = new TCDefinitionList();
		
		for (TCModule module: tcModuleList)
		{
			for (TCDefinition def: module.defs)
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
		return Dialect.VDM_SL.getFilter();
	}
	
	@Override
	protected String[] getFilenameFilters()
	{
		return new String[] { "**/*.vdm", "**/*.vdmsl" }; 
	}

	@Override
	public ModuleInterpreter getInterpreter()
	{
		if (interpreter == null)
		{
			try
			{
				TCPlugin plugin = getPlugin("TC");
				TCModuleList tcModuleList = plugin.getTC();
				INPlugin plugin2 = getPlugin("IN");
				INModuleList inModuleList = plugin2.getIN();
				interpreter = new ModuleInterpreter(inModuleList, tcModuleList);
			}
			catch (Exception e)
			{
				Log.error(e);
				interpreter = null;
			}
		}
		
		return (ModuleInterpreter) interpreter;
	}

	@Override
	protected boolean canExecute()
	{
		return getInterpreter() != null;	// inModuleList != null;
	}
	
	@Override
	protected boolean hasChanged()
	{
		INPlugin plugin = getPlugin("IN");
		INModuleList inModuleList = plugin.getIN();
		return getInterpreter() != null && getInterpreter().getIN() != inModuleList;	// TODO won't have changed??
	}
	
	@Override
	public DAPMessageList threads(DAPRequest request)
	{
		return new DAPMessageList(request, new JSONObject("threads", new JSONArray()));	// empty?
	}
}
