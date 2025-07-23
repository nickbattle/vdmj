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

package com.fujitsu.vdmj.plugins.analyses;

import static com.fujitsu.vdmj.plugins.PluginConsole.fail;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.math.MathContext;
import java.math.RoundingMode;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.debug.ConsoleDebugReader;
import com.fujitsu.vdmj.debug.ConsoleKeyWatcher;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.statements.INStatementList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.plugins.AnalysisEvent;
import com.fujitsu.vdmj.plugins.AnalysisPlugin;
import com.fujitsu.vdmj.plugins.EventListener;
import com.fujitsu.vdmj.plugins.events.AbstractCheckFilesEvent;
import com.fujitsu.vdmj.plugins.events.CheckCompleteEvent;
import com.fujitsu.vdmj.plugins.events.CheckPrepareEvent;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.Interpreter;

/**
 * IN analysis plugin
 */
abstract public class INPlugin extends AnalysisPlugin implements EventListener
{
	protected String defaultName;
	protected String logfile;
	
	@Override
	public String getName()
	{
		return "IN";
	}
	
	@Override
	public int getPriority()
	{
		return IN_PRIORITY;
	}

	@Override
	public void init()
	{
		defaultName = null;
		logfile = null;
		
		eventhub.register(CheckPrepareEvent.class, this);
		eventhub.register(CheckCompleteEvent.class, this);
	}

	public static INPlugin factory(Dialect dialect) throws Exception
	{
		switch (dialect)
		{
			case VDM_SL:
				return new INPluginSL();
				
			case VDM_PP:
				return new INPluginPP();
				
			case VDM_RT:
				return new INPluginRT();
				
			default:
				throw new Exception("Unknown dialect: " + dialect);
		}
	}
	
	@Override
	public void usage()
	{
		println("-default <name>: set the default module/class");
		println("-pre: disable precondition checks");
		println("-post: disable postcondition checks");
		println("-inv: disable type/state invariant checks");
		println("-dtc: disable all dynamic type checking");
		println("-exceptions: raise pre/post/inv violations as <RuntimeError>");
		println("-measures: disable recursive measure checking");
		println("-log <filename>: enable real-time event logging");
		println("-precision <n>: set real number precision to n places");
	}
	
	@Override
	public void processArgs(List<String> argv)
	{
		Iterator<String> iter = argv.iterator();
		
		while (iter.hasNext())
		{
			switch(iter.next())
			{
				case "-default":
					iter.remove();
					
					if (iter.hasNext())
					{
						defaultName = iter.next();
						iter.remove();
					}
					else
					{
						fail("-default requires a name");
					}
					break;
					
				case "-log":
					iter.remove();
					
					if (iter.hasNext())
					{
						logfile = iter.next();
						iter.remove();
					}
					else
					{
						fail("-log requires a log file name");
					}
					break;
					
	    		case "-pre":
	    			iter.remove();
	    			Settings.prechecks = false;
	    			break;

	    		case "-post":
	    			iter.remove();
	    			Settings.postchecks = false;
	    			break;
	    			
	    		case "-inv":
	    			iter.remove();
	    			Settings.invchecks = false;
	    			break;
	    			
	    		case "-dtc":
	    			iter.remove();
	    			// NB. Turn off both when no DTC
	    			Settings.invchecks = false;
	    			Settings.dynamictypechecks = false;
	    			break;
	    			
	    		case "-exceptions":
	    			iter.remove();
	    			Settings.exceptions = true;
	    			break;
	    			
	    		case "-measures":
	    			iter.remove();
	    			Settings.measureChecks = false;
	    			break;
	    			
	    		case "-precision":
	    			iter.remove();
	    			
        			if (iter.hasNext())
        			{
       					String arg = iter.next();
       					iter.remove();

       					try
						{
							int precision = Integer.parseInt(arg);
							
							if (precision < 10)
							{
								fail("Precision argument must be >= 10");
							}
							else
							{
								Settings.precision = new MathContext(precision, RoundingMode.HALF_UP);
							}
						}
						catch (NumberFormatException e)
						{
							fail("Precision argument must be numeric");
						}
        			}
        			else
        			{
        				fail("-precision option requires a value");
        			}
        			break;

			}
		}
		
		if (logfile != null && Settings.dialect != Dialect.VDM_RT)
		{
			fail("The -log option can only be used with -vdmrt");
		}
	}
	
	@Override
	public List<VDMMessage> handleEvent(AnalysisEvent event) throws Exception
	{
		if (event instanceof CheckPrepareEvent)
		{
			return interpreterPrepare();
		}
		else if (event instanceof CheckCompleteEvent)
		{
			CMDPlugin cmd = registry.getPlugin("CMD");

			if (cmd.startInterpreter())
			{
				event.setProperty(AbstractCheckFilesEvent.TITLE, "Initialized");
				event.setProperty(AbstractCheckFilesEvent.KIND, "init");
				return interpreterInit(cmd.isInteractive());
			}
			else
			{
				return null;
			}
		}
		else
		{
			throw new Exception("Unhandled event: " + event);
		}
	}

	protected List<VDMMessage> interpreterInit(boolean interactive)
	{
		try
		{
   			Interpreter interpreter = getInterpreter();
   			ConsoleDebugReader dbg = null;
   			ConsoleKeyWatcher watcher = null;

   			try
   			{
   				if (interactive)
   				{
	   				dbg = new ConsoleDebugReader();
	   				dbg.start();
	   				watcher = new ConsoleKeyWatcher("init");
	   				watcher.start();
   				}
   				
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
	
	abstract protected List<VDMMessage> interpreterPrepare();

	abstract public <T extends Interpreter> T getInterpreter();

	abstract public <T extends Collection<?>> T getIN();
	
	abstract public INExpressionList findExpressions(File file, int lineno);
	
	abstract public INStatementList findStatements(File file, int lineno);
}
