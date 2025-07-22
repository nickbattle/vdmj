/*******************************************************************************
 *
 *	Copyright (c) 2025 Nick Battle.
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

package com.fujitsu.vdmj.plugins.analyses;

import static com.fujitsu.vdmj.plugins.PluginConsole.infoln;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.lang.reflect.InvocationTargetException;
import com.fujitsu.vdmj.ExitStatus;
import com.fujitsu.vdmj.RemoteInterpreter;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.CommandReader;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.runtime.ContextException;

/**
 * VDM-PP command line plugin
 */
public class CMDPluginPP extends CMDPlugin
{
	@Override
	protected ExitStatus interpreterRun()
	{
		try
		{
			INPlugin in = registry.getPlugin("IN");
			ClassInterpreter interpreter = in.getInterpreter();

			if (interactive)
			{
				infoln("Interpreter started");
				return new CommandReader().run();
			}
			else if (expression != null)
			{
				println(interpreter.execute(expression));
			}
			else if (commandline != null)
			{
				AnalysisCommand command = AnalysisCommand.parse(commandline);
				String result = command.run(commandline);
				
				if (result != null)
				{
					println(result);
				}
			}
			else if (remoteInstance != null)
			{
				remoteInstance.run(new RemoteInterpreter(interpreter));
			}
		}
		catch (ContextException e)
		{
			println("Execution: " + e);

			if (e.isStackOverflow())
			{
				e.ctxt.printStackFrames(Console.out);
			}
			else
			{
				e.ctxt.printStackTrace(Console.out, true);
			}

			return ExitStatus.EXIT_ERRORS;
		}
		catch (Throwable e)
		{
			while (e instanceof InvocationTargetException)
			{
				e = (Exception)e.getCause();
			}
			
			println("Execution:");
			println(e);
			return ExitStatus.EXIT_ERRORS;
		}
		
		return ExitStatus.EXIT_OK;
	}
}
