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

import com.fujitsu.vdmj.debug.BreakpointReader;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.runtime.Interpreter;

public class DebugCommand extends AnalysisCommand
{
	private final static String USAGE = "Usage: break | trace | catch | list | remove";

	public DebugCommand(String[] argv)
	{
		super(argv);
		
		if (!argv[0].equals("break") &&
			!argv[0].equals("trace") &&
			!argv[0].equals("catch") &&
			!argv[0].equals("list") &&
			!argv[0].equals("remove"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public void run()
	{
		BreakpointReader reader = new BreakpointReader(Interpreter.getInstance());
		reader.doCommand(argv2line(0));
	}
	
	public static void help()
	{
		println("break [<file>:]<line#> [<condition>] - create a breakpoint");
		println("break <function/operation> [<condition>] - create a breakpoint");
		println("trace [<file>:]<line#> [<exp>] - create a tracepoint");
		println("trace <function/operation> [<exp>] - create a tracepoint");
		println("catch [<exp list>] - create an exception catchpoint");
		println("remove <breakpoint#> - remove a trace/breakpoint");
		println("list - list breakpoints");
	}
}
