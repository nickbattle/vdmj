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

import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.in.definitions.INClassList;
import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.definitions.INDefinitionList;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.plugins.HelpList;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

import vdmj.commands.ClassesCommand;
import vdmj.commands.AnalysisCommand;
import vdmj.commands.LogCommand;
import workspace.events.CheckPrepareEvent;

public class INPluginPR extends INPlugin
{
	private INClassList inClassList = null;
	private TCClassList tcClassList = null;
	
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
	public void preCheck(CheckPrepareEvent ev)
	{
		inClassList = new INClassList();
		tcClassList = new TCClassList();
	}
	
	@Override
	public AnalysisCommand getCommand(String line)
	{
		String[] parts = line.split("\\s+");
		
		switch (parts[0])
		{
			case "classes":	return new ClassesCommand(line);
			case "log":		return new LogCommand(line);
			
			default:
				return super.getCommand(line);
		}
	}
	
	@Override
	public HelpList getCommandHelp()
	{
		return new HelpList(super.getCommandHelp(), ClassesCommand.HELP, LogCommand.HELP);
	}
	
	@Override
	public <T extends Mappable> boolean checkLoadedFiles(T tcClassList) throws Exception
	{
		this.tcClassList = (TCClassList) tcClassList;
		this.inClassList = ClassMapper.getInstance(INNode.MAPPINGS).init().convert(tcClassList);
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Mappable> T getIN()
	{
		return (T)inClassList;
	}

	@Override
	public <T extends Mappable> Interpreter getInterpreter() throws Exception
	{
		return new ClassInterpreter(inClassList, tcClassList);
	}

	@Override
	public INDefinitionList findDefinition(TCNameToken name)
	{
		INDefinitionList results = new INDefinitionList();
		
		for (INClassDefinition module: inClassList)
		{
			if (module.name.getName().equals(name.getModule()))
			{
				for (INDefinition def: module.definitions)
				{
					if (def.name != null && def.name.equals(name))
					{
						results.add(def);
					}
				}
			}
		}
		
		return results;
	}
}
