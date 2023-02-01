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

package com.fujitsu.vdmj.plugins.analyses;

import static com.fujitsu.vdmj.plugins.PluginConsole.infoln;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.fujitsu.vdmj.ExitStatus;
import com.fujitsu.vdmj.RemoteControl;
import com.fujitsu.vdmj.RemoteInterpreter;
import com.fujitsu.vdmj.commands.ClassCommandReader;
import com.fujitsu.vdmj.commands.CommandReader;
import com.fujitsu.vdmj.debug.ConsoleDebugReader;
import com.fujitsu.vdmj.debug.ConsoleKeyWatcher;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.definitions.INClassList;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.plugins.PluginRegistry;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.tc.definitions.TCClassList;

/**
 * VDM-PP IN plugin
 */
public class INPluginPP extends INPlugin
{
	private INClassList inClassList = null;
	private ClassInterpreter interpreter = null;
	
	@Override
	protected List<VDMMessage> interpreterPrepare()
	{
		inClassList = new INClassList();
		interpreter = null;
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected List<VDMMessage> interpreterInit()
	{
		TCPlugin tc = PluginRegistry.getInstance().getPlugin("TC");
		TCClassList checkedClasses = tc.getTC();

		try
		{
   			inClassList = ClassMapper.getInstance(INNode.MAPPINGS).init().convert(checkedClasses);
   			interpreter = new ClassInterpreter(inClassList, checkedClasses);
   			ConsoleDebugReader dbg = null;
   			ConsoleKeyWatcher watcher = null;

   			try
   			{
   				dbg = new ConsoleDebugReader();
   				dbg.start();
   				watcher = new ConsoleKeyWatcher("init");
   				watcher.start();
   				
   				interpreter.init();
   			}
   			finally
   			{
   				if (dbg != null)
   				{
   					dbg.interrupt();
   				}
   				
   				if (watcher != null)
   				{
   					watcher.interrupt();
   				}
   			}

   			if (defaultName != null)
   			{
   				interpreter.setDefaultName(defaultName);
   			}
		}
		catch (ContextException e)
		{
			println("Initialization: " + e);
			
			if (e.isStackOverflow())
			{
				e.ctxt.printStackFrames(Console.out);
			}
			else
			{
				e.ctxt.printStackTrace(Console.out, true);
			}
			
			return errsOf(e);
		}
		catch (Exception e)
		{
			while (e instanceof InvocationTargetException)
			{
				e = (Exception)e.getCause();
			}
			
			println("Initialization:");
			println(e);

			return errsOf(e);
		}
		
		return null;
	}
	
	@Override
	public ExitStatus interpreterRun()
	{
		try
		{
			if (interactive)
			{
				infoln("Interpreter started");
				CommandReader reader = new ClassCommandReader(interpreter, "> ");
				ASTPlugin ast = PluginRegistry.getInstance().getPlugin("AST");
				return reader.run(ast.getFiles());
			}
			else if (expression != null)
			{
				println(interpreter.execute(expression).toString());
				return ExitStatus.EXIT_OK;
			}
			else if (remoteClass != null)
			{
				RemoteControl remote = remoteClass.getDeclaredConstructor().newInstance();
				remote.run(new RemoteInterpreter(interpreter));
				return ExitStatus.EXIT_OK;
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
		catch (Exception e)
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

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Mappable> T getIN()
	{
		return (T)inClassList;
	}
}
