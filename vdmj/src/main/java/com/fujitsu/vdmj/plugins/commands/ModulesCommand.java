/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package com.fujitsu.vdmj.plugins.commands;

import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.analyses.TCPlugin;
import com.fujitsu.vdmj.plugins.analyses.TCPluginSL;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.modules.TCModuleList;

public class ModulesCommand extends AnalysisCommand
{
	private final static String CMD = "modules";
	private final static String USAGE = "Usage: " + CMD;
	public  final static String HELP = CMD + " - list the specification modules";

	public ModulesCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("modules"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public String run(String line)
	{
		if (argv.length != 1)
		{
			return USAGE;
		}

		TCPlugin tc = registry.getPlugin("TC");	// NB. TC has DEFAULTs combined
		String def = Interpreter.getInstance().getDefaultName();
		
		if (tc instanceof TCPluginSL)
		{
			TCModuleList list = tc.getTC();
	
			for (TCModule module: list)
			{
				println(module.name.getName() + (module.name.getName().equals(def) ? " (default)" : ""));
			}
			
			return null;
		}
		else
		{
			return "Command is only available for VDM-SL";
		}
	}
}
