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
import com.fujitsu.vdmj.messages.RTLogger;
import com.fujitsu.vdmj.messages.VDMErrorsException;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.runtime.DebuggerException;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.syntax.ParserException;
import com.fujitsu.vdmj.values.Value;

public class PrintCommand extends AnalysisCommand
{
	private final static String USAGE = "Usage: print <expression>";
	private final String expression;

	public PrintCommand(String[] argv)
	{
		if (argv.length == 1)
		{
			throw new IllegalArgumentException(USAGE);
		}
		
		StringBuilder sb = new StringBuilder();
		String sep = "";
		
		for (int i=1; i<argv.length; i++)
		{
			sb.append(sep);
			sb.append(argv[i]);
			sep = " ";
		}
		
		expression = sb.toString();
	}

	@Override
	public void run()
	{
		ConsoleDebugReader dbg = null;
		ConsoleKeyWatcher watch = null;
		
		try
		{
			dbg = new ConsoleDebugReader();
			dbg.start();
			watch = new ConsoleKeyWatcher(expression);
			watch.start();
			
   			long before = System.currentTimeMillis();
   			
   			Value v = Interpreter.getInstance().execute(expression);
   			
   			println("= " + v);
   			long after = System.currentTimeMillis();
			println("Executed in " + (double)(after-before)/1000 + " secs. ");

			if (RTLogger.getLogSize() > 0)
			{
				println("Dumped RT events");
				RTLogger.dump(false);
			}
		}
		catch (ParserException e)
		{
			println("Syntax: " + e.getMessage());
		}
		catch (DebuggerException e)
		{
			println("Debug: " + e.getMessage());
		}
		catch (RuntimeException e)
		{
			println("Runtime: " + e);
		}
		catch (VDMErrorsException e)
		{
			println(e.toString());
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
	
	public void help()
	{
		println("print <exp>: evaluate an expression");
	}
}
