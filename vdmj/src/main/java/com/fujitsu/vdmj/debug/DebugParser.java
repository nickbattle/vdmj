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
   			else if (request.equals("quit") || request.equals("q"))
			{
				return DebugCommand.QUIT;
			}
			else if (request.equals("stop"))
			{
				return DebugCommand.STOP;
			}
			else if (request.equals("help") || request.equals("?"))
			{
				return DebugCommand.HELP;
			}
			else if (request.equals("stack"))
			{
				return DebugCommand.STACK;
			}
			else if (request.equals("up"))
			{
				return DebugCommand.UP;
			}
			else if (request.equals("down"))
			{
				return DebugCommand.DOWN;
			}
			else if (request.equals("step") || request.equals("s"))
			{
				return DebugCommand.STEP;
			}
			else if (request.equals("next") || request.equals("n"))
			{
				return DebugCommand.NEXT;
			}
			else if (request.equals("out") || request.equals("o"))
			{
				return DebugCommand.OUT;
			}
			else if (request.equals("continue") || request.equals("c"))
			{
				return DebugCommand.CONTINUE;
			}
			else if (request.equals("source"))
			{
				return DebugCommand.SOURCE;
			}
			else if (request.equals("threads"))
			{
				return DebugCommand.THREADS;
			}
			else if (request.startsWith("thread ") || request.startsWith("th "))
			{
				Integer th = Integer.parseInt(request.substring(request.indexOf(' ') + 1));
				return new DebugCommand(DebugType.THREAD, th);
			}
			else if (request.startsWith("print ") || request.startsWith("p "))
			{
				String args = request.substring(request.indexOf(' ') + 1);
				return new DebugCommand(DebugType.PRINT, args);
			}
			else if (request.startsWith("break") || request.startsWith("trace") || request.startsWith("catch") ||
					 request.startsWith("list")  || request.startsWith("remove"))
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
