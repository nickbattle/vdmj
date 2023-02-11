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

import static com.fujitsu.vdmj.plugins.PluginConsole.fail;
import static com.fujitsu.vdmj.plugins.PluginConsole.infoln;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

import com.fujitsu.vdmj.ExitStatus;
import com.fujitsu.vdmj.RemoteControl;
import com.fujitsu.vdmj.RemoteInterpreter;
import com.fujitsu.vdmj.debug.ConsoleDebugReader;
import com.fujitsu.vdmj.debug.ConsoleKeyWatcher;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.modules.INModuleList;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.plugins.CommandReader;
import com.fujitsu.vdmj.plugins.PluginRegistry;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ModuleInterpreter;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.util.Utils;

/**
 * VDM-SL IN plugin
 */
public class INPluginSL extends INPlugin
{
	private INModuleList inModuleList = null;
	private ModuleInterpreter interpreter = null;
	
	@Override
	public List<VDMMessage> interpreterPrepare()
	{
		inModuleList = new INModuleList();
		interpreter = null;
		return null;
	}

	@Override
	protected List<VDMMessage> interpreterInit()
	{
		try
		{
   			getInterpreter();
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
		catch (Throwable e)
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
	protected ExitStatus interpreterRun()
	{
		try
		{
			if (interactive)
			{
				infoln("Interpreter started");
				return new CommandReader().run();
			}
			else if (expression != null)
			{
				// No debug thread or watcher for -e <exp>
				println(interpreter.execute(expression));
			}
			else if (remoteClass != null)
			{
				RemoteControl remote = remoteClass.getDeclaredConstructor().newInstance();
				remote.run(new RemoteInterpreter(interpreter));
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

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Collection<?>> T getIN()
	{
		return (T)inModuleList;
	}

	@Override
	public ModuleInterpreter getInterpreter()
	{
		if (interpreter == null)
		{
			try
			{
				TCPlugin tc = PluginRegistry.getInstance().getPlugin("TC");
				TCModuleList checkedModules = tc.getTC();
				long before = System.currentTimeMillis();
				inModuleList = ClassMapper.getInstance(INNode.MAPPINGS).init().convert(checkedModules);
				Utils.mapperStats(before, INNode.MAPPINGS);
				interpreter = new ModuleInterpreter(inModuleList, checkedModules);
			}
			catch (Exception e)
			{
				println(e);
				fail("Cannot create interpreter");
			}
		}
		
		return interpreter;
	}
}
