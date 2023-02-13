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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.fujitsu.vdmj.ExitStatus;
import com.fujitsu.vdmj.commands.CommandPlugin;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.plugins.analyses.ASTPlugin;
import com.fujitsu.vdmj.plugins.commands.ReaderControl;
import com.fujitsu.vdmj.runtime.Interpreter;

@SuppressWarnings("deprecation")	// Because we're using CommandPlugin still
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
				
				switch (argv[0])
				{
					case "help":
					case "?":
						if (argv.length == 1)
						{
							registry.getHelp();
							println("reload - reload specification files");
							println("help - list all commands available");
							println("[q]uit - close the session");
						}
						else
						{
							println("Usage: help");
						}
						break;
					
					case "quit":
					case "q":
						if (argv.length == 1)
						{
							exitStatus = ExitStatus.EXIT_OK;
							carryOn = false;
						}
						else
						{
							println("Usage: [q]uit");
						}
						break;

					case "reload":
						if (argv.length == 1)
						{
							exitStatus = ExitStatus.RELOAD;
							carryOn = false;
						}
						else
						{
							println("Usage: reload");
						}
						break;

					default:
						AnalysisCommand command = registry.getCommand(argv);
						
						if (command == null)
						{
							command = loadDirectly(argv);
						}
						
						if (command == null)
						{
							println("Bad command. Try 'help'");
						}
						else
						{
							command.run();
							
							if (command instanceof ReaderControl)
							{
								ReaderControl ctrl = (ReaderControl)command;
								exitStatus = ctrl.exitStatus();
								carryOn = ctrl.carryOn();
							}
						}
						break;
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

	/**
	 * You can load an AnalysisCommand directly from the classpath, rather than getting one
	 * from an AnalysisPlugin with getCommand. This is mainly for backward compatibility, but
	 * it might be useful to offer "global" commands as an extension that are not linked to
	 * a particular plugin. The cmd-plugins jar includes a GitPlugin example that can be loaded
	 * this way.
	 * 
	 * Note that for this to work, the name of the command (as in the past) must be *Plugin.
	 */
	private AnalysisCommand loadDirectly(String[] argv) throws Exception
	{
		String[] packages = Properties.cmd_plugin_packages.split(";|:");
		
		for (String pack: packages)
		{
			try
			{
				// Remove this CommandPlugin test when we remove the @Deprecated classes.
				String plugin = Character.toUpperCase(argv[0].charAt(0)) + argv[0].substring(1).toLowerCase();
				Class<?> clazz = Class.forName(pack + "." + plugin + "Plugin");

				if (CommandPlugin.class.isAssignableFrom(clazz))
				{
					Constructor<?> ctor = clazz.getConstructor(Interpreter.class);
					CommandPlugin cmd = (CommandPlugin)ctor.newInstance(Interpreter.getInstance());
					
					// Convert an old CommandPlugin to an AnalysisCommand
					return new AnalysisCommand(argv)
					{
						@Override
						public void run()
						{
							try
							{
								cmd.run(argv);
							}
							catch (Exception e)
							{
								println(e);
							}
						}
					};
				}
				else if (AnalysisCommand.class.isAssignableFrom(clazz))
				{
					Constructor<?> ctor = clazz.getConstructor(String[].class);
					return (AnalysisCommand)ctor.newInstance(new Object[]{argv});
				}
			}
			catch (IllegalArgumentException e)	// From AnalysisCommands
			{
				println(e.getMessage());
			}
			catch (ClassNotFoundException e)
			{
				// Try next package
			}
			catch (InstantiationException e)
			{
				// Try next package
			}
			catch (IllegalAccessException e)
			{
				// Try next package
			}
		}

		return null;
	}
}
