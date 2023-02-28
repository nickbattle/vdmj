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

import static com.fujitsu.vdmj.plugins.PluginConsole.printf;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ModuleInterpreter;

public class StateCommand extends AnalysisCommand
{
	private final static String USAGE = "Usage: state";

	public StateCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("state"))
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
		else if (Settings.dialect != Dialect.VDM_SL)
		{
			return "Command is only availble for VDM-SL";
		}

		ModuleInterpreter interpreter = ModuleInterpreter.getInstance();
		Context c = interpreter.getStateContext();
		printf("%s", c == null ? "(no state)\n" : c.toString());
		
		return null;
	}
	
	public static void help()
	{
		if (Settings.dialect == Dialect.VDM_SL)
		{
			println("state - show the default module state");
		}
	}
}
