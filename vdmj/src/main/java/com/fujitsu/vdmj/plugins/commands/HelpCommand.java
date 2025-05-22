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

import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.HelpList;
import com.fujitsu.vdmj.plugins.PluginRegistry;

public class HelpCommand extends AnalysisCommand
{
	private final static String CMD = "help [<command>]";
	private final static String USAGE = "Usage: " + CMD;
	public  final static String HELP = CMD + " - list all commands available";

	public HelpCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("help") && !argv[0].equals("?"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public String run(String line)
	{
		StringBuilder sb = new StringBuilder();
		HelpList help = PluginRegistry.getInstance().getCommandHelp();

		help.add(ReloadCommand.HELP);	// These three don't come from any plugin.
		help.add(HelpCommand.HELP);
		help.add(QuitCommand.HELP);
		
		help.add(AnalysisCommand.getCommandHelp());		// Standalone commands

		String sought = null;
		String sep = "";
		
		if (argv.length == 1)
		{
			sought = null;
		}
		else if (argv.length == 2)
		{
			sought = argv[1]; 
		}
		else
		{
			return USAGE;
		}
		
		for (String cmd: help.keySet())
		{
			if (sought == null || cmd.equals(sought))
			{
				sb.append(sep);
				sb.append(help.get(cmd));
				sep = "\n";
			}
		}

		if (sb.length() == 0)
		{
			sb.append("Unknown command '" + sought + "'");
		}
		
		return sb.toString();
	}
}
