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

package workspace.plugins;

import com.fujitsu.vdmj.in.modules.INModuleList;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import workspace.WorkspaceManager;
import workspace.WorkspacePlugin;

public class INPluginSL extends WorkspacePlugin
{
	private INModuleList inModuleList = null;
	
	public INPluginSL(WorkspaceManager manager)
	{
		super(manager);
	}
	
	@Override
	public String getName()
	{
		return "IN";
	}

	@Override
	public void init()
	{
	}

	public void preCheck()
	{
		inModuleList = new INModuleList();
	}
	
	public boolean checkLoadedFiles(TCModuleList tcModuleList) throws Exception
	{
		inModuleList = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(tcModuleList);
		return true;
	}
}
