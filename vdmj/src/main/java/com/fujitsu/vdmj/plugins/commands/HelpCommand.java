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

public class HelpCommand extends AnalysisCommand
{
	private final static String USAGE = "Usage: help";

	public HelpCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("help") && !argv[0].equals("?"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public void run()
	{
		if (argv.length == 1)
		{
			registry.getHelp();
			
			ReloadCommand.help();
			HelpCommand.help();
			println("[q]uit - close the session");
		}
		else
		{
			println("Usage: help");
		}
	}
	
	public static void help()
	{
		println("help - list all commands available");
	}
}
