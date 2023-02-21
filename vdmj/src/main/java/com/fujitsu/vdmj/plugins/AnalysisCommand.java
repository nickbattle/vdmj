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

import static com.fujitsu.vdmj.plugins.PluginConsole.errorln;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.lang.reflect.Constructor;

import com.fujitsu.vdmj.commands.CommandPlugin;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.plugins.commands.ErrorCommand;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.util.Utils;

@SuppressWarnings("deprecation")
abstract public class AnalysisCommand
{
	protected final PluginRegistry registry;
	protected final String line;
	protected final String[] argv;
	
	protected AnalysisCommand(String line)
	{
		this.registry = PluginRegistry.getInstance();
		this.argv = Utils.toArgv(line);
		this.line = line;
	}

	/**
	 * Run the command.
	 */
	abstract public void run();

	
	/**
	 * Parse a command line and return a command. If no plugins can be found, an
	 * ErrorCommand is returned which prints the error on run().
	 */
	public static AnalysisCommand parse(String line)
	{
		if (line == null || line.isEmpty())
		{
			return new ErrorCommand("Unknown command");
		}

		try
		{
			AnalysisCommand cmd = PluginRegistry.getInstance().getCommand(line);
			
			if (cmd != null)
			{
				return cmd;
			}
			else
			{
				cmd = loadDirectly(line);
			}
			
			if (cmd != null)
			{
				return cmd;
			}
			else
			{
				return new ErrorCommand("Unknown command '" + line + "'. Try help");
			}
		}
		catch (Throwable e)
		{
			return new ErrorCommand("Error: " + e.getMessage());
		}
	}
	
	/**
	 * You can load an AnalysisCommand directly from the classpath, rather than getting one
	 * from a the PluginRegistry with getCommand. This is mainly for backward compatibility, but
	 * it might be useful to offer "global" commands as an extension that are not linked to
	 * a particular plugin. The cmd-plugins jar includes a GitPlugin example that can be loaded
	 * this way.
	 * 
	 * Note that for this to work, the name of the command (as in the past) must be <Cmd>Plugin.
	 */
	private static AnalysisCommand loadDirectly(String line) throws Exception
	{
		String[] packages = Properties.cmd_plugin_packages.split(";|:");
		
		for (String pack: packages)
		{
			try
			{
				// Remove this CommandPlugin test when we remove the @Deprecated classes.
				String[] argv = line.split("\\s+");
				String plugin = Character.toUpperCase(argv[0].charAt(0)) + argv[0].substring(1).toLowerCase();
				Class<?> clazz = Class.forName(pack + "." + plugin + "Plugin");

				if (CommandPlugin.class.isAssignableFrom(clazz))
				{
					Constructor<?> ctor = clazz.getConstructor(Interpreter.class);
					CommandPlugin cmd = (CommandPlugin)ctor.newInstance(Interpreter.getInstance());
					
					// Convert an old CommandPlugin to an AnalysisCommand
					return new AnalysisCommand(line)
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
								errorln(e);
							}
						}
					};
				}
				else if (AnalysisCommand.class.isAssignableFrom(clazz))
				{
					Constructor<?> ctor = clazz.getConstructor(String.class);
					return (AnalysisCommand)ctor.newInstance(new Object[]{line});
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
