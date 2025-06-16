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

import java.lang.reflect.InvocationTargetException;

import com.fujitsu.vdmj.debug.ConsoleDebugReader;
import com.fujitsu.vdmj.debug.ConsoleKeyWatcher;
import com.fujitsu.vdmj.messages.RTLogger;
import com.fujitsu.vdmj.messages.VDMErrorsException;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.runtime.DebuggerException;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.syntax.ParserException;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.values.Value;

public class PrintCommand extends AnalysisCommand
{
	private final static String CMD = "print <expression>";
	private final static String USAGE = "Usage: " + CMD;
	public  final static String HELP = CMD + " - evaluate an expression";
	
	private final Environment env;

	public PrintCommand(String line)
	{
		this(line, Interpreter.getInstance().getGlobalEnvironment());
	}

	public PrintCommand(String line, Environment env)
	{
		super(line);
		
		if (!argv[0].equals("print") && !argv[0].equals("p"))
		{
			throw new IllegalArgumentException(USAGE);
		}
		
		this.env = env;
	}

	@Override
	public String run(String line)
	{
		if (argv.length == 1)
		{
			return USAGE;
		}
		
		String expression = line.substring(line.indexOf(' ') + 1);
		ConsoleDebugReader dbg = null;
		ConsoleKeyWatcher watch = null;
		
		try
		{
			dbg = new ConsoleDebugReader();
			dbg.start();
			watch = new ConsoleKeyWatcher(expression);
			watch.start();
			
   			long before = System.currentTimeMillis();
   			Value v = Interpreter.getInstance().execute(expression, env);
   			long after = System.currentTimeMillis();
   			
   			println("= " + v);
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
		
		return null;
	}
}
