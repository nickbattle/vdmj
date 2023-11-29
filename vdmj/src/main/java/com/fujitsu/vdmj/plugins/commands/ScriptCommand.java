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

import static com.fujitsu.vdmj.plugins.PluginConsole.errorln;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.plugins.AnalysisCommand;

public class ScriptCommand extends AnalysisCommand
{
	private final static String USAGE = "Usage: script <file>";

	public ScriptCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("script"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public String run(String line)
	{
		if (argv.length != 2)
		{
			return USAGE;
		}
		
		File file = new File(argv[1]);
		
		if (!file.exists())
		{
			return "File not found: " + file;
		}

		BufferedReader script = null;
		
		try
		{
			script = new BufferedReader(new InputStreamReader(new FileInputStream(file), Settings.filecharset));

			while (true)
			{
				line = readLine(script);
				
				if (line == null)
				{
					println("END " + file);
					break;
				}
				
				line = line.trim();
				println(file + "> " + line);
				
				if (line.isEmpty() || line.startsWith("--"))
				{
					continue;
				}
				
				AnalysisCommand cmd = registry.getCommand(line);
				
				if (cmd != null)
				{
					cmd.run(line);
				}
				else
				{
					println("Unknown command in script: " + line);
				}
			}
		}
		catch (IOException e)
		{
			errorln("Script: " + e.getMessage());
		}
		finally
		{
			try
			{
				script.close();
			}
			catch (IOException e)
			{
				// ignore
			}
		}
		
		return null;
	}

	private String readLine(BufferedReader script) throws IOException
	{
		StringBuilder line = new StringBuilder();
		line.append("\\");
		
		do
		{
			line.deleteCharAt(line.length() - 1);	// Remove trailing backslash
			String part = script.readLine();
			
			if (part != null)
			{
				line.append(part);
			}
			else
			{
				return null;
			}
		}
		while (line.length() > 0 && line.charAt(line.length() - 1) == '\\');

		return line.toString();
	}
	
	public static String help()
	{
		return "script <file> - run commands from file";
	}
}
