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
import static com.fujitsu.vdmj.plugins.PluginConsole.plural;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;

import com.fujitsu.vdmj.ExitStatus;
import com.fujitsu.vdmj.RemoteControl;
import com.fujitsu.vdmj.RemoteInterpreter;
import com.fujitsu.vdmj.RemoteSimulation;
import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.commands.ClassCommandReader;
import com.fujitsu.vdmj.commands.CommandReader;
import com.fujitsu.vdmj.debug.ConsoleDebugReader;
import com.fujitsu.vdmj.debug.ConsoleKeyWatcher;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.definitions.INClassList;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.RTLogger;
import com.fujitsu.vdmj.plugins.PluginRegistry;
import com.fujitsu.vdmj.plugins.events.Event;
import com.fujitsu.vdmj.plugins.events.ShutdownEvent;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.tc.definitions.TCClassList;

/**
 * VDM-RT IN plugin
 */
public class INPluginRT extends INPlugin
{
	private INClassList inClassList = null;
	private ClassInterpreter interpreter = null;
	
	@Override
	public void init()
	{
		super.init();
		eventhub.register(ShutdownEvent.class, this);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T handleEvent(Event event) throws Exception
	{
		if (event instanceof ShutdownEvent)
		{
			if (RTLogger.getLogSize() > 0)
			{
				println("Dumping RT events");
				RTLogger.dump(true);
			}

			return (T) errsOf();
		}
		else
		{
			return super.handleEvent(event);
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected <T> T interpreterPrepare()
	{
		inClassList = new INClassList();
		interpreter = null;
		
		RemoteSimulation rs = RemoteSimulation.getInstance();
		
		if (rs != null)
		{
			try
			{
				ASTPlugin ast = PluginRegistry.getInstance().getPlugin("AST");
				ASTClassList parsedClasses = ast.getAST();
				rs.setup(parsedClasses);
			}
			catch (Exception ex)
			{
				println("Simulation: " + ex.getMessage());
				return (T) errsOf(ex);
			}
		}

		return (T) errsOf();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <T> T interpreterInit()
	{
		if (!startInterpreter)
		{
			return (T) errsOf();
		}
		
		TCPlugin tc = PluginRegistry.getInstance().getPlugin("TC");
		TCClassList checkedClasses = tc.getTC();

		if (logfile != null)
		{
    		try
    		{
    			RTLogger.setLogfileName(new File(logfile));
    			println("RT events now logged to " + logfile);
    		}
    		catch (FileNotFoundException e)
    		{
    			println("Cannot create RT event log: " + e.getMessage());
    			return (T) errsOf(e);
    		}
		}

		try
		{
   			inClassList = ClassMapper.getInstance(INNode.MAPPINGS).init().convert(checkedClasses);
   			interpreter = new ClassInterpreter(inClassList, checkedClasses);
   			long before = System.currentTimeMillis();
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

   			long after = System.currentTimeMillis();

   	   		infoln("Initialized " + plural(inClassList.size(), "class", "es") + " in " +
   	   			(double)(after-before)/1000 + " secs. ");
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
			
			return (T) errsOf(e);
		}
		catch (Exception e)
		{
			while (e instanceof InvocationTargetException)
			{
				e = (Exception)e.getCause();
			}
			
			println("Initialization:");
			println(e);

			return (T) errsOf(e);
		}
		finally
		{
			if (logfile != null)
			{
				RTLogger.dump(true);
				infoln("RT events dumped to " + logfile);
			}
		}

		return (T) errsOf();
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
