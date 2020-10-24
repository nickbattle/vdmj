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

import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.definitions.POClassList;

import workspace.WorkspaceManager;

public class POPluginPR extends POPlugin
{
	private POClassList poClassList;

	public POPluginPR(WorkspaceManager manager)
	{
		super(manager);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getPO()
	{
		return (T) poClassList;
	}

	@Override
	public <T> boolean generate(T tcList) throws Exception
	{
		poClassList = ClassMapper.getInstance(PONode.MAPPINGS).init().convert(tcList);
		return true;
	}
}
