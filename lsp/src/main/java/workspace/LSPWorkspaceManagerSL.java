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

import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.modules.TCModuleList;

import vdmj.LSPDefinitionFinder;
import workspace.plugins.ASTPluginSL;
import workspace.plugins.INPluginSL;
import workspace.plugins.TCPlugin;
import workspace.plugins.TCPluginSL;

public class LSPWorkspaceManagerSL extends LSPWorkspaceManager
{
	public LSPWorkspaceManagerSL()
	{
		registry.registerPlugin(new ASTPluginSL(this));
		registry.registerPlugin(new TCPluginSL(this));
		registry.registerPlugin(new INPluginSL(this));
	}
	
	@Override
	protected TCDefinition findDefinition(File file, int zline, int zcol)
	{
		TCPlugin plugin = registry.getPlugin("TC");
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
	protected TCDefinitionList lookupDefinition(String startsWith)
	{
		TCPlugin plugin = registry.getPlugin("TC");
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
}
