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

package com.fujitsu.vdmj.plugins;

import static com.fujitsu.vdmj.plugins.PluginConsole.*;

import java.lang.reflect.InvocationTargetException;

import com.fujitsu.vdmj.ExitStatus;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.plugins.analyses.ASTPlugin;

public class CommandReader
{
	public ExitStatus run()
	{
		boolean carryOn = true;
		long timestamp = System.currentTimeMillis();
		PluginRegistry registry = PluginRegistry.getInstance();
		ASTPlugin ast = registry.getPlugin("AST");
		ExitStatus exitStatus = ExitStatus.EXIT_OK;

		while (carryOn)
		{
			ast.checkForUpdates(timestamp);

			try
			{
				Console.out.print("> ");
				String line = Console.in.readLine();

				if (line == null)	// EOF
				{
					carryOn = false;
					continue;
				}
				
				line = line.trim();

				if (line.equals("") || line.startsWith("--"))
				{
					continue;
				}
				
				String[] argv = line.split("\\s+");
				
				if (argv[0].equals("help"))
				{
					registry.getHelp();
				}
				else if (argv[0].equals("quit") || argv[0].equals("q"))
				{
					carryOn = false;
				}
				else if (argv[0].equals("reload"))
				{
					exitStatus = ExitStatus.RELOAD;
					carryOn = false;
				}
				else
				{
					AnalysisCommand command = registry.getCommand(argv);
					
					if (command == null)
					{
						println("Unknown command, try 'help'");
					}
					else
					{
						command.run();
					}
				}
			}
			catch (Throwable e)
			{
				while (e instanceof InvocationTargetException)
				{
					e = (Exception)e.getCause();
				}
				
				println(e);
			}
		}
		
		return exitStatus;
	}
}
