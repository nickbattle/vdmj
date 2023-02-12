/*******************************************************************************
 *
 *	Copyright (c) 2017 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.debug;

/**
 * Class to process one debugger command.
 */
public class DebugParser
{
	/**
	 * Parse one debugger command
	 */
	public static DebugCommand parse(String request)
	{
   		try
   		{
   			if (request == null || request.length() == 0)
   			{
   				return null;
   			}
   			
   			String[] argv = request.split("\\s+");
   			
   			if (argv[0].equals("quit") || argv[0].equals("q"))
			{
				return DebugCommand.QUIT;
			}
			else if (argv[0].equals("stop"))
			{
				return DebugCommand.STOP;
			}
			else if (argv[0].equals("help") || argv[0].equals("?"))
			{
				return DebugCommand.HELP;
			}
			else if (argv[0].equals("stack"))
			{
				return DebugCommand.STACK;
			}
			else if (argv[0].equals("up"))
			{
				return DebugCommand.UP;
			}
			else if (argv[0].equals("down"))
			{
				return DebugCommand.DOWN;
			}
			else if (argv[0].equals("step") || argv[0].equals("s"))
			{
				return DebugCommand.STEP;
			}
			else if (argv[0].equals("next") || argv[0].equals("n"))
			{
				return DebugCommand.NEXT;
			}
			else if (argv[0].equals("out") || argv[0].equals("o"))
			{
				return DebugCommand.OUT;
			}
			else if (argv[0].equals("continue") || argv[0].equals("c"))
			{
				return DebugCommand.CONTINUE;
			}
			else if (argv[0].equals("source"))
			{
				return DebugCommand.SOURCE;
			}
			else if (argv[0].equals("threads"))
			{
				return DebugCommand.THREADS;
			}
			else if (argv[0].equals("thread") || argv[0].equals("th"))
			{
				if (argv.length != 2) return new DebugCommand(DebugType.ERROR, "Usage: thread <n>");
				Integer th = Integer.parseInt(argv[1]);
				return new DebugCommand(DebugType.THREAD, th);
			}
			else if (argv[0].equals("print") || argv[0].equals("p"))
			{
				String exp = request.substring(request.indexOf(' ') + 1);
				return new DebugCommand(DebugType.PRINT, exp);
			}
			else if (argv[0].equals("break") || argv[0].equals("trace") || argv[0].equals("catch") ||
					argv[0].equals("list")  || argv[0].equals("remove"))
			{
				return new DebugCommand(DebugType.BREAKPOINT, request);
			}
			else
			{
				return new DebugCommand(DebugType.ERROR, "Bad command. Try 'help'");
			}
		}
		catch (Exception e)
		{
			return new DebugCommand(DebugType.ERROR, e);
		}
	}
}
