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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.plugins.commands;

import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.util.List;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.scheduler.SchedulableThread;

public class ThreadsCommand extends AnalysisCommand
{
	private final static String CMD = "threads";
	private final static String USAGE = "Usage: " + CMD;
	public  final static String HELP = CMD + " - list the running threads";
	
	public ThreadsCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("threads"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public String run(String line)
	{
		List<SchedulableThread> list = SchedulableThread.getAllThreads();
		
		if (Settings.dialect == Dialect.VDM_SL)
		{
			return "Command is not available in VDM-SL";
		}
		else if (argv.length != 1)
		{
			return USAGE;
		}
		else if (list.isEmpty())
		{
			return "No threads running";
		}
		
   		int maxName = 0;
		long maxNum = 0;
		
		for (SchedulableThread th: list)
		{
			if (th.getName().length() > maxName)
			{
				maxName = th.getName().length();
			}
			
			if (th.getId() > maxNum)
			{
				maxNum = th.getId();
			}
		}
		
		int width = (int)Math.floor(Math.log10(maxNum)) + 1;
		
		for (SchedulableThread th: list)
		{
			String format = String.format("%%%dd: %%-%ds  %%s", width, maxName);
			println(String.format(format, th.getId(), th.getName(), th.getRunState()));
		}
		
		return null;
	}
}
