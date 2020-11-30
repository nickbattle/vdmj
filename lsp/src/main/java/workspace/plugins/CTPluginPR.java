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

import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.in.definitions.INClassList;
import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.definitions.INNamedTraceDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameList;

public class CTPluginPR extends CTPlugin
{
	private INClassList inClassList = null;
	
	@Override
	public void preCheck()
	{
		inClassList = null;
		super.preCheck();
	}

	@Override
	public <T> boolean checkLoadedFiles(T inList) throws Exception
	{
		inClassList = (INClassList) inList;
		return true;
	}
	
	@Override
	public Map<String, TCNameList> getTraceNames()
	{
		Map<String, TCNameList> nameMap = new HashMap<String, TCNameList>();
		
		for (INClassDefinition cls: inClassList)
		{
			for (INDefinition d: cls.definitions)
			{
				if (d instanceof INNamedTraceDefinition)
				{
					String cname = d.name.getModule();
					TCNameList names = nameMap.get(cname);
					
					if (names == null)
					{
						names = new TCNameList();
						nameMap.put(cname, names);
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
		return (T)inClassList;
	}
}
