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

import java.lang.reflect.InvocationTargetException;

import com.fujitsu.vdmj.debug.ConsoleDebugReader;
import com.fujitsu.vdmj.debug.ConsoleKeyWatcher;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.runtime.Interpreter;

public class InitCommand extends AnalysisCommand
{
	private final static String USAGE = "Usage: init";

	public InitCommand(String[] argv)
	{
		super(argv);
		
		if (!argv[0].equals("init"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public void run()
	{
		if (argv.length != 1)
		{
			println(USAGE);
			return;
		}

		ConsoleDebugReader dbg = null;
		ConsoleKeyWatcher watch = null;
		
		try
		{
			dbg = new ConsoleDebugReader();
			dbg.start();
			watch = new ConsoleKeyWatcher("init");
			watch.start();
			
   			long before = System.currentTimeMillis();
   			
   			Interpreter.getInstance().init();
   			
   			println("Global context initialized");
   			long after = System.currentTimeMillis();
			println("Executed in " + (double)(after-before)/1000 + " secs. ");
		}
		catch (Throwable e)
		{
			while (e instanceof InvocationTargetException)
			{
				e = (Exception)e.getCause();
			}
			
			println("Error: " + e.getMessage());
		}
		finally
		{
			if (dbg != null)
			{
				dbg.interrupt();	// Stop the debugger reader.
			}
			
			if (watch != null)
			{
				watch.interrupt();	// Stop ESC key watcher.
			}
		}
	}
	
	public static void help()
	{
		println("init: re-initialize the specification");
	}
}
