/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package plugins;

import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.plugins.AnalysisCommand;

/**
 * A simple AnalysisCommand to issue an operating system command.
 */
public class OsPlugin extends AnalysisCommand
{
	private final static String CMD = "os <command> [<arguments>]";
	private final static String USAGE = "Usage: " + CMD;
	public  final static String HELP = CMD + " - run an operating system command";
	
	
	public OsPlugin(String line)
	{
		super(line);
		
		if (!argv[0].equals("os"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public String run(String line)
	{
		if (argv.length == 1)
		{
			return USAGE;
		}
		
		String[] cmd = new String[argv.length - 1];
		System.arraycopy(argv, 1, cmd, 0, cmd.length);		
		
		try
		{
			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.inheritIO();
			Process p = pb.start();
			p.waitFor();
			
			if (p.exitValue() != 0)
			{
				Console.err.println("Process exit code = " + p.exitValue());
			}
		}
		catch (Exception e)
		{
			Console.err.println("Error: " + e);
		}
		
		return null;	// Even if command failed
	}
}
