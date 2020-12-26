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

import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.definitions.INClassList;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.tc.definitions.TCClassList;

public class INPluginPR extends INPlugin
{
	private INClassList inClassList = null;
	
	public INPluginPR()
	{
		super();
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

	@Override
	public void preCheck()
	{
		inClassList = new INClassList();
	}
	
	@Override
	public <T> boolean checkLoadedFiles(T tcClassList) throws Exception
	{
		inClassList = ClassMapper.getInstance(INNode.MAPPINGS).init().convert(tcClassList);
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getIN()
	{
		return (T)inClassList;
	}

	@Override
	public <T> Interpreter getInterpreter(T tcClassList) throws Exception
	{
		return new ClassInterpreter(inClassList, (TCClassList)tcClassList);
	}
}
