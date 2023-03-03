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

import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.fujitsu.vdmj.ExitStatus;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.plugins.analyses.ASTPlugin;
import com.fujitsu.vdmj.plugins.commands.HelpCommand;
import com.fujitsu.vdmj.plugins.commands.QuitCommand;
import com.fujitsu.vdmj.plugins.commands.ReaderControl;
import com.fujitsu.vdmj.plugins.commands.ReloadCommand;

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
				String line = readLine();

				if (line == null || line.equals("null"))	// EOF
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
				AnalysisCommand command = null;
				
				switch (argv[0])
				{
					case "help":
					case "?":
						command = new HelpCommand(line);
						break;
					
					case "quit":
					case "q":
						command = new QuitCommand(line);
						break;

					case "reload":
						command = new ReloadCommand(line);
						break;

					default:
						command = AnalysisCommand.parse(line);
						break;
				}

				String result = command.run(line);		// Can be an ErrorCommand
				
				if (result != null)
				{
					println(result);
				}
				
				if (command instanceof ReaderControl)
				{
					ReaderControl ctrl = (ReaderControl)command;
					exitStatus = ctrl.exitStatus();
					carryOn = ctrl.carryOn();
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
	
	private String readLine() throws IOException
	{
		StringBuilder line = new StringBuilder();
		line.append("\\");
		
		do
		{
			line.deleteCharAt(line.length() - 1);	// Remove trailing backslash
			line.append(Console.in.readLine());
		}
		while (line.length() > 0 && line.charAt(line.length() - 1) == '\\');
		
		return line.toString();
	}
}
