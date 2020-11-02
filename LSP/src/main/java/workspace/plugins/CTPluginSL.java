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

import java.util.HashMap;
import java.util.Map;

import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.definitions.INNamedTraceDefinition;
import com.fujitsu.vdmj.in.modules.INModule;
import com.fujitsu.vdmj.in.modules.INModuleList;
import com.fujitsu.vdmj.tc.lex.TCNameList;

public class CTPluginSL extends CTPlugin
{
	private INModuleList inModuleList = null;
	
	@Override
	public void preCheck()
	{
		inModuleList = null;
	}

	@Override
	public <T> boolean checkLoadedFiles(T inList) throws Exception
	{
		inModuleList = (INModuleList) inList;
		return true;
	}
	
	@Override
	public Map<String, TCNameList> getTraceNames()
	{
		Map<String, TCNameList> nameMap = new HashMap<String, TCNameList>();
		
		for (INModule m: inModuleList)
		{
			for (INDefinition d: m.defs)
			{
				if (d instanceof INNamedTraceDefinition)
				{
					String module = d.name.getModule();
					TCNameList names = nameMap.get(module);
					
					if (names == null)
					{
						names = new TCNameList();
						nameMap.put(module, names);
					}
					
					names.add(d.name);
				}
			}
		}
		
		return nameMap;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getCT()
	{
		return (T)inModuleList;
	}
}
