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

import static com.fujitsu.vdmj.plugins.PluginConsole.verboseln;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.fujitsu.vdmj.plugins.commands.ErrorCommand;
import com.fujitsu.vdmj.util.GetResource;
import com.fujitsu.vdmj.util.Utils;

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
	 * Run the command. The string returned will be printed on the console, if not null.
	 */
	abstract public String run(String line);
	
	/**
	 * Parse a command line and return a command. If no plugins can be found, an
	 * ErrorCommand is returned which prints the error on run.
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
			if (e instanceof InvocationTargetException)
			{
				e = e.getCause();
			}
			
			verboseln("Parse caught " + e);
			return new ErrorCommand(e.getMessage());
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
		String[] argv = line.split("\\s+");
		List<String> pairs = GetResource.readResource("vdmj.commands");
		
		for (String pair: pairs)
		{
			try
			{
				String[] parts = pair.split("\\s*=\\s*");
				
				if (parts.length == 2 && parts[0].equals(argv[0]))
				{
					Class<?> clazz = Class.forName(parts[1]);
					Constructor<?> ctor = clazz.getConstructor(String.class);
					return (AnalysisCommand)ctor.newInstance(new Object[]{line});
				}
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
